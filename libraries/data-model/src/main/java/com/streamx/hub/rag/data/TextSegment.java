package com.streamx.hub.rag.data;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public record TextSegment(
    String text,
    Map<String, Object> metadata
) {}