package com.threadtrades.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class WebSocketMessagingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String STORAGE_LOCAL_PATH =
            System.getProperty("java.io.tmpdir") + "/threadtrades-test-uploads-" + UUID.randomUUID();

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-path", () -> STORAGE_LOCAL_PATH);
        // TestcontainersConfiguration's jwtSecretRegistrar bean (a DynamicPropertyRegistrar)
        // isn't applied in time for RANDOM_PORT tests -- the embedded server's security
        // filter chain needs app.jwt.secret before that bean gets created. Registering it
        // here via the annotation-based mechanism runs before context refresh regardless.
        registry.add("app.jwt.secret", () -> "test-only-jwt-secret-not-for-production-use");
    }

    private String registerAndGetToken(String email, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String registerJson =
                """
                {"email":"%s","password":"hunter2pass","username":"%s","name":"Test User"}
                """
                        .formatted(email, username);
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/auth/register", new HttpEntity<>(registerJson, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return JsonPath.read(response.getBody(), "$.token");
    }

    private Long uploadItem(String token, String name) {
        HttpHeaders imagePartHeaders = new HttpHeaders();
        imagePartHeaders.setContentType(MediaType.IMAGE_JPEG);
        HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(
                new ByteArrayResource("fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return "photo.jpg";
                    }
                },
                imagePartHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);
        body.add("name", name);
        body.add("itemType", "Jacket");
        body.add("clothingSize", "M");
        body.add("condition", "GOOD");
        body.add("gender", "UNISEX");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/clothing-items", new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Number id = JsonPath.read(response.getBody(), "$.id");
        return id.longValue();
    }

    private ResponseEntity<String> swipe(String token, Long offeredItemId, Long targetItemId, String decision) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String json = """
                {"offeredItemId":%d,"clothingItemId":%d,"decision":"%s"}
                """
                .formatted(offeredItemId, targetItemId, decision);
        return restTemplate.postForEntity("/api/swipes", new HttpEntity<>(json, headers), String.class);
    }

    /** Mutually likes offeredItemId/targetItemId between the two tokens and returns the resulting matchId. */
    private Long createMatch(String tokenA, Long itemA, String tokenB, Long itemB) {
        assertEquals(HttpStatus.CREATED, swipe(tokenA, itemA, itemB, "LIKE").getStatusCode());
        ResponseEntity<String> response = swipe(tokenB, itemB, itemA, "LIKE");
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Number matchId = JsonPath.read(response.getBody(), "$.matchId");
        return matchId.longValue();
    }

    private void sendMessage(String token, Long matchId, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String json = """
                {"content":"%s"}
                """
                .formatted(content);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/matches/" + matchId + "/messages", new HttpEntity<>(json, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private WebSocketStompClient newStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client;
    }

    private StompSession connect(WebSocketStompClient client, String token, RecordingStompSessionHandler handler)
            throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders, handler)
                .get(5, TimeUnit.SECONDS);
    }

    /** Records inbound MESSAGE frames (per subscription) and CONNECT/SUBSCRIBE-time errors. */
    private static class RecordingStompSessionHandler extends StompSessionHandlerAdapter {
        final LinkedBlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();
        final CompletableFuture<Throwable> error = new CompletableFuture<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleFrame(StompHeaders headers, Object payload) {
            messages.add((Map<String, Object>) payload);
        }

        @Override
        public void handleException(
                StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            error.complete(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            error.complete(exception);
        }
    }

    @Test
    void matchParticipantReceivesLiveMessageOverWebSocket() throws Exception {
        String tokenA = registerAndGetToken("wanda@example.com", "wanda");
        String tokenB = registerAndGetToken("xavier@example.com", "xavier");
        Long itemA = uploadItem(tokenA, "Wanda's Jacket");
        Long itemB = uploadItem(tokenB, "Xavier's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();
        StompSession session = connect(client, tokenB, handler);
        session.subscribe(MatchTopics.destination(matchId), handler);
        // Give the SUBSCRIBE frame time to reach the broker before the message is sent.
        Thread.sleep(300);

        sendMessage(tokenA, matchId, "Hi Xavier!");

        Map<String, Object> received = handler.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals("Hi Xavier!", received.get("content"));
        assertEquals(matchId, ((Number) received.get("matchId")).longValue());

        session.disconnect();
        client.stop();
    }

    @Test
    void connectWithInvalidTokenIsRejected() throws Exception {
        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer not-a-real-token");

        CompletableFuture<StompSession> connectFuture =
                client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders, handler);

        boolean connectFailed = false;
        try {
            connectFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            connectFailed = true;
        }
        if (!connectFailed) {
            // Some Spring versions let the socket open but reject the STOMP CONNECT frame
            // itself, surfacing as a session-level error rather than a failed future.
            assertNotNull(handler.error.get(5, TimeUnit.SECONDS));
        }
        client.stop();
    }

    @Test
    void userNotInTheMatchCannotSubscribeToItsThread() throws Exception {
        String tokenA = registerAndGetToken("yara@example.com", "yara");
        String tokenB = registerAndGetToken("zack@example.com", "zack");
        String outsiderToken = registerAndGetToken("owen@example.com", "owen");
        Long itemA = uploadItem(tokenA, "Yara's Jacket");
        Long itemB = uploadItem(tokenB, "Zack's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();
        StompSession session = connect(client, outsiderToken, handler);
        session.subscribe(MatchTopics.destination(matchId), handler);

        sendMessage(tokenA, matchId, "Outsider should not see this");

        Map<String, Object> received = handler.messages.poll(2, TimeUnit.SECONDS);
        assertNull(received, "outsider should not receive messages from a match they aren't part of");
        // The rejected SUBSCRIBE tears the session down entirely, so there's nothing left
        // to disconnect -- that's itself part of what this test is verifying.
        assertFalse(session.isConnected());

        client.stop();
    }
}
