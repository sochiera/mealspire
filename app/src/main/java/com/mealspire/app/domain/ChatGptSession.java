package com.mealspire.app.domain;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Credentials of the app user's own ChatGPT account (Sign in with ChatGPT).
 * Nothing here is shipped with the app — every value comes from the user's
 * sign-in on this device. Immutable; a refresh produces a new instance.
 */
public final class ChatGptSession {

    public final String clientId;
    public final String accessToken;
    public final String refreshToken;
    public final String idToken;
    /** Wall-clock millis after which {@link #accessToken} must be refreshed. */
    public final long accessTokenExpiresAtMs;
    public final String subject;
    public final String email;
    public final String model;

    public ChatGptSession(String clientId, String accessToken, String refreshToken,
                          String idToken, long accessTokenExpiresAtMs, String subject,
                          String email, String model) {
        this.clientId = nonNull(clientId);
        this.accessToken = nonNull(accessToken);
        this.refreshToken = nonNull(refreshToken);
        this.idToken = nonNull(idToken);
        this.accessTokenExpiresAtMs = accessTokenExpiresAtMs;
        this.subject = nonNull(subject);
        this.email = nonNull(email);
        this.model = nonNull(model);
    }

    /** Same account and model, rotated tokens (refresh tokens are single-use). */
    public ChatGptSession withTokens(String accessToken, String refreshToken,
                                     String idToken, long accessTokenExpiresAtMs) {
        return new ChatGptSession(clientId, accessToken, refreshToken,
                idToken.isEmpty() ? this.idToken : idToken,
                accessTokenExpiresAtMs, subject, email, model);
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("clientId", clientId);
            o.put("accessToken", accessToken);
            o.put("refreshToken", refreshToken);
            o.put("idToken", idToken);
            o.put("accessTokenExpiresAtMs", accessTokenExpiresAtMs);
            o.put("subject", subject);
            o.put("email", email);
            o.put("model", model);
            return o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Parses {@link #toJson()} output; returns null for missing or corrupt data. */
    public static ChatGptSession fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            ChatGptSession session = new ChatGptSession(
                    o.optString("clientId"), o.optString("accessToken"),
                    o.optString("refreshToken"), o.optString("idToken"),
                    o.optLong("accessTokenExpiresAtMs"), o.optString("subject"),
                    o.optString("email"), o.optString("model"));
            return session.clientId.isEmpty() || session.refreshToken.isEmpty()
                    ? null : session;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
