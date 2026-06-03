package com.streamx.hub.rag.chat;

import com.streamx.hub.rag.chat.dto.ProductResponse;
import com.streamx.hub.rag.profile.ActiveProfile;
import com.streamx.hub.rag.profile.ChatProfile;
import com.streamx.hub.rag.profile.ChatProfileService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Chat endpoint. Streams GPT-4o responses token by token.
 *
 * <p>Callers may optionally pass a {@code profileName} in the request body to
 * select a specific business use-case configuration. Omitting it (or sending {@code "default"})
 * uses the default assistant behaviour.
 *
 */
@Path("/api/chat")
public class ChatResource {

  private static final Logger LOG = Logger.getLogger(ChatResource.class);
  private static final String FALLBACK_MSG =
      "I'm having trouble reaching the AI service right now. "
          + "Please wait a moment and try again.";

  @Inject
  ChatAiService chatService;
  @Inject
  ChatProfileService profileService;
  @Inject
  ActiveProfile activeProfile;

  /**
   * Chat endpoint.
   *
   * <p>Rate-limited to 30 requests per minute per JVM instance.
   * On OpenAI errors, streams a fallback message instead of an HTTP 500.
   *
   * <p>Request body:
   * <pre>{@code
   * {
   *   "question":    "Show me a grey corner sofa under £1500",
   *   "sessionId":   "abc-123",          // optional — auto-generated if absent
   *   "profileName": "customer-support"  // optional — defaults to "default"
   * }
   * }</pre>
   */
  @POST
  @Blocking
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ProductResponse chat(ChatRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      return new ProductResponse(
          "Please enter a question.",
          List.of(),
          List.of()
      );
    }

    String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
        ? request.sessionId()
        : UUID.randomUUID().toString();

    // Resolve the profile for this request and make it available to the
    // retrieval augmentor (maxResults, minScore) via the @RequestScoped ActiveProfile bean.
    ChatProfile profile = profileService.resolveOrDefault(request.profileName());
    activeProfile.set(profile);

    String systemPrompt = profileService.buildSystemPrompt(profile);

    LOG.debugf("Chat request: session=%s profile=%s", sessionId, profile.name);

    try {
      return chatService.chat(
          sessionId,
          systemPrompt,
          request.question()
      );
    } catch (Exception e) {
      LOG.errorf(e, "Chat error for session %s", sessionId);

      return new ProductResponse(
          FALLBACK_MSG,
          List.of(),
          List.of()
      );
    }
  }

  /**
   * @param question    the user's question (required)
   * @param sessionId   conversation memory key — callers should persist this across turns
   * @param profileName name of the chat profile to use (optional, defaults to "default")
   */
  public record ChatRequest(String question, String sessionId, String profileName) {

  }
}