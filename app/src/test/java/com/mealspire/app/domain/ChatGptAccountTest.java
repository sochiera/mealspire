package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;

public class ChatGptAccountTest {

    private static final String REDIRECT = "http://127.0.0.1:43210/callback";
    private static final long NOW_MS = 1_800_000_000_000L;
    private static final String MODELS =
            "{\"models\":[{\"slug\":\"gpt-x\",\"display_name\":\"X\",\"visibility\":\"list\"}]}";

    private final InMemoryChatGptSessionStore store = new InMemoryChatGptSessionStore();
    private final FakeHttpTransport http = new FakeHttpTransport();
    private final TestJwt jwt = new TestJwt();
    private long now = NOW_MS;
    private final ChatGptAccount account = new ChatGptAccount(store, http,
            new ChatGptOAuth(new SecureRandom()), new IdTokenVerifier(), () -> now);

    private String tokenJson(String idToken, String scope) {
        return "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"id_token\":\"" + idToken
                + "\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"" + scope + "\"}";
    }

    private ChatGptOAuth.PendingSignIn scriptHappySignIn(String scope) throws Exception {
        ChatGptOAuth.PendingSignIn pending = account.beginSignIn(REDIRECT);
        String idToken = jwt.sign(TestJwt.claims("oaiapp_new", pending.nonce, NOW_MS / 1000 + 600));
        http.respond(200, tokenJson(idToken, scope))
                .respond(200, jwt.jwks())
                .respond(200, MODELS);
        return pending;
    }

    @Test
    public void signInStoresUsersOwnSession() throws Exception {
        assertFalse(account.isSignedIn());
        ChatGptOAuth.PendingSignIn pending =
                scriptHappySignIn("openid email offline_access chatgpt.tokens.use.direct");

        ChatGptSession session = account.completeSignIn(pending,
                "code=abc&state=" + pending.state + "&client_id=oaiapp_new");

        assertTrue(account.isSignedIn());
        assertEquals("ola@example.com", account.email());
        assertEquals("oaiapp_new", session.clientId);
        assertEquals("gpt-x", session.model);
        assertEquals("rt", session.refreshToken);
        assertEquals(NOW_MS + 3_600_000, session.accessTokenExpiresAtMs);
        assertEquals(ChatGptOAuth.TOKEN_URL, http.calls.get(0).url);
        assertEquals(ChatGptOAuth.JWKS_URL, http.calls.get(1).url);
        assertEquals(ResponsesApi.MODELS_URL, http.calls.get(2).url);
        assertEquals("at", http.calls.get(2).bearer);
    }

    @Test
    public void signInWithoutPlanUsageConsentFails() throws Exception {
        ChatGptOAuth.PendingSignIn pending = scriptHappySignIn("openid email offline_access");
        expectFailure(pending, "code=abc&state=" + pending.state + "&client_id=oaiapp_new", "zgody");
    }

    @Test
    public void signInWithForgedIdTokenFails() throws Exception {
        ChatGptOAuth.PendingSignIn pending = account.beginSignIn(REDIRECT);
        String idToken = jwt.sign(TestJwt.claims("oaiapp_new", "replayed-nonce", NOW_MS / 1000 + 600));
        http.respond(200, tokenJson(idToken, "chatgpt.tokens.use.direct")).respond(200, jwt.jwks());
        expectFailure(pending, "code=abc&state=" + pending.state + "&client_id=oaiapp_new", "zweryfikować");
    }

    @Test
    public void accountWithoutModelsIsExplained() throws Exception {
        ChatGptOAuth.PendingSignIn pending = account.beginSignIn(REDIRECT);
        String idToken = jwt.sign(TestJwt.claims("oaiapp_new", pending.nonce, NOW_MS / 1000 + 600));
        http.respond(200, tokenJson(idToken, "chatgpt.tokens.use.direct"))
                .respond(200, jwt.jwks())
                .respond(200, "{\"models\":[]}");
        expectFailure(pending, "code=abc&state=" + pending.state + "&client_id=oaiapp_new", "Plus lub Pro");
    }

    @Test
    public void reauthenticationReusesIssuedClientId() {
        store.save(session(NOW_MS + 3_600_000));
        ChatGptOAuth.PendingSignIn pending = account.beginSignIn(REDIRECT);
        assertEquals("oaiapp_x", pending.clientId);
    }

    @Test
    public void freshTokenIsUsedWithoutRefresh() throws IOException {
        store.save(session(NOW_MS + 3_600_000));
        assertEquals("at", account.activeSession(false).accessToken);
        assertTrue(http.calls.isEmpty());
    }

    @Test
    public void expiringTokenIsRefreshedAndRotated() throws IOException {
        store.save(session(NOW_MS + 30_000));
        http.respond(200, "{\"access_token\":\"at2\",\"refresh_token\":\"rt2\",\"expires_in\":3600}");

        ChatGptSession refreshed = account.activeSession(false);

        assertEquals("at2", refreshed.accessToken);
        assertEquals("rt2", store.load().refreshToken);
        assertEquals("refresh_token", http.calls.get(0).form.get("grant_type"));
        assertEquals("rt", http.calls.get(0).form.get("refresh_token"));
        assertEquals("oaiapp_x", http.calls.get(0).form.get("client_id"));
    }

    @Test
    public void rejectedRefreshTokenSignsOut() {
        store.save(session(NOW_MS - 1));
        http.respond(400, "{\"error\":\"invalid_grant\"}");
        try {
            account.activeSession(false);
            fail();
        } catch (IOException e) {
            assertTrue(e instanceof ChatGptOAuth.SessionExpiredException);
        }
        assertFalse(account.isSignedIn());
    }

    @Test
    public void transientRefreshFailureKeepsSession() {
        store.save(session(NOW_MS - 1));
        http.respond(500, "{\"error\":\"server_error\"}");
        try {
            account.activeSession(false);
            fail();
        } catch (IOException expected) {
            // ok
        }
        assertTrue(account.isSignedIn());
    }

    @Test
    public void signedOutHasNoSession() {
        try {
            account.activeSession(false);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Zaloguj"));
        }
    }

    @Test
    public void signOutClearsLocallyAndRevokes() {
        store.save(session(NOW_MS + 3_600_000));
        http.respond(200, "");
        account.signOut();
        assertNull(store.load());
        assertEquals(ChatGptOAuth.REVOKE_URL, http.calls.get(0).url);
        assertEquals("rt", http.calls.get(0).form.get("token"));
    }

    @Test
    public void signOutSucceedsLocallyEvenWhenOffline() {
        store.save(session(NOW_MS + 3_600_000));
        account.signOut(); // no scripted response -> IOException inside
        assertFalse(account.isSignedIn());
    }

    private static ChatGptSession session(long expiresAt) {
        return new ChatGptSession("oaiapp_x", "at", "rt", "it", expiresAt, "sub",
                "ola@example.com", "gpt-x");
    }

    private void expectFailure(ChatGptOAuth.PendingSignIn pending, String query, String fragment) {
        try {
            account.completeSignIn(pending, query);
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragment));
            assertFalse("nothing may be saved on failure", account.isSignedIn());
            return;
        }
        throw new AssertionError("expected sign-in failure");
    }
}
