package com.mealspire.app.net;

import com.mealspire.app.domain.ChatGptOAuth;
import com.mealspire.app.domain.HttpTransport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * {@link HttpTransport} over HttpURLConnection — no third-party HTTP library,
 * minSdk 23. Call from a background thread.
 */
public final class HttpUrlTransport implements HttpTransport {

    private static final int CONNECT_TIMEOUT_MS = 30000;
    // Streamed answers keep the socket busy; this bounds a single silent gap.
    private static final int READ_TIMEOUT_MS = 90000;

    @Override
    public Response postForm(String url, Map<String, String> form) throws IOException {
        return send("POST", url, null, "application/x-www-form-urlencoded",
                ChatGptOAuth.formEncode(form));
    }

    @Override
    public Response get(String url, String bearerToken) throws IOException {
        return send("GET", url, bearerToken, null, null);
    }

    @Override
    public Response postJson(String url, String bearerToken, String json) throws IOException {
        return send("POST", url, bearerToken, "application/json", json);
    }

    private static Response send(String method, String url, String bearerToken,
                                 String contentType, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json, text/event-stream");
            if (bearerToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", contentType);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            return new Response(status, readBody(status >= 400
                    ? connection.getErrorStream() : connection.getInputStream()));
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
