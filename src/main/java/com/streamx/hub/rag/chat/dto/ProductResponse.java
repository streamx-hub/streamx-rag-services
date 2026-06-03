package com.streamx.hub.rag.chat.dto;

import java.util.List;

public record ProductResponse(
    String message,
    List<Product> products,
    List<Category> categories
) {

}
