package com.mealspire.app.domain;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Base64UrlTest {

    @Test
    public void encodesRfc4648VectorsWithoutPadding() {
        assertEquals("", Base64Url.encode(new byte[0]));
        assertEquals("Zg", enc("f"));
        assertEquals("Zm8", enc("fo"));
        assertEquals("Zm9v", enc("foo"));
        assertEquals("Zm9vYmFy", enc("foobar"));
    }

    @Test
    public void usesUrlSafeAlphabet() {
        assertEquals("-_8", Base64Url.encode(new byte[]{(byte) 0xfb, (byte) 0xff}));
    }

    @Test
    public void decodeRoundTripsRandomData() {
        Random random = new Random(42);
        for (int len = 0; len < 70; len++) {
            byte[] data = new byte[len];
            random.nextBytes(data);
            assertArrayEquals(data, Base64Url.decode(Base64Url.encode(data)));
        }
    }

    @Test
    public void decodeToleratesPaddingAndStandardAlphabet() {
        assertArrayEquals("fo".getBytes(StandardCharsets.UTF_8), Base64Url.decode("Zm8="));
        assertArrayEquals(new byte[]{(byte) 0xfb, (byte) 0xff}, Base64Url.decode("+/8="));
    }

    private static String enc(String s) {
        return Base64Url.encode(s.getBytes(StandardCharsets.UTF_8));
    }
}
