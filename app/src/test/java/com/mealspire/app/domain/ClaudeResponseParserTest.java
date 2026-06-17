package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ClaudeResponseParserTest {

    private final ClaudeResponseParser parser = new ClaudeResponseParser();

    @Test
    public void extractsTextFromMessagesResponse() throws Exception {
        String json = "{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"Zupa krem z dyni\"}],"
                + "\"stop_reason\":\"end_turn\"}";
        assertEquals("Zupa krem z dyni", parser.extractText(json));
    }

    @Test
    public void concatenatesMultipleTextBlocksAndIgnoresOthers() throws Exception {
        String json = "{\"content\":[{\"type\":\"thinking\",\"thinking\":\"...\"},"
                + "{\"type\":\"text\",\"text\":\"Linia 1\\n\"},"
                + "{\"type\":\"text\",\"text\":\"Linia 2\"}]}";
        assertEquals("Linia 1\nLinia 2", parser.extractText(json));
    }

    @Test
    public void throwsWithApiErrorMessage() {
        String json = "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\","
                + "\"message\":\"invalid x-api-key\"}}";
        try {
            parser.extractText(json);
            fail("expected exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("invalid x-api-key"));
        }
    }
}
