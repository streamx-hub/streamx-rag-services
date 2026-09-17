package com.streamx.hub.rag.utils;

public class RagUtils {

  private RagUtils() {
    // no instance
  }

  public static String stripMarkdown(String responseFromLlmText) {
    return responseFromLlmText
        .replace("```json", "")
        .replace("```", "")
        .trim();
  }
}
