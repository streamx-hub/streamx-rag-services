package com.streamx.hub.rag.embed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.blueprints.data.Data;
import com.streamx.blueprints.data.Resource;
import com.streamx.hub.rag.data.EmbeddingBatch;
import com.streamx.hub.rag.utils.CloudEventUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.cloudevents.CloudEvent;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmbeddingProducerTest {

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

  private InMemorySource<CloudEvent> dataSource;
  private InMemorySink<CloudEvent> dataSink;

  @Inject
  @Any
  InMemoryConnector connector;

  @Inject
  ObjectMapper objectMapper;

  @InjectMock
  EmbeddingModel embeddingModel;

  @BeforeEach
  void beforeEach() {
    dataSource = connector.source(Channels.RESOURCES);
    dataSink = connector.sink(Channels.EMBEDDINGS);
    dataSink.clear();
  }

  @Test
  void expectEmbeddedEventWithEmbeddings() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        Data.TYPE_PUBLISHED,
        new Data(PAYLOAD, TYPE),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    mockEmbeddingsResponse();

    CloudEvent resultEvent = getResourceFrom(event);

    // then
    assertBase(resultEvent, KEY, event);
    assertThat(resultEvent.getType()).isEqualTo(Data.TYPE_PUBLISHED);

    EmbeddingBatch embeddingBatch = getEmbeddingBatch(resultEvent);
    assertThat(embeddingBatch.vectors()).hasSize(1);
    assertThat(embeddingBatch.tokenUsage().totalTokenCount()).isEqualTo(-1);
    assertThat(embeddingBatch.embedded().get(1).toString()).contains(
        "\"discountedValue\": \"8.52\"\n}\n}");
  }

  @Test
  void expectDataUnpublishBeProcessed() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        Data.TYPE_UNPUBLISHED,
        new Data(PAYLOAD, TYPE),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    CloudEvent resultEvent = getResourceFrom(event);

    // then
    assertBase(resultEvent, KEY, event);
    assertThat(resultEvent.getType()).isEqualTo(Data.TYPE_UNPUBLISHED);
  }

  @Test
  void expectHtmlBeProcessed() {
    // given
    String payload = """
        <head>
            <title>Hello Title</title>
        </head>
        <body>
            <h1>Hello H1</h1>
        </body>
        """;
    CloudEvent event = CloudEventUtils.eventWithData(
        "test.html",
        "com.streamx.blueprints.page.published.v1",
        new Data(payload, "page/html"),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    mockEmbeddingsResponse();

    // then
    CloudEvent resultEvent = getResourceFrom(event);
    assertBase(resultEvent, "test.html", event);
  }

  @Test
  void expectHtmlBeProcessedPathWithoutExtension() {
    // given
    String payload = """
        <head>
            <title>Hello Title</title>
        </head>
        <body>
            <h1>Hello H1</h1>
        </body>
        """;
    CloudEvent event = CloudEventUtils.eventWithData(
        "test",
        "com.streamx.blueprints.page.published.v1",
        new Data(payload, "page/html"),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    mockEmbeddingsResponse();

    // then
    CloudEvent resultEvent = getResourceFrom(event);
    assertBase(resultEvent, "test", event);
  }

  @Test
  void expectHtmlBeProcessedPathWithSlash() {
    // given
    String payload = """
        <head>
            <title>Hello Title</title>
        </head>
        <body>
            <h1>Hello H1</h1>
        </body>
        """;
    CloudEvent event = CloudEventUtils.eventWithData(
        "test/",
        "com.streamx.blueprints.page.published.v1",
        new Data(payload, "page/html"),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    mockEmbeddingsResponse();

    // then
    CloudEvent resultEvent = getResourceFrom(event);
    assertBase(resultEvent, "test/", event);
  }

  @Test
  void expectToHaveOutgoingChannelEmpty() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        "test",
        new Data(PAYLOAD, TYPE),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    mockEmbeddingsResponse();

    // then
    assertThat(dataSink.received()).isEmpty();
    dataSource.send(event);
    assertThat(dataSink.received()).isEmpty();
  }

  @Test
  void expectIllegalStateExceptionToBeThrown() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        "test",
        Integer.valueOf(1),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    mockEmbeddingsResponse();

    // then
    assertThat(dataSink.received()).isEmpty();
    dataSource.send(event);
    assertThat(dataSink.received()).isEmpty();
  }

  private void mockEmbeddingsResponse() {
    List<Embedding> embeddings = List.of(new Embedding(new float[]{0.1f, 0.2f, 0.3f}));
    Response<List<Embedding>> response = Response.from(embeddings);
    when(embeddingModel.embedAll(anyList()))
        .thenReturn(response);
  }

  private static void assertBase(CloudEvent resultEvent, String expected, CloudEvent event) {
    assertThat(resultEvent).isNotNull();
    assertThat(resultEvent.getData()).isNotNull();
    assertThat(resultEvent.getSubject()).isEqualTo(expected);
    assertThat(resultEvent.getTime()).isEqualTo(event.getTime());
  }

  private CloudEvent getResourceFrom(CloudEvent pageEvent) {
    dataSource.send(pageEvent);
    await().until(() -> dataSink.received().size() == 1);
    return dataSink.received().getFirst().getPayload();
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
}
