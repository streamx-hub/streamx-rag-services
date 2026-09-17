package com.streamx.hub.rag.embed;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

@ConfigMapping(prefix = "streamx.hub.openai-rag-sink")
public interface Configuration {

  Optional<String> defaultNamespace();

  IngestionConfig ingestion();

  interface IngestionConfig {

    @WithDefault("500")
    int chunkSize();

    @WithDefault("50")
    int chunkOverlap();
  }
}
