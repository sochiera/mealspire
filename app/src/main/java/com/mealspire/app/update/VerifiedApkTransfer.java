package com.mealspire.app.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Bounded streaming verification; caller must discard staged bytes on any failure. */
public final class VerifiedApkTransfer {
    public static final long MAX_BYTES = 100L * 1024 * 1024;
    public interface Progress { void update(long bytes, long total); }
    private VerifiedApkTransfer() { }

    public static void copy(InputStream input, OutputStream output, String sha256,
                            long length, Progress progress) throws IOException {
        if (sha256 == null || !sha256.matches("(?i)[0-9a-f]{64}"))
            throw new IOException("Brak poprawnej sumy SHA-256 aktualizacji.");
        if (length > MAX_BYTES) throw new IOException("Plik aktualizacji jest zbyt duży.");
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        byte[] buffer = new byte[16384];
        long bytes = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Pobieranie anulowane.");
            int count = input.read(buffer);
            if (count == -1) break;
            bytes += count;
            if (bytes > MAX_BYTES) throw new IOException("Plik aktualizacji jest zbyt duży.");
            digest.update(buffer, 0, count);
            output.write(buffer, 0, count);
            progress.update(bytes, length);
        }
        if (bytes == 0 || (length >= 0 && length != bytes))
            throw new IOException("Pobrano niepełną aktualizację. Spróbuj ponownie.");
        StringBuilder actual = new StringBuilder();
        for (byte b : digest.digest()) actual.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        if (!actual.toString().equalsIgnoreCase(sha256))
            throw new IOException("Suma SHA-256 nie pasuje. Spróbuj ponownie później.");
    }
}
