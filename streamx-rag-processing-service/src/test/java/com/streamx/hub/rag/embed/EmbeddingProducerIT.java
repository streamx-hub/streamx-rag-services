package com.streamx.hub.rag.embed;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.blueprints.data.Data;
import com.streamx.blueprints.data.Resource;
import com.streamx.hub.rag.data.EmbeddingBatch;
import com.streamx.hub.rag.embed.EmbeddingProducerIT.IntegrationTestProfile;
import com.streamx.hub.rag.embed.commons.BaseQuarkusIntegrationTest;
import com.streamx.hub.rag.embed.commons.BaseQuarkusIntegrationTestProfile;
import com.streamx.hub.rag.utils.CloudEventUtils;
import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@QuarkusIntegrationTest
@TestProfile(IntegrationTestProfile.class)
public class EmbeddingProducerIT extends BaseQuarkusIntegrationTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String KEY = "pim:B07TMH6289";
  private static final String TYPE = "data/product";
  private static final String PAYLOAD = """
          {
            "id": "B07TMH6289",
            "sku": "B07TMH6289",
            "lang": "en",
            "name": "LeatherSoft Kids/Youth Recliner with Armrest Storage, \
      5+ Age Group, Light Blue",
            "label": "LeatherSoft Kids/Youth Recliner with Armrest Storage, \
      5+ Age Group, Light Blue",
            "description": "LeatherSoft Kids/Youth Recliner with Armrest Storage, \
      5+ Age Group, Light Blue",
            "slug": leathersoft-kids-youth-recliner-with-armrest-storage-5\
            -age-group-light-blue-b07tmh6289",
            "quantity": "100",
            "type": "simple",
            "price": {
              "value": "10.02",
              "discountedValue": "8.52"
            }
          }
      """;

  @BeforeEach
  void setup() {
    stubFor(post(urlEqualTo("/embeddings"))
        .willReturn(okJson("""
            {
              "object": "list",
              "data": [
                {
                  "object": "embedding",
                  "index": 0,
                  "embedding": [0.1, 0.2, 0.3]
                }
              ]
            }
            """)));
  }

  @Test
  void shouldProduceEmbeddingBatch() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        Data.TYPE_PUBLISHED,
        new Data(PAYLOAD, TYPE),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    sendEvent(event, Channels.RESOURCES);

    // then
    CloudEvent outgoingEvent = waitForResponseEvent(Channels.EMBEDDINGS);

    assertThat(outgoingEvent.getId()).isNotEqualTo(event.getId());
    assertThat(outgoingEvent.getData()).isNotNull();
    assertThat(outgoingEvent.getSubject()).isEqualTo(KEY);
    assertThat(outgoingEvent.getTime()).isEqualTo(event.getTime());
    assertThat(outgoingEvent.getType()).isEqualTo(Data.TYPE_PUBLISHED);

    EmbeddingBatch embeddingBatch = getEmbeddingBatch(outgoingEvent);
    assertThat(embeddingBatch.vectors()).hasSize(1);
    assertThat(embeddingBatch.tokenUsage().totalTokenCount()).isEqualTo(-1);
    assertThat(embeddingBatch.embedded().get(1).toString()).contains(
        "\"discountedValue\": \"8.52\"\n}\n}");
  }

  private EmbeddingBatch getEmbeddingBatch(CloudEvent event) {
    Resource resource = CloudEventUtils.getData(event, Resource.class);
    String json = resource.getContentAsString();
    try {
      return objectMapper.readValue(json, EmbeddingBatch.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class IntegrationTestProfile extends BaseQuarkusIntegrationTestProfile {

    @Override
    protected Map<String, String> getServiceConfigProperties() {
      return Map.of("quarkus.langchain4j.openai.base-url",
          "http://%s:${quarkus.wiremock.devservices.port}".formatted(getContainerLocalhost()),
          "quarkus.langchain4j.openai.api-key", "test-key");
    }
  }
}
