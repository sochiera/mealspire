package com.mealspire.app.domain;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure protocol pieces of Sign in with ChatGPT for open-source apps:
 * Authorization Code + PKCE with dynamic client registration and a loopback
 * redirect. No client secret exists — the user's own consent in the browser
 * issues a client id bound to their account.
 *
 * @see <a href="https://developers.openai.com/siwc/token-sharing-open-source/sign-in">Sign in</a>
 */
public final class ChatGptOAuth {

    public static final String ISSUER = "https://auth.openai.com";
    public static final String AUTHORIZE_URL = ISSUER + "/api/accounts/authorize";
    public static final String TOKEN_URL = ISSUER + "/api/accounts/oauth/token";
    public static final String REVOKE_URL = ISSUER + "/api/accounts/oauth/revoke";
    public static final String JWKS_URL = ISSUER + "/.well-known/jwks.json";
    public static final String RESOURCE = "https://api.openai.com/v1";
    public static final String DYNAMIC_CLIENT_ID = "dynamic_agent_client";
    public static final String PLAN_USAGE_SCOPE = "chatgpt.tokens.use.direct";
    public static final String SCOPE =
            "openid profile email offline_access resource.invoke " + PLAN_USAGE_SCOPE;
    public static final String APP_NAME = "Mealspire";

    private final SecureRandom random;

    public ChatGptOAuth(SecureRandom random) {
        this.random = random;
    }

    /** One in-flight browser sign-in: everything needed to finish it safely. */
    public static final class PendingSignIn {
        public final String clientId;
        public final String redirectUri;
        public final String state;
        public final String nonce;
        public final String codeVerifier;
        public final String authorizeUrl;

