package com.streamx.hub.rag.retrieval;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface TranslationAiService {

  @SystemMessage("{translationPrompt}")
  @UserMessage("{query}")
  String translate(String translationPrompt, String query);
}
