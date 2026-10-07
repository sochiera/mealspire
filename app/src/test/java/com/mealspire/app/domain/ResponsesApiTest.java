package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

public class ResponsesApiTest {

    @Test
    public void requestUsesOnlyParametersAllowedForPlanUsage() throws Exception {
        JSONObject root = new JSONObject(ResponsesApi.buildRequest("gpt-x", "Jesteś kucharzem.", "Obiad \"szybki\""));

        assertEquals("gpt-x", root.getString("model"));
        assertFalse(root.getBoolean("store"));
        assertTrue(root.getBoolean("stream"));
        assertEquals(4, root.length());
        JSONArray input = root.getJSONArray("input");
        assertEquals(2, input.length());
        assertEquals("developer", input.getJSONObject(0).getString("role"));
        assertEquals("Jesteś kucharzem.", input.getJSONObject(0).getString("content"));
        assertEquals("user", input.getJSONObject(1).getString("role"));
        assertEquals("Obiad \"szybki\"", input.getJSONObject(1).getString("content"));
    }

    @Test
    public void requestWithoutSystemPromptSendsOnlyUserMessage() throws Exception {
        JSONObject root = new JSONObject(ResponsesApi.buildRequest("gpt-x", "", "hej"));
        assertEquals(1, root.getJSONArray("input").length());
    }

    private static String catalog(String... slugsAndVisibility) {
        StringBuilder sb = new StringBuilder("{\"models\":[");
        for (int i = 0; i < slugsAndVisibility.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"slug\":\"").append(slugsAndVisibility[i])
                    .append("\",\"display_name\":\"x\",\"visibility\":\"")
                    .append(slugsAndVisibility[i + 1]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    @Test
    public void resolvesDocumentedLunaAndSolIds() throws IOException {
        String models = catalog("gpt-6-astra", "list", "gpt-6.1-sol", "list", "gpt-6-luna", "list");
        assertEquals("gpt-6-luna", ResponsesApi.resolveModel(models, GptModel.LUNA));
        assertEquals("gpt-6.1-sol", ResponsesApi.resolveModel(models, GptModel.SOL));
    }

    @Test
    public void prefersNewerDocumentedSol() throws IOException {
        assertEquals("gpt-6.1-sol", ResponsesApi.resolveModel(
                catalog("gpt-6-sol", "list", "gpt-6.1-sol", "list"), GptModel.SOL));
        assertEquals("gpt-6-sol", ResponsesApi.resolveModel(
                catalog("gpt-6-sol", "list"), GptModel.SOL));
    }

    @Test
    public void picksNewestFamilyMemberFromCatalogWhenNoDocumentedIdListed() throws IOException {
        assertEquals("gpt-6.2-luna", ResponsesApi.resolveModel(
                catalog("gpt-5.6-luna", "list", "gpt-6.2-luna", "list"), GptModel.LUNA));
    }

    @Test
    public void hiddenOrForeignModelsAreNeverSubstituted() throws IOException {
        assertEquals("", ResponsesApi.resolveModel(
                catalog("gpt-6-luna", "hide", "gpt-6-astra", "list", "lunar-x", "list"), GptModel.LUNA));
        assertEquals("", ResponsesApi.resolveModel(catalog("gpt-6-luna", "list"), GptModel.SOL));
        assertEquals("", ResponsesApi.resolveModel("{}", GptModel.LUNA));
    }

    @Test
    public void lunaIsTheDefault() {
        assertEquals(GptModel.LUNA, GptModel.DEFAULT);
        assertEquals(GptModel.LUNA, GptModel.fromName(""));
        assertEquals(GptModel.LUNA, GptModel.fromName("NIEZNANY"));
        assertEquals(GptModel.SOL, GptModel.fromName("SOL"));
    }

    @Test
    public void collectsDeltasUntilCompleted() throws IOException {
        String sse = "event: response.created\n"
                + "data: {\"type\":\"response.created\"}\n\n"
                + "event: response.output_text.delta\n"
                + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Placki \"}\n\n"
                + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"z jabłkami\\nPrzepis\"}\n\n"
                + "data: {\"type\":\"response.completed\",\"response\":{}}\n\n";
        assertEquals("Placki z jabłkami\nPrzepis", ResponsesApi.parseStream(sse));
    }

    @Test
    public void streamWithoutCompletedIsAnError() {
        expect("data: {\"type\":\"response.output_text.delta\",\"delta\":\"Pół\"}\n", "nie została dokończona");
    }

    @Test
    public void incompleteStreamIsAnError() {
        expect("data: {\"type\":\"response.incomplete\"}\n", "przerwana");
    }

    @Test
    public void usageLimitHasFriendlyMessage() {
        expect("data: {\"type\":\"response.failed\",\"response\":{\"error\":"
                + "{\"code\":\"subscription_sharing_usage_limit_exceeded\",\"message\":\"x\"}}}\n",
                "limit");
    }

    @Test
    public void temporaryUnavailabilityHasFriendlyMessage() {
        expect("data: {\"type\":\"response.failed\",\"response\":{\"error\":"
                + "{\"code\":\"subscription_sharing_usage_unavailable\"}}}\n", "chwilowo");
    }

    @Test
    public void errorEventSurfacesMessage() {
        expect("data: {\"type\":\"error\",\"code\":\"unknown_error\",\"message\":\"Ups\"}\n", "Ups");
    }

    @Test
    public void errorBodyOfNonStreamingFailure() {
        assertTrue(ResponsesApi.parseErrorBody(400, "{\"error\":{\"message\":\"Model niedostępny\"}}")
                .getMessage().contains("Model niedostępny"));
        assertTrue(ResponsesApi.parseErrorBody(502, "<html>").getMessage().contains("HTTP 502"));
    }

    private static void expect(String sse, String fragment) {
        try {
            ResponsesApi.parseStream(sse);
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragment));
            return;
        }
        throw new AssertionError("expected IOException");
    }
}
