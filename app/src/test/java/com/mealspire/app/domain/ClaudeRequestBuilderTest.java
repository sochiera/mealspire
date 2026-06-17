package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ClaudeRequestBuilderTest {

    private final ClaudeRequestBuilder builder = new ClaudeRequestBuilder("claude-sonnet-4-6", 1024);

    @Test
    public void buildsValidMessagesPayload() throws Exception {
        String body = builder.build("system text", "user text");
        JSONObject json = new JSONObject(body);

        assertEquals("claude-sonnet-4-6", json.getString("model"));
        assertEquals(1024, json.getInt("max_tokens"));
        assertEquals("system text", json.getString("system"));

        JSONArray messages = json.getJSONArray("messages");
        assertEquals(1, messages.length());
        assertEquals("user", messages.getJSONObject(0).getString("role"));
        assertEquals("user text", messages.getJSONObject(0).getString("content"));
    }

    @Test
    public void escapesSpecialCharacters() throws Exception {
        String body = builder.build("sys", "Cytat \"x\" i nowa\nlinia");
        JSONObject json = new JSONObject(body);
        assertEquals("Cytat \"x\" i nowa\nlinia",
                json.getJSONArray("messages").getJSONObject(0).getString("content"));
        // The serialized form must not contain a raw newline inside the string.
        assertTrue(body.contains("\\n"));
    }
}
