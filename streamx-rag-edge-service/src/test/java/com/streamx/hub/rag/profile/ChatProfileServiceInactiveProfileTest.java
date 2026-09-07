package com.streamx.hub.rag.profile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamx.hub.rag.profile.ChatProfileServiceInactiveProfileTest.InactiveChatProfileTestProfile;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(InactiveChatProfileTestProfile.class)
public class ChatProfileServiceInactiveProfileTest {

  @Inject
  ChatProfileService service;

  @BeforeEach
  @Transactional
  void cleanup() {
    ChatProfile.deleteAll();
  }

  @Test
  @TestTransaction
  void shouldFallbackToDefaultProfileWhenMissing() {
    service.seedDefaultProfile();
    ChatProfile resolved = service.getProfileOrDefault("does-not-exist");

    assertEquals(
        ChatProfileService.DEFAULT_PROFILE_NAME,
        resolved.name
    );
  }

  @Test
  @TestTransaction
  void shouldThrownExceptionWhenDefaultProfileMissing() {
    assertThatThrownBy(() -> service.getProfileOrDefault("does-not-exist"))
        .isInstanceOf(IllegalStateException.class)
            .hasMessage("Default chat profile missing — run seed");
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

    ChatProfile resolved = service.getProfileOrDefault("inactive");

    assertEquals("default", resolved.name);
  }

  public static class InactiveChatProfileTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "streamx.hub.openai-rag-sink.chat-profile.active", "false"
      );
    }
  }
}
