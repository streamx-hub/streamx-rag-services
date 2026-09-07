package com.streamx.hub.rag.sink;

import static com.streamx.hub.rag.utils.CloudEventUtils.isPublishingType;
import static com.streamx.hub.rag.utils.CloudEventUtils.isUnpublishingType;
import static com.streamx.hub.rag.utils.PathUtils.getPathFrom;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.blueprints.data.Resource;
import com.streamx.hub.rag.Channels;
import com.streamx.hub.rag.Configuration;
import com.streamx.hub.rag.data.EmbeddingBatch;
import com.streamx.hub.rag.data.TextSegment;
import com.streamx.hub.rag.utils.CloudEventUtils;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.IngestionResult;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OpenAiRagSink {

  static final String META_SOURCE_URL = "source_url";

  private static final ObjectMapper objectMapper = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
  );

  private Set<String> htmlResourceTypes;
  private String defaultNamespace;

  @Inject
  Logger log;

  @Inject
  Configuration config;

  @Inject
  EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore;

  @PostConstruct
  void init() {
    this.htmlResourceTypes = config.htmlResourceTypes();
    this.defaultNamespace = config.defaultNamespace();
    this.embeddingStore = Optional.ofNullable(embeddingStore)
        .orElseThrow(() -> new IllegalArgumentException("embeddingStore cannot be null"));
  }

  @Incoming(Channels.EMBEDDINGS)
  public Uni<Void> consume(CloudEvent event) {
    String subject = CloudEventUtils.getSubject(event);
    Resource resource = CloudEventUtils.getDataSkippingUnknownProperties(event, Resource.class);

    return Optional.ofNullable(resource)
        .map(res -> getEmbeddingBatch(resource.getContentAsBytes()))
        .map(batch -> process(
            batch,
            subject,
            event.getType(),
            Objects.requireNonNull(event.getTime()).toInstant().toEpochMilli()
        ))
        .orElseGet(() -> Uni.createFrom().voidItem());
  }

  private EmbeddingBatch getEmbeddingBatch(byte[] content) {
    try {
      return objectMapper.readValue(content, EmbeddingBatch.class);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot parse data", e);
    }
  }

  private Uni<Void> process(EmbeddingBatch embeddingBatch, String subject, String type,
      long eventTime) {
    boolean isHtmlResource = htmlResourceTypes.contains(type);
    String path = getPathFrom(subject, isHtmlResource, defaultNamespace);
    log.tracef("Storing %s resource: subject %s, type %s, event time %s under path %s",
        (isHtmlResource ? "HTML" : "non-HTML"), subject, type, eventTime, path);
    return updateStorage(embeddingBatch, path, type);
  }

  private Uni<Void> updateStorage(EmbeddingBatch embeddingBatch, String path, String type) {
    if (isPublishingType(type)) {
      return ingest(embeddingBatch, path);
    }
    if (isUnpublishingType(type)) {
      removeByUrl(path);
      log.tracef("Resource deleted: %s", path);
    }
    return Uni.createFrom().voidItem();
  }

  public Uni<Void> ingest(EmbeddingBatch embeddingBatch, String path) {
    log.tracef("Updating resource: %s", path);
    return Uni.createFrom().item(() -> ingest(embeddingBatch))
        .runSubscriptionOn(Infrastructure.getDefaultExecutor()).replaceWithVoid();
  }

  public IngestionResult ingest(EmbeddingBatch embeddingBatch) {
    if (embeddingBatch.embedded() == null) {
      return new IngestionResult(new TokenUsage(0));
    }
    log.debugf("Starting to store %s text segments into the embedding store",
        embeddingBatch.embedded().size());
    this.embeddingStore.addAll(getEmbeddings(embeddingBatch.vectors()),
        getTextSegments(embeddingBatch.embedded()));
    log.debugf("Finished storing %s text segments into the embedding store",
        embeddingBatch.embedded().size());
    return new IngestionResult(new TokenUsage(embeddingBatch.tokenUsage().inputTokenCount()));
  }

  private static List<dev.langchain4j.data.segment.TextSegment> getTextSegments(
      List<TextSegment> embedded) {
    return embedded.stream()
        .map(segment -> new dev.langchain4j.data.segment.TextSegment(segment.text(),
            new Metadata(segment.metadata())))
        .toList();
  }

  private static List<Embedding> getEmbeddings(List<float[]> vectors) {
    return vectors.stream()
        .map(Embedding::from)
        .toList();
  }

  private void removeByUrl(String url) {
    embeddingStore.removeAll(
        MetadataFilterBuilder.metadataKey(META_SOURCE_URL)
            .isEqualTo(url));
  }
}
