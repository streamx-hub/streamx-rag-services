package com.streamx.hub.rag;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "streamx.hub.openai-rag-sink")
public interface Configuration {

  @WithDefault("")
  String defaultNamespace();

  String contextualizationPrompt();

  String translationPrompt();

  ChatProfile chatProfile();

  interface ChatProfile {

    String name();

    String displayName();

    String systemPrompt();
  }
}
