package com.streamx.hub.rag.data;

import dev.langchain4j.model.output.TokenUsage;

public record SerializableTokenUsage(Integer inputTokenCount, Integer outputTokenCount,
                                     Integer totalTokenCount) {

  public TokenUsage getTokenUsage() {
    return new TokenUsage(inputTokenCount, outputTokenCount, totalTokenCount);
  }
}
