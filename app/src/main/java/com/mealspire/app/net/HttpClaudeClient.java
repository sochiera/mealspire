package com.mealspire.app.net;

import com.mealspire.app.domain.ClaudeClient;
import com.mealspire.app.domain.ClaudeRequestBuilder;
import com.mealspire.app.domain.ClaudeResponseParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * {@link ClaudeClient} backed by the Anthropic Messages API over HttpURLConnection.
 * No third-party HTTP library is needed, which keeps the APK small and compatible
 * with minSdk 23. Call from a background thread.
 */
public final class HttpClaudeClient implements ClaudeClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 1024;
    private static final int TIMEOUT_MS = 30000;

    private final String apiKey;
    private final ClaudeRequestBuilder requestBuilder;
    private final ClaudeResponseParser responseParser;

    public HttpClaudeClient(String apiKey) {
        this.apiKey = apiKey;
        this.requestBuilder = new ClaudeRequestBuilder(MODEL, MAX_TOKENS);
        this.responseParser = new ClaudeResponseParser();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws IOException {
        if (!hasApiKey()) {
            throw new IOException("Brak klucza API. Ustaw anthropic.api.key w local.properties.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);

            byte[] payload = requestBuilder.build(systemPrompt, userPrompt)
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }

            int status = connection.getResponseCode();
            String body = readBody(status >= 400
                    ? connection.getErrorStream() : connection.getInputStream());
            // The parser surfaces API-level error objects; this covers transport errors.
            if (status >= 400 && body.isEmpty()) {
                throw new IOException("Żądanie nie powiodło się (HTTP " + status + ").");
            }
            return responseParser.extractText(body);
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }
}
