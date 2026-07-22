package com.streamx.hub.rag.embed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.streamx.hub.rag.embed.data.EmbeddingBatch;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.spi.ServiceHelper;
import dev.langchain4j.spi.data.document.splitter.DocumentSplitterFactory;
import dev.langchain4j.spi.model.embedding.EmbeddingModelFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@QuarkusTest
public class CustomEmbeddingModelTest {

  @InjectMock
  EmbeddingModel embeddingModel;

  @Test
  void expectCustomEmbeddingModelBeCreatedWithEmbeddingModelFactory() {
    EmbeddingModelFactory factory = mock(EmbeddingModelFactory.class);
    EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

    List<Embedding> embeddings = List.of(new Embedding(new float[]{0.1f, 0.2f, 0.3f}));
    Response<List<Embedding>> response = Response.from(embeddings);
    when(embeddingModel.embedAll(anyList()))
        .thenReturn(response);
    when(factory.create()).thenReturn(embeddingModel);

    try (MockedStatic<ServiceHelper> mocked = Mockito.mockStatic(ServiceHelper.class)) {

      mocked.when(() ->
              ServiceHelper.loadFactories(EmbeddingModelFactory.class))
          .thenReturn(List.of(factory));

      CustomEmbeddingModel model = CustomEmbeddingModel.builder()
          .embeddingModel(null)
          .documentSplitter(null)
          .build();

      assertThat(mocked).isNotNull();
      EmbeddingBatch embeddingBatch = model.embed(
          Document.from("Test", Metadata.metadata("test", "test")));
      assertThat(embeddingBatch).isNotNull();
      assertThat(embeddingBatch.embedded().get(0).text()).isEqualTo("Test");
    }
  }

  @Test
  void expectCustomEmbeddingModelBeCreatedWithDocumentFactory() {
    DocumentSplitterFactory factory = mock(DocumentSplitterFactory.class);
    DocumentSplitter documentSplitter = mock(DocumentSplitter.class);

    when(factory.create()).thenReturn(documentSplitter);

    try (MockedStatic<ServiceHelper> mocked = Mockito.mockStatic(ServiceHelper.class)) {

      mocked.when(() ->
              ServiceHelper.loadFactories(DocumentSplitterFactory.class))
          .thenReturn(List.of(factory));

      CustomEmbeddingModel model = CustomEmbeddingModel.builder()
          .embeddingModel(embeddingModel)
          .documentSplitter(null)
          .build();

      assertThat(model).isNotNull();
    }
  }

  @Test
  void expectRuntimeExceptionToBeThrownOnDocumentSplitter() {
    DocumentSplitterFactory factory = mock(DocumentSplitterFactory.class);
    DocumentSplitterFactory factory2 = mock(DocumentSplitterFactory.class);

    try (MockedStatic<ServiceHelper> mocked = Mockito.mockStatic(ServiceHelper.class)) {
      mocked.when(() ->
              ServiceHelper.loadFactories(DocumentSplitterFactory.class))
          .thenReturn(List.of(factory, factory2));

      assertThatExceptionOfType(RuntimeException.class)
          .isThrownBy(() -> CustomEmbeddingModel.builder()
              .embeddingModel(embeddingModel)
              .documentSplitter(null)
              .build());
    }
  }

  @Test
  void expectRuntimeExceptionToBeThrownOnEmbeddingModel() {
    EmbeddingModelFactory factory = mock(EmbeddingModelFactory.class);
    EmbeddingModelFactory factory2 = mock(EmbeddingModelFactory.class);

    try (MockedStatic<ServiceHelper> mocked = Mockito.mockStatic(ServiceHelper.class)) {
      mocked.when(() ->
              ServiceHelper.loadFactories(EmbeddingModelFactory.class))
          .thenReturn(List.of(factory, factory2));

      assertThatExceptionOfType(RuntimeException.class)
          .isThrownBy(() -> CustomEmbeddingModel.builder()
              .embeddingModel(null)
              .documentSplitter(null)
              .build());
    }
  }
}
