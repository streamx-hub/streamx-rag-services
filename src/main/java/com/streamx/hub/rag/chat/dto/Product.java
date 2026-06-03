package com.streamx.hub.rag.chat.dto;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;
import java.util.Map;

@Description("Contains extracted information about product")
public record Product(
    @Description("The id of the product")
    String id,
    @Description("The Stock Keeping Unit")
    String sku,
    @Description("The name of the product")
    String name,
    @Description("The description of the product")
    String description,
    @Description("The human-readable, URL-friendly portion of a web address that identifies "
        + "a specific page or resource")
    String slug,
    int quantity,
    double price,
    double discountedPrice,
    @Description("The primary image of the product")
    String primaryImage,
    @Description("Attributes of the product")
    Map<String, String> attributes,
    @Description("Categories of the product")
    List<String> categories
) {

}
