package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builds the JSON request body for the Anthropic Messages API. Pure and
 * unit-testable; uses org.json so escaping is handled correctly.
 */
public final class ClaudeRequestBuilder {

    private final String model;
    private final int maxTokens;

    public ClaudeRequestBuilder(String model, int maxTokens) {
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public String build(String systemPrompt, String userPrompt) {
        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", userPrompt);

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject root = new JSONObject();
            root.put("model", model);
            root.put("max_tokens", maxTokens);
            root.put("system", systemPrompt);
            root.put("messages", messages);
            return root.toString();
        } catch (JSONException e) {
            // Inputs are plain strings; this should never happen.
            throw new IllegalStateException("Nie udało się zbudować zapytania", e);
        }
    }
}
