package com.streamx.hub.rag.retrieval;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * Rewrites vague follow-up questions using conversation history so they become self-contained
 * queries suitable for vector search.
 * <p>
 * Example: History: "User: Show me sofas / Bot: Here is the Nordic Sofa..." Question: "What are its
 * dimensions?" Output:  "dimensions of Nordic Sofa"
 */
@RegisterAiService
public interface ContextualizingAiService {

  @SystemMessage("{contextualizationPrompt}")
  @UserMessage("Conversation history (last messages):\n{history}\n\nLatest question: {question}")
  String contextualize(String contextualizationPrompt, String history, String question);
}
