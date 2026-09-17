package com.streamx.hub.rag.embed.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cloudevents.CloudEvent;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

@ConnectWireMock
public abstract class BaseQuarkusIntegrationTest {

  private static final String INTEGRATION_TEST = "integration-test";
  protected static final String SERVICE_BASE_URL = "http://localhost:8081";

  // will be injected automatically when the test class is annotated with @ConnectWireMock
  protected WireMock wiremock;

  @BeforeAll
  static void setServiceInstanceId() {
    System.setProperty("streamx.service.instance-id", INTEGRATION_TEST);
  }

  @BeforeEach
  void setupEndpointsForReceivingOutgoingEvents() {
    for (String channel : ChannelsReader.OUTGOING_CHANNELS) {
      String endpoint = toEndpoint(channel);
      wiremock.register(post(urlEqualTo(endpoint))
          .willReturn(aResponse().withStatus(202).withBody("ACK")));
    }
  }

  protected static void sendEvent(CloudEvent cloudEvent, String channel) {
    String serializedEvent = CloudEventsSerialization.serialize(cloudEvent);
    String url = toUrl(channel);
    HttpRequestor.post(url, serializedEvent);
  }

  protected CloudEvent waitForResponseEvent(String outgoingChannel) {
    return waitForLastResponseEvent(outgoingChannel, 1);
  }

  protected CloudEvent waitForLastResponseEvent(String outgoingChannel, int totalCount) {
    return waitForResponseEvents(outgoingChannel, totalCount).getLast();
  }

  private List<CloudEvent> waitForResponseEvents(String outgoingChannel, int totalCount) {
    String endpoint = toEndpoint(outgoingChannel);
    List<LoggedRequest> responses = waitForResponseRequests(endpoint, totalCount);
    return responses.stream()
        .map(LoggedRequest::getBody)
        .map(CloudEventsSerialization::deserialize)
        .toList();
  }

  private List<LoggedRequest> waitForResponseRequests(String endpoint, int totalCount) {
    List<LoggedRequest> results = new LinkedList<>();
    try {
      await().atMost(waitForResponseEventsTimeout()).untilAsserted(() -> {
        List<LoggedRequest> requests = WireMock.findAll(postRequestedFor(urlEqualTo(endpoint)));
        assertThat(requests).hasSize(totalCount);
        results.addAll(requests);
      });
    } catch (ConditionTimeoutException ex) {
      return fail(ex);
    }
    return results;
  }

  protected Duration waitForResponseEventsTimeout() {
    return Duration.ofSeconds(3);
  }

  static Map<String, String> propertiesForOutgoingChannels() {
    Map<String, String> properties = new HashMap<>();

    String host = getContainerLocalhost();
    for (String channel : ChannelsReader.OUTGOING_CHANNELS) {
      String endpoint = toEndpoint(channel);
      properties.put(
          "mp.messaging.outgoing." + channel + ".url",
          "http://%s:${quarkus.wiremock.devservices.port}%s".formatted(host, endpoint)
      );
    }

    return properties;
  }

  private static String toUrl(String channel) {
    return SERVICE_BASE_URL + toEndpoint(channel);
  }

  private static String toEndpoint(String channel) {
    return "/" + channel;
  }

  protected static String getContainerLocalhost() {
    if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) {
      return "172.17.0.1";
    } else {
      return "host.docker.internal";
    }
  }
}