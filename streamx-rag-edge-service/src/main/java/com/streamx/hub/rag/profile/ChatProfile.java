package com.streamx.hub.rag.profile;

public class ChatProfile {

  /**
   * Unique slug used as the API identifier, e.g. "default", "customer-support".
   */
  public String name;

  /**
   * Human-readable label shown in admin tooling.
   */
  public String displayName;

  /**
   * Full system prompt sent to GPT-4o for every request using this profile. Supports plain text; a
   * NEVER-discuss guardrail is appended automatically when {@link #topicBlocklist} is non-empty.
   */
  public String systemPrompt;

  /**
   * Comma-separated list of topics the assistant must not discuss. Example:
   * {@code "competitors,internal pricing,legal advice"}
   *
   * <p>Each entry is appended to the system prompt as a hard guardrail.
   * Leave blank to impose no restrictions.
   */
  public String topicBlocklist;

  /**
   * When false, chat requests for this profile fall back to the "default" profile. Use this to
   * temporarily disable a profile without deleting it.
   */
  public boolean active = true;

  public static ChatProfile create(String name, String displayName, String systemPrompt) {
    ChatProfile p = new ChatProfile();
    p.name = name;
    p.displayName = displayName;
    p.systemPrompt = systemPrompt;
    return p;
  }
}
