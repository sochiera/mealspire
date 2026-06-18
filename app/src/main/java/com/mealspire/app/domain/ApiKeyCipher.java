package com.mealspire.app.domain;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Password-based encryption for the bundled API key.
 *
 * <p>The key is derived from the password with PBKDF2 (HMAC-SHA1, available on
 * minSdk 23) and the secret is sealed with AES/GCM, whose authentication tag
 * lets us tell a wrong password apart from a right one. The stored blob is
 * hex-encoded (not Base64) so it works the same on the JVM and on Android 23+.
 *
 * <p>This is deliberately modest protection: anyone with the app and the
 * password can recover the key. It only keeps the key out of the repo in
 * plaintext.
 */
public final class ApiKeyCipher {

    private static final String KDF = "PBKDF2WithHmacSHA1";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    /** Encrypts {@code plaintext} under {@code password}, returning a hex blob. */
    public String encrypt(String plaintext, String password) throws GeneralSecurityException {
        byte[] salt = randomBytes(SALT_LEN);
        byte[] iv = randomBytes(IV_LEN);
        Cipher cipher = Cipher.getInstance(TRANSFORM);
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt),
                new GCMParameterSpec(TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] out = new byte[salt.length + iv.length + ct.length];
        System.arraycopy(salt, 0, out, 0, salt.length);
        System.arraycopy(iv, 0, out, salt.length, iv.length);
        System.arraycopy(ct, 0, out, salt.length + iv.length, ct.length);
        return toHex(out);
    }

    /**
     * Decrypts a hex blob produced by {@link #encrypt}. Throws if the password is
     * wrong or the blob is malformed.
     */
    public String decrypt(String hexBlob, String password) throws GeneralSecurityException {
        byte[] all = fromHex(hexBlob);
        if (all.length < SALT_LEN + IV_LEN) {
            throw new GeneralSecurityException("Niepoprawny format zaszyfrowanego klucza.");
        }
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        byte[] ct = new byte[all.length - SALT_LEN - IV_LEN];
        System.arraycopy(all, 0, salt, 0, SALT_LEN);
        System.arraycopy(all, SALT_LEN, iv, 0, IV_LEN);
        System.arraycopy(all, SALT_LEN + IV_LEN, ct, 0, ct.length);

        Cipher cipher = Cipher.getInstance(TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt),
                new GCMParameterSpec(TAG_BITS, iv));
        // A wrong password fails the GCM tag check with AEADBadTagException.
        return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    }

    private SecretKey deriveKey(String password, byte[] salt) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        byte[] keyBytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) throws GeneralSecurityException {
        String clean = hex == null ? "" : hex.trim();
        if (clean.length() % 2 != 0) {
            throw new GeneralSecurityException("Niepoprawny format zaszyfrowanego klucza.");
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(clean.charAt(i * 2), 16);
            int lo = Character.digit(clean.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new GeneralSecurityException("Niepoprawny format zaszyfrowanego klucza.");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
