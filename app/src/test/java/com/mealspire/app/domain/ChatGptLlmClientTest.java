package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;

public class ChatGptLlmClientTest {

    private static final long NOW_MS = 1_800_000_000_000L;
    private static final String OK_STREAM =
            "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Zupa\"}\n"
                    + "data: {\"type\":\"response.completed\"}\n";

    private final InMemoryChatGptSessionStore store = new InMemoryChatGptSessionStore();
    private final FakeHttpTransport http = new FakeHttpTransport();
    private final ChatGptAccount account = new ChatGptAccount(store, http,
            new ChatGptOAuth(new SecureRandom()), new IdTokenVerifier(), () -> NOW_MS);
    private final ChatGptLlmClient client = new ChatGptLlmClient(account, http);

    @Test
    public void sendsPromptsOnUsersPlanWithBearerToken() throws Exception {
        store.save(new ChatGptSession("oaiapp_x", "at", "rt", "it", NOW_MS + 3_600_000,
                "sub", "e", "gpt-x"));
        http.respond(200, OK_STREAM);

        assertEquals("Zupa", client.complete("system", "user"));

        FakeHttpTransport.Call call = http.calls.get(0);
        assertEquals(ResponsesApi.RESPONSES_URL, call.url);
        assertEquals("at", call.bearer);
        assertEquals("gpt-x", new JSONObject(call.json).getString("model"));
    }

    @Test
    public void unauthorizedTriggersOneRefreshAndRetry() throws Exception {
        store.save(new ChatGptSession("oaiapp_x", "at", "rt", "it", NOW_MS + 3_600_000,
                "sub", "e", "gpt-x"));
        http.respond(401, "{\"error\":{\"message\":\"expired\"}}")
                .respond(200, "{\"access_token\":\"at2\",\"refresh_token\":\"rt2\",\"expires_in\":3600}")
                .respond(200, OK_STREAM);

        assertEquals("Zupa", client.complete("s", "u"));
        assertEquals("at2", http.calls.get(2).bearer);
    }

    @Test
    public void signedOutUserGetsSignInHint() {
        try {
            client.complete("s", "u");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("ChatGPT"));
            assertTrue(http.calls.isEmpty());
            return;
        }
        throw new AssertionError("expected IOException");
    }
}
