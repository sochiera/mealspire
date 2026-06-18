package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.security.GeneralSecurityException;

/**
 * The API key is shipped encrypted with a password. {@link ApiKeyCipher} must
 * round-trip with the right password and refuse the wrong one.
 */
public class ApiKeyCipherTest {

    private final ApiKeyCipher cipher = new ApiKeyCipher();

    // Arbitrary password for exercising the crypto — NOT the real app password.
    private static final String PASSWORD = "test-haslo";

    @Test
    public void decryptsBackToOriginalWithCorrectPassword() throws Exception {
        String secret = "sk-ant-test-1234567890";
        String blob = cipher.encrypt(secret, PASSWORD);
        assertEquals(secret, cipher.decrypt(blob, PASSWORD));
    }

    @Test
    public void wrongPasswordFailsToDecrypt() throws Exception {
        String blob = cipher.encrypt("sk-ant-test-1234567890", PASSWORD);
        try {
            cipher.decrypt(blob, "inne-haslo");
            fail("Expected decryption with the wrong password to fail");
        } catch (GeneralSecurityException expected) {
            // Authentication tag mismatch — exactly what we want.
        }
    }

    @Test
    public void ciphertextIsNotPlaintextAndIsRandomized() throws Exception {
        String secret = "sk-ant-test-1234567890";
        String blob1 = cipher.encrypt(secret, PASSWORD);
        String blob2 = cipher.encrypt(secret, PASSWORD);
        // Stored form must not reveal the key...
        assertFalse(blob1.contains(secret));
        // ...and a fresh salt/IV means the same input encrypts differently.
        assertNotEquals(blob1, blob2);
    }
}
