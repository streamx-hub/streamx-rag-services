package com.streamx.hub.rag.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SystemPromptTest {

  @Test
  void shouldReturnBasePromptWhenTopicBlocklistIsNull() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "You are a helpful assistant.";
    profile.topicBlocklist = null;

    String result = SystemPrompt.build(profile);

    assertEquals(
        "You are a helpful assistant.",
        result
    );
  }

  @Test
  void shouldReturnBasePromptWhenTopicBlocklistIsBlank() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "You are a helpful assistant.";
    profile.topicBlocklist = "   ";

    String result = SystemPrompt.build(profile);

    assertEquals(
        "You are a helpful assistant.",
        result
    );
  }

  @Test
  void shouldReturnBasePromptWhenTopicBlocklistContainsOnlyCommasAndWhitespace() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "You are a helpful assistant.";
    profile.topicBlocklist = " , ,   , ";

    String result = SystemPrompt.build(profile);

    assertEquals(
        "You are a helpful assistant.",
        result
    );
  }

  @Test
  void shouldAppendGuardrailsForMultipleTopics() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "You are a helpful assistant.";
    profile.topicBlocklist = "politics, religion, gambling";

    String result = SystemPrompt.build(profile);

    assertEquals(
        expectedPrompt("You are a helpful assistant.", "politics, religion, gambling"),
        result);
  }

  @Test
  void shouldTrimTopicsAndIgnoreEmptyEntries() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "You are a helpful assistant.";
    profile.topicBlocklist = " politics , , religion,   gambling  , ";

    String result = SystemPrompt.build(profile);

    assertEquals(
        expectedPrompt("You are a helpful assistant.", "politics, religion, gambling"),
        result
    );
  }

  @Test
  void shouldPreserveBasePromptExactly() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "Line 1\nLine 2\nLine 3";
    profile.topicBlocklist = null;

    String result = SystemPrompt.build(profile);

    assertEquals(
        "Line 1\nLine 2\nLine 3",
        result
    );
  }

  private static String expectedPrompt(String base, String topics) {
    return base + "\n\n"
           + "STRICT RULE: NEVER discuss the following topics: " + topics + ". "
           + "If the user asks about any of these, politely decline and explain "
           + "that you can only help with the topics described above.";
  }
}
