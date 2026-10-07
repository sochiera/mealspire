package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;

public class ChatGptOAuthTest {

    private static final String HOST = "urn:uuid:00000000-0000-4000-8000-000000000001";
    private static final String REDIRECT = "http://127.0.0.1:43210/callback";

    private final ChatGptOAuth oauth = new ChatGptOAuth(new SecureRandom());

    @Test
    public void pkceChallengeMatchesRfc7636Example() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                ChatGptOAuth.codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test
    public void firstSignInRegistersDynamicClientWithPlanUsageScope() {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "", "");

        assertTrue(pending.authorizeUrl.startsWith(ChatGptOAuth.AUTHORIZE_URL + "?"));
        Map<String, String> q = query(pending.authorizeUrl);
        assertEquals("dynamic_agent_client", q.get("client_id"));
        assertEquals("Mealspire", q.get("agent_name_hint"));
        assertEquals(HOST, q.get("ext_agent_host_id"));
        assertEquals("code", q.get("response_type"));
        assertEquals(REDIRECT, q.get("redirect_uri"));
        assertEquals("openid profile email offline_access resource.invoke chatgpt.tokens.use.direct",
                q.get("scope"));
        assertEquals("https://api.openai.com/v1", q.get("resource"));
        assertEquals("S256", q.get("code_challenge_method"));
        assertEquals(ChatGptOAuth.codeChallenge(pending.codeVerifier), q.get("code_challenge"));
        assertEquals(pending.state, q.get("state"));
        assertEquals(pending.nonce, q.get("nonce"));
        assertFalse("spaces must be %20, not +", pending.authorizeUrl.contains("+"));
    }

    @Test
    public void everySignInUsesFreshStateNonceAndVerifier() {
        ChatGptOAuth.PendingSignIn a = oauth.begin(HOST, REDIRECT, "", "");
        ChatGptOAuth.PendingSignIn b = oauth.begin(HOST, REDIRECT, "", "");
        assertNotEquals(a.state, b.state);
        assertNotEquals(a.nonce, b.nonce);
        assertNotEquals(a.codeVerifier, b.codeVerifier);
        assertTrue(a.codeVerifier.length() >= 43);
    }

    @Test
    public void reauthenticationReusesIssuedClientIdWithLoginHint() {
        ChatGptOAuth.PendingSignIn pending =
                oauth.begin(HOST, REDIRECT, "oaiapp_abc", "ola@example.com");
        Map<String, String> q = query(pending.authorizeUrl);
        assertEquals("oaiapp_abc", q.get("client_id"));
        assertEquals("ola@example.com", q.get("login_hint"));
        assertFalse(q.containsKey("agent_name_hint"));
    }

    @Test
    public void callbackReturnsIssuedClientIdAndCode() throws IOException {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "", "");
        ChatGptOAuth.Callback callback = oauth.parseCallback(pending,
                "code=abc&scope=openid%20chatgpt.tokens.use.direct&state=" + pending.state
                        + "&client_id=oaiapp_new");
        assertEquals("abc", callback.code);
        assertEquals("oaiapp_new", callback.clientId);
        assertEquals("openid chatgpt.tokens.use.direct", callback.scope);
    }

    @Test
    public void callbackWithoutClientIdKeepsKnownClient() throws IOException {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "oaiapp_abc", "");
        assertEquals("oaiapp_abc",
                oauth.parseCallback(pending, "code=c&state=" + pending.state).clientId);
    }

    @Test
    public void callbackWithWrongStateIsRejected() {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "", "");
        expectIOException(() -> oauth.parseCallback(pending, "code=c&state=forged&client_id=oaiapp_x"));
    }

    @Test
    public void deniedConsentIsReportedAsCancelled() {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "", "");
        IOException e = expectIOException(() ->
                oauth.parseCallback(pending, "error=access_denied&state=" + pending.state));
        assertTrue(e.getMessage().contains("anulowane"));
    }

    @Test
    public void newRegistrationWithoutIssuedClientIdIsRejected() {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "", "");
        expectIOException(() -> oauth.parseCallback(pending, "code=c&state=" + pending.state));
    }

    @Test
    public void tokenExchangeUsesPkceVerifierAndExactRedirectWithoutSecret() throws IOException {
        ChatGptOAuth.PendingSignIn pending = oauth.begin(HOST, REDIRECT, "", "");
        ChatGptOAuth.Callback callback = oauth.parseCallback(pending,
                "code=abc&state=" + pending.state + "&client_id=oaiapp_new");
        Map<String, String> form = oauth.tokenExchangeForm(pending, callback);
        assertEquals("authorization_code", form.get("grant_type"));
        assertEquals("oaiapp_new", form.get("client_id"));
        assertEquals("abc", form.get("code"));
        assertEquals(pending.codeVerifier, form.get("code_verifier"));
        assertEquals(REDIRECT, form.get("redirect_uri"));
        assertEquals("https://api.openai.com/v1", form.get("resource"));
        assertFalse(form.containsKey("client_secret"));
    }

    @Test
    public void refreshFormMatchesSpec() {
        Map<String, String> form = ChatGptOAuth.refreshForm("oaiapp_x", "rt");
        assertEquals("refresh_token", form.get("grant_type"));
        assertEquals("oaiapp_x", form.get("client_id"));
        assertEquals("rt", form.get("refresh_token"));
        assertEquals("https://api.openai.com/v1", form.get("resource"));
    }

    @Test
    public void parsesTokenResponse() throws IOException {
        ChatGptOAuth.TokenResponse t = ChatGptOAuth.parseTokenResponse(new HttpTransport.Response(200,
                "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"id_token\":\"it\","
                        + "\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"openid\"}"));
        assertEquals("at", t.accessToken);
        assertEquals("rt", t.refreshToken);
        assertEquals("it", t.idToken);
        assertEquals(3600, t.expiresInSeconds);
    }

    @Test
    public void invalidGrantMeansSessionExpired() {
        try {
            ChatGptOAuth.parseTokenResponse(new HttpTransport.Response(400,
                    "{\"error\":\"invalid_grant\"}"));
            fail();
        } catch (ChatGptOAuth.SessionExpiredException expected) {
            // ok
        } catch (IOException e) {
            fail("expected SessionExpiredException, got " + e);
        }
    }

    @Test
    public void otherTokenErrorsCarryDescription() {
        IOException e = expectIOException(() -> ChatGptOAuth.parseTokenResponse(
                new HttpTransport.Response(400,
                        "{\"error\":\"invalid_request\",\"error_description\":\"bad redirect\"}")));
        assertTrue(e.getMessage().contains("bad redirect"));
        assertFalse(e instanceof ChatGptOAuth.SessionExpiredException);
    }

    @Test
    public void scopeCheckMatchesWholeWordsOnly() {
        assertTrue(ChatGptOAuth.hasScope("openid chatgpt.tokens.use.direct", "chatgpt.tokens.use.direct"));
        assertFalse(ChatGptOAuth.hasScope("openid chatgpt.tokens.use.directX", "chatgpt.tokens.use.direct"));
        assertFalse(ChatGptOAuth.hasScope("", "chatgpt.tokens.use.direct"));
    }

    private static Map<String, String> query(String url) {
        return ChatGptOAuth.parseQuery(url.substring(url.indexOf('?') + 1));
    }

    interface Action {
        void run() throws IOException;
    }

    static IOException expectIOException(Action action) {
        try {
            action.run();
        } catch (IOException e) {
            return e;
        }
        throw new AssertionError("expected IOException");
    }
}
