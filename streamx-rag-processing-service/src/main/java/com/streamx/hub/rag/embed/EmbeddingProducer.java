package com.streamx.hub.rag.embed;

import static com.streamx.hub.rag.utils.CloudEventUtils.isPublishingType;
import static com.streamx.hub.rag.utils.CloudEventUtils.isUnpublishingType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.blueprints.data.Data;
import com.streamx.blueprints.data.Resource;
import com.streamx.hub.rag.data.EmbeddingBatch;
import com.streamx.hub.rag.utils.CloudEventUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.cloudevents.CloudEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmbeddingProducer {

  private static final String META_SOURCE_URL = "source_url";
  private static final String INDEX_HTML_SUFFIX = "index.html";
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
  EmbeddingModel embeddingModel;

  @PostConstruct
  void init() {
    this.htmlResourceTypes = config.htmlResourceTypes().orElseGet(Collections::emptySet);
    this.defaultNamespace = config.defaultNamespace().orElse("");
  }

  @Incoming(Channels.RESOURCES)
  @Outgoing(Channels.EMBEDDINGS)
  public CloudEvent consume(CloudEvent event) {
    String subject = CloudEventUtils.getSubject(event);
    Resource resource;
    try {
      resource = CloudEventUtils.getDataSkippingUnknownProperties(event, Resource.class);
    } catch (IllegalStateException e) {
      log.warnf(e, "Unsupported event: subject %s, type %s", subject, event.getType());
      return null;
    }
    return process(resource, subject, event);
  }

  private CloudEvent process(Resource resource, String subject, CloudEvent event) {
    String type = event.getType();
    OffsetDateTime eventTime = event.getTime();
    boolean isHtmlResource = htmlResourceTypes.contains(type);
    String path = getPathFrom(subject, isHtmlResource);
    log.tracef("Storing %s resource: subject %s, type %s, event time %s under path %s",
        (isHtmlResource ? "HTML" : "non-HTML"), subject, type,
        Objects.requireNonNull(eventTime).toInstant().toEpochMilli(), path);

    if (isPublishingType(type)) {
      return embed(resource, path, subject, eventTime);
    }
    if (isUnpublishingType(type)) {
      return CloudEventUtils.eventCopyWithData(event, event.getData()).build();
    }
    return null;
  }

  public CloudEvent embed(Resource resource, String path, String key, OffsetDateTime eventTime) {
    log.tracef("Updating resource: %s", path);
    CustomEmbeddingModel customEmbeddingModel = buildModel();
    EmbeddingBatch embeddings = customEmbeddingModel.embed(
        Document.from(resource.getContentAsString(),
            new Metadata(Map.of("type", resource.getType(), META_SOURCE_URL, path))));
    try {
      return CloudEventUtils.eventWithData(key, Data.TYPE_PUBLISHED,
          new Data(objectMapper.writeValueAsString(embeddings), Data.TYPE_PUBLISHED), eventTime);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Payload could not be serialized.", e);
    }
  }

  private String getPathFrom(String subject, boolean isHtmlResource) {
    String namespace = CloudEventUtils.getSubjectNamespace(subject).orElse(defaultNamespace);
    String path = namespace + "/" + CloudEventUtils.getSubjectWithoutNamespace(subject);
    return isHtmlResource ? computeHtmlResourcePath(path) : path;
  }

  static String computeHtmlResourcePath(String path) {
    if (path.endsWith("/")) {
      return path + INDEX_HTML_SUFFIX;
    }
    if (FilenameUtils.getExtension(path).isEmpty()) {
      return path + "/" + INDEX_HTML_SUFFIX;
    }
    return path;
  }

  private CustomEmbeddingModel buildModel() {
    return CustomEmbeddingModel.builder()
        .embeddingModel(embeddingModel)
        .documentSplitter(DocumentSplitters.recursive(
            config.ingestion().chunkSize(),
            config.ingestion().chunkOverlap()
        ))
        .build();
  }
}
