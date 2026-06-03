package com.streamx.hub.rag.chat.dto;

import dev.langchain4j.model.output.structured.Description;

public record Category(
    @Description("The human-readable, URL-friendly portion of a web address that identifies "
        + "a specific page or resource")
    String slug,
    @Description("The name of the product")
    String name) {
}
