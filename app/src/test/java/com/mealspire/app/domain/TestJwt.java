package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;

/** Signs RS256 ID tokens and publishes a matching JWKS, like auth.openai.com. */
final class TestJwt {

    static final String KID = "test-key";
    private static KeyPair cached;

    final KeyPair keyPair;

    TestJwt() {
        this.keyPair = sharedKeyPair();
    }

    private static synchronized KeyPair sharedKeyPair() {
        if (cached == null) {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                cached = generator.generateKeyPair();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return cached;
    }

    String jwks() throws Exception {
        RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
        JSONObject jwk = new JSONObject();
        jwk.put("kty", "RSA");
        jwk.put("kid", KID);
        jwk.put("alg", "RS256");
        jwk.put("n", Base64Url.encode(unsigned(key.getModulus())));
        jwk.put("e", Base64Url.encode(unsigned(key.getPublicExponent())));
        return new JSONObject().put("keys", new JSONArray().put(jwk)).toString();
    }

    String sign(JSONObject payload) throws Exception {
        return sign(new JSONObject().put("alg", "RS256").put("kid", KID).put("typ", "JWT"), payload);
    }

    String sign(JSONObject header, JSONObject payload) throws Exception {
        String input = Base64Url.encode(header.toString().getBytes(StandardCharsets.UTF_8)) + "."
                + Base64Url.encode(payload.toString().getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(input.getBytes(StandardCharsets.US_ASCII));
        return input + "." + Base64Url.encode(signature.sign());
    }

    static JSONObject claims(String clientId, String nonce, long exp) throws Exception {
        return new JSONObject()
                .put("iss", ChatGptOAuth.ISSUER)
                .put("aud", clientId)
                .put("sub", "user-123")
                .put("email", "ola@example.com")
                .put("nonce", nonce)
                .put("exp", exp)
                .put("iat", exp - 3600);
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0 && bytes.length > 1) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }
}
