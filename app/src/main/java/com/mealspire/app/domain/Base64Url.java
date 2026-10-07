package com.mealspire.app.domain;

import java.io.ByteArrayOutputStream;

/**
 * Unpadded base64url (RFC 4648 §5) for PKCE and JWT handling. Hand-rolled
 * because {@code java.util.Base64} needs API 26 and {@code android.util.Base64}
 * is stubbed in plain JVM tests.
 */
public final class Base64Url {

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

    private Base64Url() {
    }

    public static String encode(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 4 + 2) / 3);
        int i = 0;
        while (i + 2 < data.length) {
            int n = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8) | (data[i + 2] & 0xff);
            sb.append(ALPHABET[(n >> 18) & 63]).append(ALPHABET[(n >> 12) & 63])
                    .append(ALPHABET[(n >> 6) & 63]).append(ALPHABET[n & 63]);
            i += 3;
        }
        int rest = data.length - i;
        if (rest == 1) {
            int n = (data[i] & 0xff) << 16;
            sb.append(ALPHABET[(n >> 18) & 63]).append(ALPHABET[(n >> 12) & 63]);
        } else if (rest == 2) {
            int n = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8);
            sb.append(ALPHABET[(n >> 18) & 63]).append(ALPHABET[(n >> 12) & 63])
                    .append(ALPHABET[(n >> 6) & 63]);
        }
        return sb.toString();
    }

    /** Decodes base64url, tolerating padding and the standard {@code +/} alphabet. */
    public static byte[] decode(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length() * 3 / 4);
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '=') {
                break;
            }
            int value = valueOf(c);
            buffer = (buffer << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xff);
            }
        }
        return out.toByteArray();
    }

    private static int valueOf(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '-' || c == '+') return 62;
        if (c == '_' || c == '/') return 63;
        throw new IllegalArgumentException("Niepoprawny znak base64url: " + c);
    }
}
