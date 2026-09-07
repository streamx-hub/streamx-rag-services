package com.streamx.hub.rag.sink;

import static com.streamx.hub.rag.sink.OpenAiRagSink.META_SOURCE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.blueprints.data.Resource;
import com.streamx.hub.rag.Channels;
import com.streamx.hub.rag.utils.CloudEventUtils;
import com.streamx.hub.rag.utils.PathUtils;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.cloudevents.CloudEvent;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;

@QuarkusTest
public class OpenAiRagSinkTest {

  public static final String KEY = "pim:B07TMH6289";
  public static final String TYPE = "data/product";
  public static final String TYPE_PUBLISHED = "com.streamx.blueprints.data.published.v1";
  public static final String TYPE_UNPUBLISHED = "com.streamx.blueprints.data.unpublished.v1";

  public static final String PAYLOAD = """
      {
        "vectors": [
          [
            0.1,
            0.2,
            0.3
          ]
        ],
        "embedded": [
          {
            "text": "{\\n\\"id\\": \\"B07TMH6289\\",\\n\\"sku\\": \\"B07TMH6289\\",\\n\\"lang\\": \
            \\"en\\",\\n\\"name\\": \\"LeatherSoft Kids/Youth Recliner with Armrest Storage, \
            5+ Age Group, Light Blue\\",\\n\\"label\\": \\"LeatherSoft Kids/Youth Recliner with \
            Armrest Storage, 5+ Age Group, Light Blue\\",\\n\\"description\\": \
            \\"LeatherSoft Kids/Youth Recliner with Armrest Storage, 5+ Age Group, Light Blue\\",\
            \\n\\"slug\\": leathersoft-kids-youth-recliner-with-armrest-storage-5      \
            -age-group-light-blue-b07tmh6289\\",\\n\\"quantity\\": \\"100\\",\\n\
            \\"type\\": \\"simple\\",\\n\\"price\\": {",
            "metadata": {
              "type": "data/product",
              "index": "0",
              "source_url": "pim/B07TMH6289"
            }
          },
          {
            "text": "\\"value\\": \\"10.02\\",\\n\\"discountedValue\\": \\"8.52\\"\\n}\\n}",
            "metadata": {
              "type": "data/product",
              "index": "1",
              "source_url": "pim/B07TMH6289"
            }
          }
        ],
        "serializableTokenUsage": {
          "inputTokenCount": -1,
          "outputTokenCount": -1,
          "totalTokenCount": -1
        }
      }
      """;

  private InMemorySource<CloudEvent> dataSource;

  @Inject
  @Any
  InMemoryConnector connector;

  @InjectMock
  EmbeddingStore embeddingStore;

  @Mock
  ObjectMapper objectMapper;

  @BeforeEach
  void beforeEach() {
    dataSource = connector.source(Channels.EMBEDDINGS);
  }

  @Test
  void expectDataToBeStoredInEmbeddingStore() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        TYPE_PUBLISHED,
        new Resource(PAYLOAD, TYPE),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    dataSource.send(event);

    // then
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() ->
            verify(embeddingStore).addAll(any(), any()));
  }

  @Test
  void expectDataToBeUnpublished() {
    // given
    CloudEvent event = CloudEventUtils.eventWithData(
        KEY,
        TYPE_UNPUBLISHED,
        new Resource(PAYLOAD, TYPE),
        CloudEventUtils.toOffsetDateTime(1L)
    );

    // when
    dataSource.send(event);

    // then
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() ->
            verify(embeddingStore).removeAll(MetadataFilterBuilder.metadataKey(META_SOURCE_URL)
                .isEqualTo("pim/B07TMH6289")));
  }

  @Test
  void shouldReturnPathWhenIsNotHtmlResource() {
    String path = PathUtils.getPathFrom("test", false, "default");

    assertThat(path).isEqualTo("default/test");
  }

  @CsvSource(delimiterString = "->", textBlock = """
      c.html     ->  c.html
      a/b/c.html ->  a/b/c.html
      c          ->  c/index.html
      a/b/c      ->  a/b/c/index.html
      a/b/c/     ->  a/b/c/index.html
      /          ->  /index.html
      ''         ->  /index.html
      """)
  @ParameterizedTest
  void shouldComputeHtmlResourcePath(String inputPath, String expectedResult) {
    String actualResult = PathUtils.computeHtmlResourcePath(inputPath);
    assertThat(actualResult).isEqualTo(expectedResult);
  }
}
