package com.streamx.hub.rag.chat;

import com.streamx.hub.rag.Configuration;
import com.streamx.hub.rag.profile.ChatProfile;
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
 */
@Path("/api/chat")
public class ChatResource {

  private static final Logger LOG = Logger.getLogger(ChatResource.class);
  private static final String FALLBACK_MSG =
      "Sorry, I’m having a little trouble right now. Please give it another try in a moment.";

  @Inject
  ChatAiService chatService;
  @Inject
  Configuration config;

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

    ChatProfile profile = getProfile();
    String systemPrompt = SystemPrompt.build(profile);

    String sessionId = getSessionId(request);
    LOG.debugf("Chat request: session=%s profile=%s", sessionId, profile.name);

    return chatService.chat(sessionId, systemPrompt, request.question())
        .onFailure().recoverWithMulti(t -> {
          LOG.warnf(t, "Chat stream error for session %s", sessionId);
          return Multi.createFrom().item(FALLBACK_MSG);
        });
  }

  private static String getSessionId(ChatRequest request) {
    return (request.sessionId() != null && !request.sessionId().isBlank())
        ? request.sessionId()
        : UUID.randomUUID().toString();
  }

  private ChatProfile getProfile() {
    Configuration.ChatProfile profileConfig = config.chatProfile();
    return ChatProfile.create(
        profileConfig.name(),
        profileConfig.displayName(),
        profileConfig.systemPrompt());
  }

  /**
   * @param question    the user's question (required)
   * @param sessionId   conversation memory key — callers should persist this across turns
   * @param profileName name of the chat profile to use (optional, defaults to "default")
   */
  public record ChatRequest(String question, String sessionId, String profileName) {

  }
}