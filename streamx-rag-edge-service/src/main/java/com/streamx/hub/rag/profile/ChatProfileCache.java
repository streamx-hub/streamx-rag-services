package com.streamx.hub.rag.profile;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatProfileCache {

  /**
   * Simple time-to-live cache: profileName → resolved profile.
   */
  private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

  public void put(String name, ChatProfile profile) {
    cache.put(name, new CachedEntry(profile));
  }

  public ChatProfile getCachedProfile(String name) {
    CachedEntry entry = cache.get(name);
    if (entry == null) {
      return null;
    }
    return entry.profile;
  }

  private record CachedEntry(ChatProfile profile) {

  }
}
