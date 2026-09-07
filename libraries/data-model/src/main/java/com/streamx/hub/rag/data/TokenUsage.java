package com.streamx.hub.rag.data;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TokenUsage(Integer inputTokenCount, Integer outputTokenCount,
                         Integer totalTokenCount) {

  public TokenUsage(dev.langchain4j.model.output.TokenUsage tokenUsage) {
    this(tokenUsage != null ? valueOrDefault(tokenUsage.inputTokenCount()) : -1,
        tokenUsage != null ? valueOrDefault(tokenUsage.outputTokenCount()) : -1,
        tokenUsage != null ? valueOrDefault(tokenUsage.totalTokenCount()) : -1);
  }

  private static int valueOrDefault(Integer value) {
    return value != null ? value : -1;
  }
}

