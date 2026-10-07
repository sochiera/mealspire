package com.mealspire.app.domain;

import java.io.IOException;

/**
 * The app user's own ChatGPT account: finishes the browser sign-in, keeps the
 * access token fresh and signs out. AI requests then run on the user's ChatGPT
 * plan — no API key is built into or pasted into the app.
 */
public final class ChatGptAccount {

    /** Wall clock, injectable for tests (java.util.function needs API 24). */
    public interface Clock {
        long nowMillis();
    }

    /** Refresh this long before the access token actually expires. */
    static final long REFRESH_MARGIN_MS = 60_000;

    private final ChatGptSessionStore store;
    private final HttpTransport transport;
    private final ChatGptOAuth oauth;
    private final IdTokenVerifier verifier;
    private final Clock clock;

    public ChatGptAccount(ChatGptSessionStore store, HttpTransport transport,
                          ChatGptOAuth oauth, IdTokenVerifier verifier, Clock clock) {
        this.store = store;
        this.transport = transport;
        this.oauth = oauth;
        this.verifier = verifier;
        this.clock = clock;
    }

    public boolean isSignedIn() {
        return store.load() != null;
    }

    /** E-mail of the signed-in account, or empty. */
    public String email() {
        ChatGptSession session = store.load();
        return session == null ? "" : session.email;
    }

    /** Starts a sign-in whose browser redirect lands on {@code redirectUri}. */
    public ChatGptOAuth.PendingSignIn beginSignIn(String redirectUri) {
        ChatGptSession previous = store.load();
        return oauth.begin(store.hostId(), redirectUri,
                previous == null ? "" : previous.clientId,
                previous == null ? "" : previous.email);
    }

    /**
     * Completes the sign-in from the redirect's query string: checks state,
     * exchanges the code (PKCE), verifies the ID token, requires plan-usage
     * consent and picks a model the account may use. Saves and returns the session.
     */
    public ChatGptSession completeSignIn(ChatGptOAuth.PendingSignIn pending, String query)
            throws IOException {
        ChatGptOAuth.Callback callback = oauth.parseCallback(pending, query);
        ChatGptOAuth.TokenResponse tokens = ChatGptOAuth.parseTokenResponse(
                transport.postForm(ChatGptOAuth.TOKEN_URL,
                        oauth.tokenExchangeForm(pending, callback)));
        if (tokens.refreshToken.isEmpty()) {
            throw new IOException("Logowanie do ChatGPT nie zwróciło tokenu odświeżania.");
        }
        String granted = tokens.scope.isEmpty() ? callback.scope : tokens.scope;
        if (!ChatGptOAuth.hasScope(granted, ChatGptOAuth.PLAN_USAGE_SCOPE)) {
            throw new IOException("Nie udzielono zgody na korzystanie z planu ChatGPT "
                    + "w Mealspire — bez niej AI nie działa.");
        }

        HttpTransport.Response jwks = transport.get(ChatGptOAuth.JWKS_URL, null);
        if (!jwks.isSuccess()) {
            throw new IOException("Nie udało się pobrać kluczy logowania ChatGPT (HTTP "
                    + jwks.status + ").");
        }
        long now = clock.nowMillis();
        IdTokenVerifier.Claims claims = verifier.verify(tokens.idToken, jwks.body,
                ChatGptOAuth.ISSUER, callback.clientId, pending.nonce, now / 1000);

        HttpTransport.Response models = transport.get(ResponsesApi.MODELS_URL, tokens.accessToken);
        if (!models.isSuccess()) {
            throw ResponsesApi.parseErrorBody(models.status, models.body);
        }
        String model = ResponsesApi.pickModel(models.body);
        if (model.isEmpty()) {
            throw new IOException("Twoje konto ChatGPT nie udostępnia modeli innym aplikacjom "
                    + "(potrzebny plan Plus lub Pro).");
        }

        ChatGptSession session = new ChatGptSession(callback.clientId, tokens.accessToken,
                tokens.refreshToken, tokens.idToken,
                now + tokens.expiresInSeconds * 1000, claims.subject, claims.email, model);
        store.save(session);
        return session;
    }

    /**
     * Returns a usable session, refreshing the access token when it is (about
     * to be) expired or when {@code forceRefresh} is set (after a 401). A
     * rejected refresh token signs the user out.
     */
    public synchronized ChatGptSession activeSession(boolean forceRefresh) throws IOException {
        ChatGptSession session = store.load();
        if (session == null) {
            throw new IOException("Zaloguj się kontem ChatGPT, aby korzystać z AI.");
        }
        if (!forceRefresh && clock.nowMillis() < session.accessTokenExpiresAtMs - REFRESH_MARGIN_MS) {
            return session;
        }
        ChatGptOAuth.TokenResponse tokens;
        try {
            tokens = ChatGptOAuth.parseTokenResponse(transport.postForm(ChatGptOAuth.TOKEN_URL,
                    ChatGptOAuth.refreshForm(session.clientId, session.refreshToken)));
        } catch (ChatGptOAuth.SessionExpiredException e) {
            store.clear();
            throw e;
        }
        // Refresh tokens rotate: keep the new one, falling back only if none came back.
        ChatGptSession refreshed = session.withTokens(tokens.accessToken,
                tokens.refreshToken.isEmpty() ? session.refreshToken : tokens.refreshToken,
                tokens.idToken, clock.nowMillis() + tokens.expiresInSeconds * 1000);
        store.save(refreshed);
        return refreshed;
    }

    /** Signs out locally and (best effort) revokes the refresh token at OpenAI. */
    public void signOut() {
        ChatGptSession session = store.load();
        store.clear();
        if (session == null) {
            return;
        }
        try {
            transport.postForm(ChatGptOAuth.REVOKE_URL,
                    ChatGptOAuth.revokeForm(session.clientId, session.refreshToken));
        } catch (IOException ignored) {
            // Local sign-out already happened; the token also expires on its own.
        }
    }
}
