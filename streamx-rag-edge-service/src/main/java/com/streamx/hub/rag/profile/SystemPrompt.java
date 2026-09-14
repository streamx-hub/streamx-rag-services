package com.streamx.hub.rag.profile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SystemPrompt {

  /**
   * Returns the full system prompt for the profile, with topic guardrails appended when
   * {@code topicBlocklist} is non-empty.
   */
  public static String build(ChatProfile profile) {
    String base = profile.systemPrompt;
    if (profile.topicBlocklist == null || profile.topicBlocklist.isBlank()) {
      return base;
    }
    List<String> topics = Arrays.stream(profile.topicBlocklist.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
    if (topics.isEmpty()) {
      return base;
    }
    String list = String.join(", ", topics);
    return base + "\n\n"
        + "STRICT RULE: NEVER discuss the following topics: " + list + ". "
        + "If the user asks about any of these, politely decline and explain "
        + "that you can only help with the topics described above.";
  }
}
