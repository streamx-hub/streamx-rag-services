package com.streamx.hub.rag.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamx.hub.rag.chat.guardrail.CustomOutputGuardrail;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomOutputGuardrailTest {

  private CustomOutputGuardrail guardrail;

  @BeforeEach
  void setUp() {
    guardrail = new CustomOutputGuardrail();
  }

  @Test
  void shouldAcceptValidJson() {
    AiMessage response = AiMessage.from("""
                {
                  "name": "John",
                  "age": 30
                }
                """);

    OutputGuardrailResult result = guardrail.validate(response);

    assertTrue(result.isSuccess());
    assertFalse(result.isReprompt());
    assertEquals(
        dev.langchain4j.guardrail.GuardrailResult.Result.SUCCESS,
        result.result()
    );
  }

  @Test
  void shouldAcceptMarkdownWrappedJson() {
    AiMessage response = AiMessage.from("""
                ```json
                {
                  "name": "John",
                  "age": 30
                }
                ```
                """);

    OutputGuardrailResult result = guardrail.validate(response);

    assertTrue(result.isSuccess());
  }

  @Test
  void shouldRejectInvalidJson() {
    AiMessage response = AiMessage.from("""
                {
                  "name": "John",
                  "age": 30,
                }
                """);

    OutputGuardrailResult result = guardrail.validate(response);

    assertFalse(result.isSuccess());
    assertTrue(result.isReprompt());

    assertEquals(
        "Invalid JSON format",
        result.failures().get(0).message()
    );

    assertTrue(result.getReprompt().isPresent());
    assertTrue(
        result.getReprompt().get().contains("Please correct it.")
    );
    assertTrue(
        result.getReprompt().get().contains(response.text())
    );
  }
}