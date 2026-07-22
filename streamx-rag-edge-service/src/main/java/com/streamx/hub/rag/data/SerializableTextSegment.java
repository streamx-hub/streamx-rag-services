package com.streamx.hub.rag.data;

import java.util.Map;

public record SerializableTextSegment(
    String text,
    Map<String, Object> metadata
) {}