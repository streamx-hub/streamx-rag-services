package com.streamx.hub.rag;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;
import java.util.Set;

@ConfigMapping(prefix = "streamx.hub.openai-rag-sink")
public interface Configuration {

  Optional<String> defaultNamespace();

  Optional<Set<String>> htmlResourceTypes();

  ChatProfile chatProfile();

  interface ChatProfile {

    Optional<String> name();

    String displayName();

    boolean active();

    String systemPrompt();
  }
}