        PendingSignIn(String clientId, String redirectUri, String state, String nonce,
                      String codeVerifier, String authorizeUrl) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.state = state;
            this.nonce = nonce;
            this.codeVerifier = codeVerifier;
            this.authorizeUrl = authorizeUrl;
        }
    }

    /**
     * Prepares a sign-in. {@code knownClientId} is the id issued by an earlier
     * sign-in on this device (re-authentication) or empty for a first sign-in,
     * which registers a new client via {@link #DYNAMIC_CLIENT_ID}.
     */
    public PendingSignIn begin(String hostId, String redirectUri, String knownClientId,
                               String loginHint) {
        String state = randomToken();
        String nonce = randomToken();
        String verifier = randomToken();
        boolean register = knownClientId == null || knownClientId.isEmpty();
        String clientId = register ? DYNAMIC_CLIENT_ID : knownClientId;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", clientId);
        if (register) {
            params.put("agent_name_hint", APP_NAME);
        } else if (loginHint != null && !loginHint.isEmpty()) {
            params.put("login_hint", loginHint);
        }
        params.put("ext_agent_host_id", hostId);
        params.put("response_type", "code");
        params.put("redirect_uri", redirectUri);
        params.put("scope", SCOPE);
        params.put("resource", RESOURCE);
        params.put("state", state);
        params.put("nonce", nonce);
        params.put("code_challenge_method", "S256");
        params.put("code_challenge", codeChallenge(verifier));
        String url = AUTHORIZE_URL + "?" + formEncode(params);
        return new PendingSignIn(clientId, redirectUri, state, nonce, verifier, url);
    }

    /** Result of the loopback redirect, after the state check. */
    public static final class Callback {
        public final String code;
        public final String clientId;
        public final String scope;

        Callback(String code, String clientId, String scope) {
            this.code = code;
            this.clientId = clientId;
            this.scope = scope;
        }
    }

    /**
     * Validates the redirect's query string against {@code pending}. The issued
     * client id comes back only for a new registration; otherwise the one sent
     * in the request stays.
     */
    public Callback parseCallback(PendingSignIn pending, String query) throws IOException {
        Map<String, String> params = parseQuery(query);
        if (!pending.state.equals(params.get("state"))) {
            throw new IOException("Logowanie przerwane: niezgodny parametr state.");
        }
        String error = params.get("error");
        if (error != null) {
            if ("access_denied".equals(error)) {
                throw new IOException("Logowanie do ChatGPT anulowane.");
            }
            String description = params.get("error_description");
            throw new IOException("Logowanie do ChatGPT nie powiodło się: "
                    + (description != null ? description : error));
        }
        String code = params.get("code");
        if (code == null || code.isEmpty()) {
            throw new IOException("Logowanie do ChatGPT nie zwróciło kodu autoryzacji.");
        }
        String issued = params.get("client_id");
        String clientId = issued != null && !issued.isEmpty() ? issued : pending.clientId;
        if (DYNAMIC_CLIENT_ID.equals(clientId)) {
            throw new IOException("ChatGPT nie wydał identyfikatora klienta dla Mealspire.");
        }
        String scope = params.get("scope");
        return new Callback(code, clientId, scope == null ? "" : scope);
    }

    public Map<String, String> tokenExchangeForm(PendingSignIn pending, Callback callback) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", callback.clientId);
        form.put("code", callback.code);
        form.put("code_verifier", pending.codeVerifier);
        form.put("redirect_uri", pending.redirectUri);
        form.put("resource", RESOURCE);
        return form;
    }

    public static Map<String, String> refreshForm(String clientId, String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId);
        form.put("refresh_token", refreshToken);
        form.put("resource", RESOURCE);
        return form;
    }

    public static Map<String, String> revokeForm(String clientId, String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("token", refreshToken);
        form.put("token_type_hint", "refresh_token");
        form.put("client_id", clientId);
        return form;
    }

    /** Parsed token-endpoint answer. */
    public static final class TokenResponse {
        public final String accessToken;
        public final String refreshToken;
        public final String idToken;
        public final long expiresInSeconds;
        public final String scope;

        TokenResponse(String accessToken, String refreshToken, String idToken,
                      long expiresInSeconds, String scope) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.idToken = idToken;
            this.expiresInSeconds = expiresInSeconds;
            this.scope = scope;
        }
    }

    /** Thrown when the refresh token is no longer accepted — the user must sign in again. */
    public static final class SessionExpiredException extends IOException {
        public SessionExpiredException() {
            super("Sesja ChatGPT wygasła — zaloguj się ponownie (Więcej… → Zaloguj się kontem ChatGPT).");
        }
    }

    public static TokenResponse parseTokenResponse(HttpTransport.Response response)
            throws IOException {
        JSONObject root;
        try {
            root = new JSONObject(response.body);
        } catch (JSONException e) {
            throw new IOException("Nieczytelna odpowiedź logowania ChatGPT (HTTP "
                    + response.status + ").");
        }
        if (!response.isSuccess() || root.has("error")) {
            String error = root.optString("error", "");
            if ("invalid_grant".equals(error)) {
                throw new SessionExpiredException();
            }
            String description = root.optString("error_description", error);
            throw new IOException("Logowanie do ChatGPT nie powiodło się: "
                    + (description.isEmpty() ? "HTTP " + response.status : description));
        }
        String access = root.optString("access_token", "");
        if (access.isEmpty()) {
            throw new IOException("Logowanie do ChatGPT nie zwróciło tokenu dostępu.");
        }
        return new TokenResponse(access, root.optString("refresh_token", ""),
                root.optString("id_token", ""), root.optLong("expires_in", 3600),
                root.optString("scope", ""));
    }

    static boolean hasScope(String scopes, String wanted) {
        for (String s : scopes.trim().split("\\s+")) {
            if (s.equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    static String codeChallenge(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64Url.encode(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64Url.encode(bytes);
    }

    /** RFC 3986-style form encoding (spaces as %20, which every server accepts). */
    public static String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) {
            return params;
        }
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.put(decode(key), decode(value));
        }
        return params;
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return s;
        }
    }
}
