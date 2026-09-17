package com.streamx.hub.rag.embed;

import com.streamx.hub.rag.data.EmbeddingBatch;
import com.streamx.hub.rag.data.TextSegment;
import com.streamx.hub.rag.data.TokenUsage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.spi.ServiceHelper;
import dev.langchain4j.spi.data.document.splitter.DocumentSplitterFactory;
import dev.langchain4j.spi.model.embedding.EmbeddingModelFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.logging.Logger;

public class CustomEmbeddingModel {

  private static final Logger log = Logger.getLogger(CustomEmbeddingModel.class);
  private final DocumentSplitter documentSplitter;
  private final EmbeddingModel embeddingModel;

  public CustomEmbeddingModel(DocumentSplitter documentSplitter, EmbeddingModel embeddingModel) {
    this.documentSplitter = Utils.getOrDefault(documentSplitter,
        CustomEmbeddingModel::loadDocumentSplitter);
    this.embeddingModel = ValidationUtils.ensureNotNull(
        Utils.getOrDefault(embeddingModel, CustomEmbeddingModel::loadEmbeddingModel),
        "embeddingModel");
  }

  private static DocumentSplitter loadDocumentSplitter() {
    Collection<DocumentSplitterFactory> factories = ServiceHelper.loadFactories(
        DocumentSplitterFactory.class);
    if (factories.size() > 1) {
      throw new RuntimeException(
          "Conflict: multiple document splitters have been found in the classpath. "
          + "Please explicitly specify the one you wish to use.");
    } else {
      return factories.stream()
          .findFirst()
          .map(DocumentSplitterFactory::create)
          .map(documentSplitter -> {
            log.debugf("Loaded the following document splitter through SPI: %s", documentSplitter);
            return documentSplitter;
          })
          .orElse(null);
    }
  }

  private static EmbeddingModel loadEmbeddingModel() {
    Collection<EmbeddingModelFactory> factories = ServiceHelper.loadFactories(
        EmbeddingModelFactory.class);
    if (factories.size() > 1) {
      throw new RuntimeException(
          "Conflict: multiple embedding models have been found in the classpath. "
          + "Please explicitly specify the one you wish to use.");
    } else {
      return factories.stream()
          .findFirst()
          .map(EmbeddingModelFactory::create)
          .map(embeddingModel -> {
            log.debugf("Loaded the following embedding model through SPI: %s", embeddingModel);
            return embeddingModel;
          })
          .orElse(null);
    }
  }

  public EmbeddingBatch embed(Document document) {
    return this.embed(Collections.singletonList(document));
  }

  public EmbeddingBatch embed(List<Document> documents) {
    log.debugf("Starting to ingest %s documents", documents.size());
    List<dev.langchain4j.data.segment.TextSegment> segments;
    if (this.documentSplitter != null) {
      segments = this.documentSplitter.splitAll(documents);
      log.debugf("Documents were split into %s text segments", segments.size());
    } else {
      segments = documents.stream().map(Document::toTextSegment).toList();
    }

    log.debugf("Starting to embed %s text segments", segments.size());
    Response<List<Embedding>> embeddingsResponse = this.embeddingModel.embedAll(segments);
    log.debugf("Finished embedding %s text segments", segments.size());

    return new EmbeddingBatch(getVectors(embeddingsResponse), getSerializableTextSegment(segments),
        new TokenUsage(embeddingsResponse.tokenUsage()));
  }

  private List<TextSegment> getSerializableTextSegment(
      List<dev.langchain4j.data.segment.TextSegment> segments) {
    return segments.stream()
        .map(segment -> new TextSegment(
            segment.text(),
            segment.metadata().toMap()))
        .toList();
  }

  private List<float[]> getVectors(Response<List<Embedding>> response) {
    return response.content().stream()
        .map(Embedding::vector)
        .toList();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private DocumentSplitter documentSplitter;
    private EmbeddingModel embeddingModel;

    public Builder documentSplitter(DocumentSplitter documentSplitter) {
      this.documentSplitter = documentSplitter;
      return this;
    }

    public Builder embeddingModel(EmbeddingModel embeddingModel) {
      this.embeddingModel = embeddingModel;
      return this;
    }

    public CustomEmbeddingModel build() {
      return new CustomEmbeddingModel(this.documentSplitter, this.embeddingModel);
    }
  }
}
