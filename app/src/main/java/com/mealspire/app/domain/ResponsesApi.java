package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Request/response shapes of the OpenAI Responses API as allowed for ChatGPT
 * plan usage: only {@code model}, {@code input}, {@code store:false} and
 * {@code stream:true} — no temperature or token limits. Pure and unit-testable.
 *
 * @see <a href="https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference">Models and inference</a>
 */
public final class ResponsesApi {

    public static final String RESPONSES_URL = ChatGptOAuth.RESOURCE + "/responses";
    public static final String MODELS_URL = ChatGptOAuth.RESOURCE + "/models";

    private ResponsesApi() {
    }

    public static String buildRequest(String model, String systemPrompt, String userPrompt) {
        try {
            JSONArray input = new JSONArray();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                input.put(message("developer", systemPrompt));
            }
            input.put(message("user", userPrompt));

            JSONObject root = new JSONObject();
            root.put("model", model);
            root.put("input", input);
            root.put("store", false);
            root.put("stream", true);
            return root.toString();
        } catch (JSONException e) {
            // Inputs are plain strings; this should never happen.
            throw new IllegalStateException("Nie udało się zbudować zapytania", e);
        }
    }

    private static JSONObject message(String role, String content) throws JSONException {
        JSONObject m = new JSONObject();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    /**
     * Slug of {@code family} in the account's catalog, among models the account
     * may use ({@code visibility == "list"}), or empty when the plan lacks it.
     */
    public static String resolveModel(String modelsJson, GptModel family) throws IOException {
        try {
            JSONArray models = new JSONObject(modelsJson).optJSONArray("models");
            List<String> listed = new ArrayList<>();
            if (models != null) {
                for (int i = 0; i < models.length(); i++) {
                    JSONObject m = models.optJSONObject(i);
                    if (m != null && "list".equals(m.optString("visibility"))
                            && !m.optString("slug").isEmpty()) {
                        listed.add(m.optString("slug"));
                    }
                }
            }
            return family.resolve(listed);
        } catch (JSONException e) {
            throw new IOException("Nieczytelna lista modeli ChatGPT.", e);
        }
    }

    /**
     * Collects the answer text from a server-sent-events body. Succeeds only on
     * {@code response.completed}; failures map to Polish messages for the UI.
     */
    public static String parseStream(String body) throws IOException {
        StringBuilder text = new StringBuilder();
        boolean completed = false;
        for (String line : body.split("\n")) {
            line = line.trim();
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            JSONObject event;
            try {
                event = new JSONObject(data);
            } catch (JSONException e) {
                continue;
            }
            String type = event.optString("type");
            switch (type) {
                case "response.output_text.delta":
                    text.append(event.optString("delta"));
                    break;
                case "response.completed":
                    completed = true;
                    break;
                case "response.failed": {
                    JSONObject response = event.optJSONObject("response");
                    JSONObject error = response == null ? null : response.optJSONObject("error");
                    throw apiError(error);
                }
                case "error":
                    throw apiError(event);
                case "response.incomplete":
                    throw new IOException("Odpowiedź ChatGPT została przerwana. Spróbuj ponownie.");
                default:
                    break;
            }
        }
        if (!completed) {
            throw new IOException("Odpowiedź ChatGPT nie została dokończona. Spróbuj ponownie.");
        }
        return text.toString();
    }

    /** Error body of a non-2xx answer (plain JSON, not a stream). */
    public static IOException parseErrorBody(int status, String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject error = root.optJSONObject("error");
            return apiError(error != null ? error : root);
        } catch (JSONException e) {
            return new IOException("Żądanie do ChatGPT nie powiodło się (HTTP " + status + ").");
        }
    }

    static IOException apiError(JSONObject error) {
        String code = error == null ? "" : error.optString("code", "");
        String message = error == null ? "" : error.optString("message", "");
        switch (code) {
            case "subscription_sharing_usage_limit_exceeded":
                return new IOException("Wykorzystano limit Twojego planu ChatGPT dla Mealspire. "
                        + "Spróbuj później albo zmień limit w ustawieniach ChatGPT.");
            case "subscription_sharing_usage_unavailable":
                return new IOException("Plan ChatGPT jest chwilowo niedostępny. Spróbuj za chwilę.");
            default:
                String detail = !message.isEmpty() ? message : (!code.isEmpty() ? code : "nieznany błąd");
                return new IOException("Błąd ChatGPT: " + detail);
        }
    }
}
