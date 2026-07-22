package com.streamx.hub.rag.retrieval;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class RagRetrievalAugmenterSupplierTest {

  @Test
  void shouldCreateRetrievalAugmenter() {

    RagRetrievalAugmenterSupplier supplier = new RagRetrievalAugmenterSupplier(
        Mockito.mock(EmbeddingStore.class),
        Mockito.mock(EmbeddingModel.class),
        Mockito.mock(TranslatingQueryTransformer.class)
    );

    assertThat(supplier.get()).isNotNull();
  }
}
