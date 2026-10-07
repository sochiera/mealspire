package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

public class IdTokenVerifierTest {

    private static final long NOW = 1_800_000_000L;

    private final TestJwt jwt = new TestJwt();
    private final IdTokenVerifier verifier = new IdTokenVerifier();

    private IdTokenVerifier.Claims verify(String token) throws Exception {
        return verifier.verify(token, jwt.jwks(), ChatGptOAuth.ISSUER, "oaiapp_x", "n1", NOW);
    }

    @Test
    public void acceptsValidTokenAndReturnsIdentity() throws Exception {
        IdTokenVerifier.Claims claims = verify(jwt.sign(TestJwt.claims("oaiapp_x", "n1", NOW + 600)));
        assertEquals("user-123", claims.subject);
        assertEquals("ola@example.com", claims.email);
    }

    @Test
    public void acceptsAudienceArray() throws Exception {
        JSONObject payload = TestJwt.claims("ignored", "n1", NOW + 600)
                .put("aud", new JSONArray().put("other").put("oaiapp_x"));
        assertEquals("user-123", verify(jwt.sign(payload)).subject);
    }

    @Test
    public void rejectsTamperedPayload() throws Exception {
        String token = jwt.sign(TestJwt.claims("oaiapp_x", "n1", NOW + 600));
        String[] parts = token.split("\\.");
        String forged = Base64Url.encode(TestJwt.claims("oaiapp_x", "n1", NOW + 600)
                .put("sub", "attacker").toString().getBytes("UTF-8"));
        expectRejected(parts[0] + "." + forged + "." + parts[2]);
    }

    @Test
    public void rejectsWrongNonce() throws Exception {
        expectRejected(jwt.sign(TestJwt.claims("oaiapp_x", "other", NOW + 600)));
    }

    @Test
    public void rejectsWrongAudience() throws Exception {
        expectRejected(jwt.sign(TestJwt.claims("oaiapp_someone_else", "n1", NOW + 600)));
    }

    @Test
    public void rejectsWrongIssuer() throws Exception {
        expectRejected(jwt.sign(TestJwt.claims("oaiapp_x", "n1", NOW + 600)
                .put("iss", "https://evil.example")));
    }

    @Test
    public void rejectsExpiredToken() throws Exception {
        expectRejected(jwt.sign(TestJwt.claims("oaiapp_x", "n1", NOW - 3600)));
    }

    @Test
    public void rejectsUnsignedAlgorithm() throws Exception {
        expectRejected(jwt.sign(new JSONObject().put("alg", "none").put("kid", TestJwt.KID),
                TestJwt.claims("oaiapp_x", "n1", NOW + 600)));
    }

    @Test
    public void rejectsUnknownKeyId() throws Exception {
        expectRejected(jwt.sign(new JSONObject().put("alg", "RS256").put("kid", "rotated-away"),
                TestJwt.claims("oaiapp_x", "n1", NOW + 600)));
    }

    @Test
    public void rejectsGarbage() throws Exception {
        expectRejected("not-a-jwt");
        expectRejected("");
    }

    private void expectRejected(String token) throws Exception {
        try {
            verify(token);
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("zweryfikować"));
            return;
        }
        throw new AssertionError("token should be rejected");
    }
}
