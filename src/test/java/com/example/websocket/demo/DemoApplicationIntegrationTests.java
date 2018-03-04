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
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
        ResponseEntity<?> response = loginRequest();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String sessionIdCookie = parseForSessionIdCookie(response);
        assertThat(sessionIdCookie).isNotNull().isNotEmpty();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders webSocketsHttpHeaders = new WebSocketHttpHeaders();
        webSocketsHttpHeaders.add(HttpHeaders.COOKIE, sessionIdCookie);

        URI webSocketUri = new URI("ws", null, "localhost", port, "/ws/echo", null, null);

        CompletableFuture<WebSocketMessage<?>> completableFuture = new CompletableFuture<>();
        WebSocketSession session = client.doHandshake(new AbstractWebSocketHandler() {

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

        }, webSocketsHttpHeaders, webSocketUri).get();
        session.sendMessage(new TextMessage("Ala ma kota"));

        assertThat(completableFuture.get(2000, TimeUnit.MILLISECONDS).getPayload().toString()).isEqualTo("Ala ma kota");
    }

    @Test
    public void shouldReceiveEchoMessageOnSubscribe() throws Exception {
        ResponseEntity<?> response = loginRequest();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String sessionIdCookie = parseForSessionIdCookie(response);
        assertThat(sessionIdCookie).isNotNull().isNotEmpty();

        WebSocketHttpHeaders webSocketsHttpHeaders = new WebSocketHttpHeaders();
        webSocketsHttpHeaders.add(HttpHeaders.COOKIE, sessionIdCookie);

        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        CompletableFuture<Object> completableFuture = new CompletableFuture<>();

        URI webSocketUri = new URI("ws", null, "localhost", port, "/stomp", null, null);
        StompSession stompSession = stompClient.connect(webSocketUri, webSocketsHttpHeaders, null,
                new MyStompSessionHandlerAdapter(completableFuture)).get();

        stompSession.subscribe("/topic/echo", new MyStompSessionHandlerAdapter(completableFuture));
        stompSession.send("/app/echo", "Ala ma kota");

        assertThat(completableFuture.get(2000, TimeUnit.MILLISECONDS).toString()).isEqualTo("Ala ma kota");
    }

    @Test
    public void shouldReceiveEchoMessageOnSubscribeTwoClients() throws Exception {
        ResponseEntity<?> response = loginRequest();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String sessionIdCookie = parseForSessionIdCookie(response);
        assertThat(sessionIdCookie).isNotNull().isNotEmpty();

        WebSocketHttpHeaders webSocketsHttpHeaders = new WebSocketHttpHeaders();
        webSocketsHttpHeaders.add(HttpHeaders.COOKIE, sessionIdCookie);

        WebSocketClient webSocketClient = new StandardWebSocketClient();

        WebSocketStompClient stompClient1 = new WebSocketStompClient(webSocketClient);
        stompClient1.setMessageConverter(new StringMessageConverter());

        WebSocketStompClient stompClient2 = new WebSocketStompClient(webSocketClient);
        stompClient2.setMessageConverter(new StringMessageConverter());

        URI webSocketUri = new URI("ws", null, "localhost", port, "/stomp", null, null);

        CompletableFuture<Object> completableFuture1 = new CompletableFuture<>();
        StompSession stompSession1 = stompClient1.connect(webSocketUri, webSocketsHttpHeaders, null,
                new MyStompSessionHandlerAdapter(completableFuture1)).get();
        stompSession1.subscribe("/user/topic/echo", new MyStompSessionHandlerAdapter(completableFuture1));

        CompletableFuture<Object> completableFuture2 = new CompletableFuture<>();
        StompSession stompSession2 = stompClient2.connect(webSocketUri, webSocketsHttpHeaders, null,
                new MyStompSessionHandlerAdapter(completableFuture2)).get();
        stompSession2.subscribe("/user/topic/echo", new MyStompSessionHandlerAdapter(completableFuture2));

        stompSession1.send("/app/echo", "Ala ma kota");

        assertThat(completableFuture1.get(2000, TimeUnit.MILLISECONDS).toString()).isEqualTo("Ala ma kota");
        assertThat(completableFuture2.get(2000, TimeUnit.MILLISECONDS).toString()).isEqualTo("Ala ma kota");
    }

    private ResponseEntity<?> loginRequest() {
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

    private static class MyStompSessionHandlerAdapter extends StompSessionHandlerAdapter {

        private final CompletableFuture<Object> completableFuture;

        public MyStompSessionHandlerAdapter(CompletableFuture<Object> completableFuture) {
            this.completableFuture = completableFuture;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            completableFuture.complete(payload);
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            completableFuture.completeExceptionally(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            completableFuture.completeExceptionally(exception);
        }
    }
}
