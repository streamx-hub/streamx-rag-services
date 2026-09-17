package com.streamx.hub.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TranslatingQueryTransformerTest {

  @Inject
  TranslatingQueryTransformer transformer;

  @InjectMock
  TranslationAiService translationService;

  @InjectMock
  ContextualizingAiService contextualizingService;

  @ConfigProperty(name = "streamx.hub.openai-rag-sink.translation-prompt")
  String translationPrompt;

  @Test
  void shouldTranslatePolishQueryToEnglish() {
    when(translationService.translate(translationPrompt, "najtanszy stol")).thenReturn(
        "cheapest table");

    Collection<Query> result = transformer.transform(
        Query.from("najtanszy stol", mock(Metadata.class)));

    assertEquals(1, result.size());
    assertEquals("cheapest table", result.iterator().next().text());
  }

  @Test
  void shouldReturnOriginalOnTranslationFailure() {
    when(translationService.translate(anyString(), anyString()))
        .thenThrow(new RuntimeException("OpenAI error"));

    Collection<Query> result = transformer.transform(
        Query.from("Show me sofas", mock(Metadata.class)));

    assertEquals(1, result.size());
    assertEquals("Show me sofas", result.iterator().next().text());
  }

  @Test
  void shouldContextualizeVaguePronounQuery() {
    when(contextualizingService.contextualize(anyString(), anyString(), anyString()))
        .thenReturn("dimensions of Scandinavian Sofa");
    when(translationService.translate(translationPrompt, "dimensions of Scandinavian Sofa"))
        .thenReturn("dimensions of Scandinavian Sofa");
    Metadata metadata = mock(Metadata.class);
    when(metadata.chatMemory()).thenReturn(List.of(UserMessage.from("hello")));

    Query vague = Query.from("What are its dimensions?", metadata);
    Collection<Query> result = transformer.transform(vague);

    assertEquals(1, result.size());
    assertEquals("dimensions of Scandinavian Sofa", result.iterator().next().text());
  }

  @Test
  void shouldNotContextualizeSelfContainedQuery() {
    when(translationService.translate(translationPrompt, "cheapest lamp")).thenReturn(
        "cheapest lamp");

    Collection<Query> result = transformer.transform(
        Query.from("cheapest lamp", mock(Metadata.class)));

    assertEquals(1, result.size());
    assertEquals("cheapest lamp", result.iterator().next().text());
  }
}
