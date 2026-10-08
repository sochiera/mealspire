package com.mealspire.app.update;

import org.junit.Test;
import java.io.*;
import java.security.MessageDigest;
import static org.junit.Assert.*;

public class VerifiedApkTransferTest {
    private static final byte[] APK = {1, 2, 3};
    private String hash() throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(APK))
            out.append(String.format("%02x", b & 255));
        return out.toString();
    }
    @Test public void transfersAndReportsProgress() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long[] progress = {0};
        VerifiedApkTransfer.copy(new ByteArrayInputStream(APK), out, hash(), 3,
                (bytes, total) -> progress[0] = bytes);
        assertArrayEquals(APK, out.toByteArray());
        assertEquals(3, progress[0]);
    }
    @Test public void acceptsUnknownLengthAndUppercaseDigest() throws Exception {
        VerifiedApkTransfer.copy(new ByteArrayInputStream(APK), new ByteArrayOutputStream(),
                hash().toUpperCase(), -1, (bytes, total) -> {});
    }
    @Test public void rejectsCorruptionTruncationMissingHashAndEmptyBody() throws Exception {
        rejected(APK, "0".repeat(64), 3);
        rejected(APK, hash(), 4);
        rejected(APK, "", 3);
        rejected(new byte[0], hash(), -1);
    }
    private void rejected(byte[] bytes, String digest, long length) throws Exception {
        try {
            VerifiedApkTransfer.copy(new ByteArrayInputStream(bytes), new ByteArrayOutputStream(),
                    digest, length, (done, total) -> {});
            fail("Expected rejection");
        } catch (IOException expected) { }
    }
    @Test public void refusesOversizedDeclaredBodyBeforeReading() throws Exception {
        InputStream input = new InputStream() {
            public int read() { throw new AssertionError("Oversized body must not be read"); }
        };
        try {
            VerifiedApkTransfer.copy(input, new ByteArrayOutputStream(), hash(),
                    VerifiedApkTransfer.MAX_BYTES + 1, (done, total) -> {});
            fail();
        } catch (IOException expected) { }
    }
    @Test public void honorsCancellation() throws Exception {
        Thread.currentThread().interrupt();
        try { rejected(APK, hash(), 3); }
        finally { Thread.interrupted(); }
    }
}
