package com.streamx.hub.rag.chat;

import com.streamx.hub.rag.chat.guardrail.CustomOutputGuardrail;
import com.streamx.hub.rag.retrieval.RagRetrievalAugmentorSupplier;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.OutputGuardrails;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = RagRetrievalAugmentorSupplier.class)
@OutputGuardrails(CustomOutputGuardrail.class)
public interface ChatAiService {

  /**
   * Streams a GPT-4o response for the given question.
   *
   * <p>The system prompt is supplied dynamically from the active chat profile,
   * allowing behaviour to be changed at runtime without redeploying the service.
   *
   * @param sessionId    conversation memory key (one memory per session)
   * @param systemPrompt full system prompt for the active profile (with guardrails appended)
   * @param question     the user's question, passed as-is to preserve their language
   */
  @SystemMessage("{systemPrompt}")
  @UserMessage("{question}")
  String chat(@MemoryId String sessionId,
      @V("systemPrompt") String systemPrompt,
      @V("question") String question);
}
