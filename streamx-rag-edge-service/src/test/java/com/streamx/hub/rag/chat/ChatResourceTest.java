package com.streamx.hub.rag.chat;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ChatResourceTest {

  @InjectMock
  ChatAiService chatService;

  @Test
  void expectPostBeAcceptedAndSseReturned() {
    when(chatService.chat(anyString(), anyString(), anyString()))
        .thenReturn("[\"Sofa\", \"Table\"]");

    given()
        .contentType(ContentType.JSON)
        .body("{\"question\": \"What products are available?\"}")
        .when()
        .post("/api/chat")
        .then()
        .statusCode(200)
        .contentType("application/json;charset=UTF-8");
  }

  @Test
  void expectPostBeAcceptedWithoutQuestion() {
    when(chatService.chat(anyString(), anyString(), anyString()))
        .thenReturn("[\"Sofa\", \"Table\"]");

    given()
        .contentType(ContentType.JSON)
        .when()
        .post("/api/chat")
        .then()
        .statusCode(200)
        .contentType("application/json;charset=UTF-8");
  }

  @Test
  void expectPostBeAcceptedWithSession() {
    when(chatService.chat(anyString(), anyString(), anyString()))
        .thenReturn("[\"Sofa\", \"Table\"]");

    given()
        .contentType(ContentType.JSON)
        .body("{\"question\": \"What products are available?\", \"sessionId\": \"test-123\"}")
        .when()
        .post("/api/chat")
        .then()
        .statusCode(200)
        .contentType("application/json;charset=UTF-8");
  }
}
