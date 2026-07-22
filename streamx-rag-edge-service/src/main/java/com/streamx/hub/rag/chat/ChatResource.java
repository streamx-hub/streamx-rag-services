package com.streamx.hub.rag.chat;

import com.streamx.hub.rag.profile.ChatProfile;
import com.streamx.hub.rag.profile.ChatProfileService;
import com.streamx.hub.rag.profile.SystemPrompt;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

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
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.TEXT_PLAIN)
  public Multi<String> chat(ChatRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      return Multi.createFrom().item("Please enter a question.");
    }

    // Resolve the profile for this request and make it available to the
    // retrieval augmentor (maxResults, minScore) via the @RequestScoped ActiveProfile bean.
    ChatProfile profile = profileService.resolveOrDefault(request.profileName());
    String systemPrompt = SystemPrompt.build(profile);

    String sessionId = getSessionId(request);
    LOG.debugf("Chat request: session=%s profile=%s", sessionId, profile.name);

    return chatService.chat(sessionId, systemPrompt, request.question())
        .onFailure().recoverWithMulti(t -> {
          LOG.errorf(t, "Chat stream error for session %s", sessionId);
          return Multi.createFrom().item(FALLBACK_MSG);
        });
  }

  private static String getSessionId(ChatRequest request) {
    return (request.sessionId() != null && !request.sessionId().isBlank())
        ? request.sessionId()
        : UUID.randomUUID().toString();
  }

  /**
   * @param question    the user's question (required)
   * @param sessionId   conversation memory key — callers should persist this across turns
   * @param profileName name of the chat profile to use (optional, defaults to "default")
   */
  public record ChatRequest(String question, String sessionId, String profileName) {

  }
}