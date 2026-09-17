package com.streamx.hub.rag.chat.guardrail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamx.hub.rag.utils.RagUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CustomOutputGuardrail implements OutputGuardrail {

  private static final Logger LOG = Logger.getLogger(CustomOutputGuardrail.class);
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public OutputGuardrailResult validate(AiMessage responseFromLlm) {
    try {
      LOG.debugf("Response from LLM=%s", responseFromLlm.text());
      mapper.readTree(RagUtils.stripMarkdown(responseFromLlm.text()));
      return success();
    } catch (JsonProcessingException e) {
      String correctionInstruction = String.format(
          "The following output is invalid: %s%nError: %s%nPlease correct it.",
          responseFromLlm.text(), e.getMessage()
      );
      return reprompt("Invalid JSON format", correctionInstruction);
    }
  }
}
