package com.threadtrades.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
import com.threadtrades.security.SessionCookieService;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    /**
     * The session JWT and CSRF token, both delivered as Set-Cookie headers on register/login
     * (see SecurityConfig's csrf.spa() + CsrfCookieFilter). TestRestTemplate has no cookie jar
     * of its own (unlike a browser), so every subsequent request in this test manually replays
     * both cookies via authHeaders(), mirroring what the browser does automatically for real.
     */
    private record Session(String sessionCookieValue, String csrfToken) {
    }

    /**
     * A single response can carry more than one Set-Cookie for the same name (e.g. register
     * gets both a clearing XSRF-TOKEN cookie from CsrfFilter's exemption handling for that path
     * and a fresh one from CsrfCookieFilter forcing eager token generation) -- a real browser's
     * cookie jar just applies them in order and keeps whatever's last, so this does the same
     * (skipping any that carry no value) rather than naively taking the first match.
     */
    private static String extractCookieValue(ResponseEntity<?> response, String cookieName) {
        List<String> matches = response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(setCookie -> setCookie.startsWith(cookieName + "="))
                .map(setCookie -> setCookie.substring(cookieName.length() + 1, setCookie.indexOf(';')))
                .filter(value -> !value.isEmpty())
                .toList();
        if (matches.isEmpty()) {
            throw new AssertionError("No " + cookieName + " cookie in response");
        }
        return matches.get(matches.size() - 1);
    }

    /**
     * Cookie + X-XSRF-TOKEN headers for a mutating (non-GET) authenticated request. Both the
     * session cookie AND the XSRF-TOKEN cookie must be replayed -- CookieCsrfTokenRepository
     * validates by comparing the X-XSRF-TOKEN header against the XSRF-TOKEN cookie on the
     * *incoming* request, not just checking the header is present.
     */
    private static HttpHeaders authHeaders(Session session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(
                HttpHeaders.COOKIE,
                SessionCookieService.COOKIE_NAME + "=" + session.sessionCookieValue() + "; XSRF-TOKEN="
                        + session.csrfToken());
        headers.add("X-XSRF-TOKEN", session.csrfToken());
        return headers;
    }

    private Session registerAndGetToken(String email, String username) {
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
        return new Session(
                extractCookieValue(response, SessionCookieService.COOKIE_NAME), extractCookieValue(response, "XSRF-TOKEN"));
    }

    private Long uploadItem(Session session, String name) {
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

        HttpHeaders headers = authHeaders(session);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/clothing-items", new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Number id = JsonPath.read(response.getBody(), "$.id");
        return id.longValue();
    }

    private ResponseEntity<String> swipe(Session session, Long offeredItemId, Long targetItemId, String decision) {
        HttpHeaders headers = authHeaders(session);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = """
                {"offeredItemId":%d,"clothingItemId":%d,"decision":"%s"}
                """
                .formatted(offeredItemId, targetItemId, decision);
        return restTemplate.postForEntity("/api/swipes", new HttpEntity<>(json, headers), String.class);
    }

    /** Mutually likes offeredItemId/targetItemId between the two sessions and returns the resulting matchId. */
    private Long createMatch(Session sessionA, Long itemA, Session sessionB, Long itemB) {
        assertEquals(HttpStatus.CREATED, swipe(sessionA, itemA, itemB, "LIKE").getStatusCode());
        ResponseEntity<String> response = swipe(sessionB, itemB, itemA, "LIKE");
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Number matchId = JsonPath.read(response.getBody(), "$.matchId");
        return matchId.longValue();
    }

    private void acceptSwap(Session session, Long matchId) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/matches/" + matchId + "/swap/accept",
                org.springframework.http.HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(session)),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void sendMessage(Session session, Long matchId, String content) {
        HttpHeaders headers = authHeaders(session);
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private StompSession connect(WebSocketStompClient client, Session session, RecordingStompSessionHandler handler)
            throws Exception {
        // The session cookie travels on the WS handshake itself (a normal HTTP GET the browser
        // attaches cookies to automatically) -- see SessionCookieHandshakeInterceptor -- not as
        // a STOMP CONNECT header.
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add(HttpHeaders.COOKIE, SessionCookieService.COOKIE_NAME + "=" + session.sessionCookieValue());
        return client.connectAsync("ws://localhost:" + port + "/ws", handshakeHeaders, new StompHeaders(), handler)
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
        Session sessionA = registerAndGetToken("wanda@example.com", "wanda");
        Session sessionB = registerAndGetToken("xavier@example.com", "xavier");
        Long itemA = uploadItem(sessionA, "Wanda's Jacket");
        Long itemB = uploadItem(sessionB, "Xavier's Jacket");
        Long matchId = createMatch(sessionA, itemA, sessionB, itemB);

        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();
        StompSession session = connect(client, sessionB, handler);
        session.subscribe(MatchTopics.destination(matchId), handler);
        // Give the SUBSCRIBE frame time to reach the broker before the message is sent.
        Thread.sleep(300);

        sendMessage(sessionA, matchId, "Hi Xavier!");

        Map<String, Object> received = handler.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals("Hi Xavier!", received.get("content"));
        assertEquals(matchId, ((Number) received.get("matchId")).longValue());

        session.disconnect();
        client.stop();
    }

    @Test
    void matchParticipantReceivesLiveSwapUpdateOverWebSocket() throws Exception {
        Session sessionA = registerAndGetToken("uma@example.com", "uma");
        Session sessionB = registerAndGetToken("victor@example.com", "victor");
        Long itemA = uploadItem(sessionA, "Uma's Jacket");
        Long itemB = uploadItem(sessionB, "Victor's Jacket");
        Long matchId = createMatch(sessionA, itemA, sessionB, itemB);

        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();
        StompSession session = connect(client, sessionB, handler);
        session.subscribe(SwapTopics.destination(matchId), handler);
        // Give the SUBSCRIBE frame time to reach the broker before the accept is recorded.
        Thread.sleep(300);

        acceptSwap(sessionA, matchId);

        Map<String, Object> received = handler.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals("PENDING", received.get("status"));
        assertEquals(matchId, ((Number) received.get("matchId")).longValue());

        session.disconnect();
        client.stop();
    }

    @Test
    void connectWithoutASessionCookieIsRejected() throws Exception {
        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();

        CompletableFuture<StompSession> connectFuture = client.connectAsync(
                "ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), new StompHeaders(), handler);

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
        Session sessionA = registerAndGetToken("yara@example.com", "yara");
        Session sessionB = registerAndGetToken("zack@example.com", "zack");
        Session outsiderSession = registerAndGetToken("owen@example.com", "owen");
        Long itemA = uploadItem(sessionA, "Yara's Jacket");
        Long itemB = uploadItem(sessionB, "Zack's Jacket");
        Long matchId = createMatch(sessionA, itemA, sessionB, itemB);

        WebSocketStompClient client = newStompClient();
        RecordingStompSessionHandler handler = new RecordingStompSessionHandler();
        StompSession session = connect(client, outsiderSession, handler);
        session.subscribe(MatchTopics.destination(matchId), handler);

        sendMessage(sessionA, matchId, "Outsider should not see this");

        Map<String, Object> received = handler.messages.poll(2, TimeUnit.SECONDS);
        assertNull(received, "outsider should not receive messages from a match they aren't part of");
        // The rejected SUBSCRIBE tears the session down entirely, so there's nothing left
        // to disconnect -- that's itself part of what this test is verifying.
        assertFalse(session.isConnected());

        client.stop();
    }
}
