package com.streamx.hub.rag.profile;

import static com.streamx.hub.rag.profile.SystemPrompt.DEFAULT_SYSTEM_PROMPT;

import com.streamx.hub.rag.Configuration;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Instant;
import java.util.List;
import org.jboss.logging.Logger;
import org.jspecify.annotations.NonNull;

/**
 * Business logic for managing chat profiles.
 *
 * <p>Profiles are cached in-memory for 60 seconds to avoid a database round-trip
 * on every chat request. The cache is invalidated on any write operation.
 */
@ApplicationScoped
public class ChatProfileService {

  private static final Logger LOG = Logger.getLogger(ChatProfileService.class);
  static final String DEFAULT_PROFILE_NAME = "default";

  @Inject
  Configuration config;
  @Inject
  ChatProfileCache profileCache;

  /**
   * Creates the built-in "default" profile on first boot if it does not exist. This ensures the
   * chat endpoint works out-of-the-box without any admin setup.
   */
  @Startup
  @Transactional
  void seedDefaultProfile() {
    if (ChatProfile.existsByName(DEFAULT_PROFILE_NAME)) {
      LOG.debugf("Default chat profile already exists — skipping seed");
      return;
    }

    ChatProfile profile = ChatProfile.create(
        DEFAULT_PROFILE_NAME,
        "Default — Product & Content Assistant",
        DEFAULT_SYSTEM_PROMPT,
        0.50
    );
    profile.persist();
    LOG.info("Seeded default chat profile");
  }

  /**
   * Resolves a profile by name, falling back to the "default" profile if the requested name is
   * blank or not found.
   *
   * <p>{@code TxType.SUPPORTS} joins an existing transaction when present and
   * opens a read-only session otherwise, which is required for Panache queries called from a
   * non-transactional context (e.g. the SSE chat endpoint).
   *
   * @throws IllegalStateException if neither the requested profile nor the default profile exists
   *                               (should never happen after seed)
   */
  @Transactional(TxType.SUPPORTS)
  public ChatProfile resolveOrDefault(String profileName) {
    Configuration.ChatProfile envChatProfile = config.chatProfile();
    String name = getName(profileName, envChatProfile);

    ChatProfile cached = profileCache.getCachedProfile(name);
    if (cached != null) {
      return cached;
    }

    ChatProfile profile = getEnvironmentProfile(name, envChatProfile);
    if (profile != null) {
      return profile;
    }
    return getDatabaseProfile(name);
  }

  private ChatProfile getEnvironmentProfile(String name,
      Configuration.ChatProfile env) {
    if (env.name().isEmpty() || !env.active()) {
      return null;
    }

    ChatProfile profile = ChatProfile.create(
        env.name().get(),
        env.displayName(),
        env.systemPrompt());

    profileCache.put(name, profile);
    return profile;
  }

  private ChatProfile getDatabaseProfile(String name) {
    ChatProfile profile = ChatProfile.findByName(name);

    if (profile == null) {
      LOG.warnf("Profile '%s' not found — falling back to default", name);
      profile = ChatProfile.findByName(DEFAULT_PROFILE_NAME);
    }
    if (profile == null) {
      throw new IllegalStateException("Default chat profile missing — run seed");
    }
    if (!profile.active) {
      LOG.warnf("Profile '%s' is inactive — falling back to default", name);
      profile = ChatProfile.findByName(DEFAULT_PROFILE_NAME);
    }

    profileCache.put(name, profile);
    return profile;
  }

  private static @NonNull String getName(String profileName,
      Configuration.ChatProfile envChatProfile) {
    return (profileName == null || profileName.isBlank())
        ? envChatProfile.name().orElse(DEFAULT_PROFILE_NAME)
        : profileName.trim();
  }

  @Transactional(TxType.SUPPORTS)
  public List<ChatProfile> listAll() {
    return ChatProfile.listAll();
  }

  @Transactional(TxType.SUPPORTS)
  public ChatProfile findByName(String name) {
    return ChatProfile.findByName(name);
  }

  @Transactional
  public ChatProfile create(ChatProfileRequest req) {
    if (ChatProfile.existsByName(req.name())) {
      throw new IllegalArgumentException("Profile with name '" + req.name() + "' already exists");
    }
    ChatProfile profile = new ChatProfile();
    profile.name = req.name().trim();   // name is set only here, never via applyRequest
    applyRequest(profile, req);
    profile.createdAt = Instant.now();
    profile.persist();
    profileCache.invalidateCache();
    LOG.infof("Created chat profile: %s", profile.name);
    return profile;
  }

  /**
   * Applies mutable fields from the request onto the profile entity.
   *
   * <p>{@code name} is intentionally excluded — it is the resource identifier
   * (the URL path parameter) and must never be changed via an update request. Changing the name
   * would silently break all callers referencing the old name.
   */
  private void applyRequest(ChatProfile p, ChatProfileRequest req) {
    p.setDisplayName(req.displayName());
    p.setSystemPrompt(req.systemPrompt());
    p.setMaxResults(req.maxResults());
    p.setMinScore(req.minScore());
    p.setTopicBlocklist(req.topicBlocklist());
    p.setActive(req.active());
  }
}
