package com.streamx.hub.rag.data;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record EmbeddingBatch(
    List<float[]> vectors,
    List<TextSegment> embedded,
    TokenUsage tokenUsage
) {

}
