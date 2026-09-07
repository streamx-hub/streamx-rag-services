package com.streamx.hub.rag;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Set;

@ConfigMapping(prefix = "streamx.hub.openai-rag-sink")
public interface Configuration {

  @WithDefault("")
  String defaultNamespace();

  @WithDefault("")
  Set<String> htmlResourceTypes();

  ChatProfile chatProfile();

  interface ChatProfile {

    String name();

    String displayName();

    boolean active();

    String systemPrompt();
  }
}
