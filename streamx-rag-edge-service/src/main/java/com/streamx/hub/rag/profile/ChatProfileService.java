package com.streamx.hub.rag.profile;

import static com.streamx.hub.rag.profile.SystemPrompt.DEFAULT_SYSTEM_PROMPT;

import com.streamx.hub.rag.Configuration;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

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
  public ChatProfile getProfileOrDefault(String profileName) {
    if (StringUtils.isNotBlank(profileName)) {
      ChatProfile cached = profileCache.getCachedProfile(profileName);
      if (cached != null) {
        return cached;
      }
    }

    ChatProfile profile = getProfile(config.chatProfile());
    if (profile != null) {
      return profile;
    }
    return getPersistedProfile(profileName);
  }

  private ChatProfile getProfile(Configuration.ChatProfile config) {
    if (config.name().isEmpty() || !config.active()) {
      return null;
    }

    String name = config.name();
    ChatProfile profile = ChatProfile.create(
        name,
        config.displayName(),
        config.systemPrompt());

    profileCache.put(name, profile);
    return profile;
  }

  private ChatProfile getPersistedProfile(String name) {
    ChatProfile profile = ChatProfile.findByName(name);

    if (profile == null) {
      LOG.debugf("Profile '%s' not found — falling back to default", name);
      profile = ChatProfile.findByName(DEFAULT_PROFILE_NAME);
    }
    if (profile == null) {
      throw new IllegalStateException("Default chat profile missing — run seed");
    }
    if (!profile.active) {
      LOG.debugf("Profile '%s' is inactive — falling back to default", name);
      profile = ChatProfile.findByName(DEFAULT_PROFILE_NAME);
    }

    profileCache.put(name, profile);
    return profile;
  }

  @Transactional(TxType.SUPPORTS)
  public List<ChatProfile> listAll() {
    return ChatProfile.listAll();
  }

  @Transactional(TxType.SUPPORTS)
  public ChatProfile findByName(String name) {
    return ChatProfile.findByName(name);
  }
}
