package com.streamx.hub.rag.retrieval;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;

@ApplicationScoped
public class RagRetrievalAugmenterSupplier implements Supplier<RetrievalAugmentor> {

  private final EmbeddingStore<TextSegment> embeddingStore;
  private final EmbeddingModel embeddingModel;
  private final TranslatingQueryTransformer queryTransformer;

  public RagRetrievalAugmenterSupplier(
      EmbeddingStore<TextSegment> embeddingStore,
      EmbeddingModel embeddingModel,
      TranslatingQueryTransformer queryTransformer) {
    this.embeddingStore = embeddingStore;
    this.embeddingModel = embeddingModel;
    this.queryTransformer = queryTransformer;
  }

  @Override
  public RetrievalAugmentor get() {
    EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel)
        .maxResults(10)
        .minScore(0.65)
        .build();

    return DefaultRetrievalAugmentor.builder()
        .queryTransformer(queryTransformer)
        .contentRetriever(contentRetriever)
        .build();
  }
}
