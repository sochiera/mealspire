package com.mealspire.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.ApiKeyCipher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.GeneralSecurityException;

/**
 * Verifies the encrypted API key shipped in resources. The real password is
 * never stored in the repo: supply it via {@code -Dmealspire.key.password=...}
 * (or the {@code MEALSPIRE_KEY_PASSWORD} env var) to run the positive check;
 * without it that test is skipped. The "wrong password stays locked" check
 * always runs.
 */
@RunWith(RobolectricTestRunner.class)
public class EncryptedApiKeyTest {

    private String shippedBlob() {
        Context ctx = ApplicationProvider.getApplicationContext();
        return ctx.getString(R.string.encrypted_api_key);
    }

    private static String suppliedPassword() {
        String pw = System.getProperty("mealspire.key.password");
        if (pw == null || pw.isEmpty()) {
            pw = System.getenv("MEALSPIRE_KEY_PASSWORD");
        }
        return pw;
    }

    @Test
    public void unlocksWithSuppliedPassword() throws Exception {
        String password = suppliedPassword();
        assumeFalse("Set -Dmealspire.key.password to run this check",
                password == null || password.isEmpty());

        String key = new ApiKeyCipher().decrypt(shippedBlob(), password);
        assertTrue("decrypted value should look like an Anthropic key",
                key.startsWith("sk-ant-"));
    }

    @Test
    public void staysLockedWithWrongPassword() {
        try {
            new ApiKeyCipher().decrypt(shippedBlob(), "definitely-not-the-password");
            fail("Expected wrong password to fail decryption");
        } catch (GeneralSecurityException expected) {
            // good
        }
    }
}
