package com.streamx.hub.rag.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ChatProfileServiceTest {

  private ChatProfileService service;

  ChatProfileServiceTest(ChatProfileService chatProfileService) {
    this.service = chatProfileService;
  }

  @BeforeEach
  @Transactional
  void cleanup() {
    ChatProfile.deleteAll();
  }

  @Test
  @TestTransaction
  void shouldCreateDefaultProfileOnStartup() {
    service.seedDefaultProfile();

    ChatProfile profile = ChatProfile.findByName(ChatProfileService.DEFAULT_PROFILE_NAME);

    assertNotNull(profile);
    assertEquals(ChatProfileService.DEFAULT_PROFILE_NAME, profile.name);
    assertEquals(SystemPrompt.DEFAULT_SYSTEM_PROMPT, profile.systemPrompt);
  }

  @Test
  @TestTransaction
  void shouldNotCreateDuplicateDefaultProfile() {
    service.seedDefaultProfile();
    service.seedDefaultProfile();

    long count = ChatProfile.count("name", ChatProfileService.DEFAULT_PROFILE_NAME);

    assertEquals(1, count);
  }

  @Test
  @TestTransaction
  void shouldCreateProfile() {
    ChatProfileRequest request = new ChatProfileRequest(
        "support",
        "Support Assistant",
        "Answer support questions",
        10,
        0.7,
        "billing,password",
        true
    );

    ChatProfile created = service.create(request);

    assertNotNull(created);
    assertEquals("support", created.name);
    assertEquals("Support Assistant", created.displayName);
    assertEquals(10, created.maxResults);
    assertEquals(0.7, created.minScore);
    assertEquals("billing,password", created.topicBlocklist);
  }

  @Test
  @TestTransaction
  void shouldRejectDuplicateProfileCreation() {
    ChatProfileRequest request = new ChatProfileRequest(
        "duplicate",
        "One",
        "Prompt",
        5,
        0.5,
        null,
        true
    );

    service.create(request);

    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  @Test
  @TestTransaction
  void shouldFallbackToDefaultProfileWhenMissing() {
    service.seedDefaultProfile();
    ChatProfile resolved = service.resolveOrDefault("does-not-exist");

    assertEquals(
        ChatProfileService.DEFAULT_PROFILE_NAME,
        resolved.name
    );
  }

  @Test
  @TestTransaction
  void shouldFallbackWhenProfileInactive() {
    service.seedDefaultProfile();

    ChatProfile inactive = new ChatProfile();
    inactive.name = "inactive";
    inactive.displayName = "Inactive";
    inactive.systemPrompt = "Prompt";
    inactive.active = false;
    inactive.persist();

    ChatProfile resolved = service.resolveOrDefault("inactive");

    assertEquals(ChatProfileService.DEFAULT_PROFILE_NAME, resolved.name);
  }

  @Test
  void shouldBuildSystemPromptWithoutBlocklist() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "Base prompt";
    profile.topicBlocklist = null;

    String result = SystemPrompt.build(profile);

    assertEquals("Base prompt", result);
  }

  @Test
  void shouldAppendTopicGuardrails() {
    ChatProfile profile = new ChatProfile();
    profile.systemPrompt = "Base prompt";
    profile.topicBlocklist = "politics, religion";

    String result = SystemPrompt.build(profile);

    assertTrue(result.startsWith("Base prompt"));
    assertTrue(result.contains("politics"));
    assertTrue(result.contains("religion"));
    assertTrue(result.contains("STRICT RULE"));
  }

  @Test
  @TestTransaction
  void shouldListAllProfiles() {
    ChatProfile a = createProfile("a");
    a.persist();

    ChatProfile b = createProfile("b");
    b.persist();

    List<ChatProfile> profiles = service.listAll();

    assertEquals(2, profiles.size());
  }

  @Test
  @TestTransaction
  void shouldFindProfileByName() {
    ChatProfile profile = createProfile("lookup");
    profile.persist();

    ChatProfile found = service.findByName("lookup");

    assertNotNull(found);
    assertEquals("lookup", found.name);
  }

  private ChatProfile createProfile(String name) {
    return ChatProfile.create(name, name + " display", "Test system prompt", 0.5);
  }
}