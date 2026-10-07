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

    @Test
    public void picksFirstListedModel() throws IOException {
        assertEquals("gpt-b", ResponsesApi.pickModel("{\"models\":["
                + "{\"slug\":\"gpt-a\",\"display_name\":\"A\",\"visibility\":\"hide\"},"
                + "{\"slug\":\"gpt-b\",\"display_name\":\"B\",\"visibility\":\"list\"},"
                + "{\"slug\":\"gpt-c\",\"display_name\":\"C\",\"visibility\":\"list\"}]}"));
    }

    @Test
    public void noListedModelGivesEmpty() throws IOException {
        assertEquals("", ResponsesApi.pickModel("{\"models\":[]}"));
        assertEquals("", ResponsesApi.pickModel("{}"));
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
