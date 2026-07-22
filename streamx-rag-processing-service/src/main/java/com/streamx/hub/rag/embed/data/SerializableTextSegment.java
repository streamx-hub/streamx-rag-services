package com.streamx.hub.rag.embed.data;

import java.util.Map;

public record SerializableTextSegment(
    String text,
    Map<String, Object> metadata
) {}