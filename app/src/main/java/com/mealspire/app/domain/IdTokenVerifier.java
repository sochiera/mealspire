package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;

/**
 * Validates the OpenID Connect ID token from Sign in with ChatGPT: RS256
 * signature against OpenAI's JWKS, issuer, audience (the issued client id),
 * expiry and the nonce we sent. Uses only platform crypto (minSdk 23).
 */
public final class IdTokenVerifier {

    private static final long CLOCK_SKEW_SECONDS = 300;

    /** The identity claims Mealspire uses. */
    public static final class Claims {
        public final String subject;
        public final String email;

        Claims(String subject, String email) {
            this.subject = subject;
            this.email = email;
        }
    }

    public Claims verify(String idToken, String jwksJson, String expectedIssuer,
                         String expectedAudience, String expectedNonce, long nowSeconds)
            throws IOException {
        String[] parts = idToken == null ? new String[0] : idToken.split("\\.");
        if (parts.length != 3) {
            throw fail("niepoprawny format");
        }
        try {
            JSONObject header = new JSONObject(utf8(parts[0]));
            JSONObject payload = new JSONObject(utf8(parts[1]));
            if (!"RS256".equals(header.optString("alg"))) {
                throw fail("nieobsługiwany algorytm " + header.optString("alg"));
            }
            PublicKey key = findKey(jwksJson, header.optString("kid"));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(Base64Url.decode(parts[2]))) {
                throw fail("zły podpis");
            }
            if (!expectedIssuer.equals(payload.optString("iss"))) {
                throw fail("zły wystawca");
            }
            if (!hasAudience(payload, expectedAudience)) {
                throw fail("zły odbiorca");
            }
            if (payload.optLong("exp", 0) + CLOCK_SKEW_SECONDS < nowSeconds) {
                throw fail("token wygasł");
            }
            if (!expectedNonce.equals(payload.optString("nonce"))) {
                throw fail("niezgodny nonce");
            }
            String subject = payload.optString("sub", "");
            if (subject.isEmpty()) {
                throw fail("brak identyfikatora konta");
            }
            return new Claims(subject, payload.optString("email", ""));
        } catch (JSONException | GeneralSecurityException | IllegalArgumentException e) {
            throw fail(e.getMessage());
        }
    }

    private static PublicKey findKey(String jwksJson, String kid)
            throws JSONException, GeneralSecurityException, IOException {
        JSONArray keys = new JSONObject(jwksJson).getJSONArray("keys");
        for (int i = 0; i < keys.length(); i++) {
            JSONObject jwk = keys.getJSONObject(i);
            if ("RSA".equals(jwk.optString("kty")) && kid.equals(jwk.optString("kid"))) {
                BigInteger n = new BigInteger(1, Base64Url.decode(jwk.getString("n")));
                BigInteger e = new BigInteger(1, Base64Url.decode(jwk.getString("e")));
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
            }
        }
        throw fail("nieznany klucz podpisu");
    }

    private static boolean hasAudience(JSONObject payload, String expected) {
        JSONArray list = payload.optJSONArray("aud");
        if (list == null) {
            return expected.equals(payload.optString("aud"));
        }
        for (int i = 0; i < list.length(); i++) {
            if (expected.equals(list.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static String utf8(String base64Url) {
        return new String(Base64Url.decode(base64Url), StandardCharsets.UTF_8);
    }

    private static IOException fail(String reason) {
        return new IOException("Nie udało się zweryfikować logowania ChatGPT (" + reason + ").");
    }
}
