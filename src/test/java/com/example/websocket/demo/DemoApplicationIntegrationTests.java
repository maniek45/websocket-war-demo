package com.example.websocket.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationIntegrationTests {

    @LocalServerPort
    private int port = 8080;

    @Test
    public void shouldRespondWithTheSameMessageOnRequest() throws Exception {
        CompletableFuture<WebSocketMessage<?>> completableFuture = new CompletableFuture<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
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

        }, "ws://localhost:{port}/ws/echo", port).get();
        session.sendMessage(new TextMessage("Ala ma kota"));

        assertThat(completableFuture.get().getPayload().toString()).isEqualTo("Ala ma kota");
    }
}
