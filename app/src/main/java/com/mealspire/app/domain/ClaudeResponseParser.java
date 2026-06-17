package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Parses an Anthropic Messages API response body into the assistant text,
 * surfacing API errors as exceptions.
 */
public final class ClaudeResponseParser {

    public String extractText(String responseBody) throws IOException {
        JSONObject root;
        try {
            root = new JSONObject(responseBody);
        } catch (JSONException e) {
            throw new IOException("Nie udało się odczytać odpowiedzi API: " + e.getMessage(), e);
        }

        if (root.has("error")) {
            JSONObject error = root.optJSONObject("error");
            String message = error != null ? error.optString("message", "nieznany błąd") : "nieznany błąd";
            throw new IOException("Błąd API: " + message);
        }

        JSONArray content = root.optJSONArray("content");
        if (content == null) {
            throw new IOException("Odpowiedź API nie zawiera treści.");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.optJSONObject(i);
            if (block != null && "text".equals(block.optString("type"))) {
                sb.append(block.optString("text"));
            }
        }
        return sb.toString();
    }
}
