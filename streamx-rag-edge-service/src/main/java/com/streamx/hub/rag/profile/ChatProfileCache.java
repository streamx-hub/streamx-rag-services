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
  private static final long CACHE_TTL_MS = 60_000;

  public void put(String name, ChatProfile profile) {
    cache.put(name, new CachedEntry(profile));
  }

  public ChatProfile getCachedProfile(String name) {
    CachedEntry entry = cache.get(name);
    if (entry == null || entry.isExpired()) {
      return null;
    }
    return entry.profile;
  }

  /**
   * Clears the entire cache on any write.
   *
   * <p>A targeted eviction would miss entries where a missing profile was
   * cached as a pointer to "default" (e.g. cache["customer-support"] = defaultProfile). A full
   * clear guarantees consistency and is acceptable because profile changes are rare and the cache
   * rebuilds in < 1 ms on the next request.
   */
  public void invalidateCache() {
    cache.clear();
  }

  private record CachedEntry(ChatProfile profile, long expiresAt) {

    CachedEntry(ChatProfile profile) {
      this(profile, System.currentTimeMillis() + CACHE_TTL_MS);
    }

    boolean isExpired() {
      return System.currentTimeMillis() > expiresAt;
    }
  }
}
