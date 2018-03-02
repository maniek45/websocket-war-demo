package com.example.websocket.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationIntegrationTests {

    @LocalServerPort
    private int port = 8080;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void shouldRespondWithTheSameMessageOnRequest() throws Exception {
        ResponseEntity<?> response = login();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String sessionIdCookie = parseForSessionIdCookie(response);
        assertThat(sessionIdCookie).isNotNull().isNotEmpty();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders webSocketsHttpHeaders = new WebSocketHttpHeaders();
        webSocketsHttpHeaders.add(HttpHeaders.COOKIE, sessionIdCookie);

        URI webSocketUri = new URI("ws", null, "localhost", port, "/ws/echo", null, null);

        CompletableFuture<WebSocketMessage<?>> completableFuture = new CompletableFuture<>();
        WebSocketSession session = client.doHandshake(new WebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                completableFuture.complete(message);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                completableFuture.completeExceptionally(exception);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                completableFuture.completeExceptionally(new RuntimeException("socket closed"));
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }

        }, webSocketsHttpHeaders, webSocketUri).get();
        session.sendMessage(new TextMessage("Ala ma kota"));

        assertThat(completableFuture.get().getPayload().toString()).isEqualTo("Ala ma kota");
    }

    private ResponseEntity<?> login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{ \"user\": \"user\", \"password\": \"password\" }", headers);
        return testRestTemplate.postForEntity("http://localhost:{port}/login", entity, Void.class, port);
    }

    private String parseForSessionIdCookie(ResponseEntity<?> response) {
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (cookie != null) {
            int index = cookie.indexOf(';');
            if (index >= 0) {
                cookie = cookie.substring(0, index);
            }
        }
        return cookie;
    }
}
