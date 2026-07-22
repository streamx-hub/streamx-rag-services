package com.streamx.hub.rag.embed.data;

import dev.langchain4j.model.output.TokenUsage;

public record SerializableTokenUsage(Integer inputTokenCount, Integer outputTokenCount,
                                     Integer totalTokenCount) {

  public SerializableTokenUsage(TokenUsage tokenUsage) {
    this(tokenUsage != null ? valueOrDefault(tokenUsage.inputTokenCount()) : -1,
        tokenUsage != null ? valueOrDefault(tokenUsage.outputTokenCount()) : -1,
        tokenUsage != null ? valueOrDefault(tokenUsage.totalTokenCount()) : -1);
  }

  private static int valueOrDefault(Integer value) {
    return value != null ? value : -1;
  }
}
