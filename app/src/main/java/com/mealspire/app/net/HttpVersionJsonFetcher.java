package com.mealspire.app.net;

import com.mealspire.app.domain.UpdateChecker;
import com.mealspire.app.domain.VersionJsonSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Downloads dist/wersja.json over HttpURLConnection and returns the raw body.
 * Unlike {@link HttpPageFetcher} this does no HTML extraction — the JSON must
 * arrive verbatim. Small size cap guards against a surprise huge response.
 * Call from a background thread.
 */
public final class HttpVersionJsonFetcher implements VersionJsonSource {

    private static final int TIMEOUT_MS = 20000;
    private static final int MAX_CHARS = 64 * 1024;

    @Override
    public String fetchJson() throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(UpdateChecker.VERSION_URL).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mealspire/1.0");

            int status = connection.getResponseCode();
            if (status >= 400) {
                throw new IOException("Nie udało się sprawdzić wersji (HTTP " + status + ").");
            }
            return readBody(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && sb.length() <= MAX_CHARS) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
