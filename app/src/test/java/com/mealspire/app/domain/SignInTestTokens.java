package com.mealspire.app.domain;

/** Public bridge to {@link TestJwt} for activity tests outside this package. */
public final class SignInTestTokens {

    private static final TestJwt JWT = new TestJwt();

    private SignInTestTokens() {
    }

    public static String jwks() throws Exception {
        return JWT.jwks();
    }

    public static String idToken(String clientId, String nonce, long expSeconds) throws Exception {
        return JWT.sign(TestJwt.claims(clientId, nonce, expSeconds));
    }
}
