package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ChatGptSessionTest {

    private final ChatGptSession session = new ChatGptSession("oaiapp_x", "at", "rt", "it",
            123L, "sub", "ola@example.com", "gpt-6-luna", "gpt-6.1-sol");

    @Test
    public void jsonRoundTrip() {
        ChatGptSession copy = ChatGptSession.fromJson(session.toJson());
        assertEquals("oaiapp_x", copy.clientId);
        assertEquals("at", copy.accessToken);
        assertEquals("rt", copy.refreshToken);
        assertEquals("it", copy.idToken);
        assertEquals(123L, copy.accessTokenExpiresAtMs);
        assertEquals("sub", copy.subject);
        assertEquals("ola@example.com", copy.email);
        assertEquals("gpt-6-luna", copy.lunaModel);
        assertEquals("gpt-6.1-sol", copy.solModel);
        assertEquals("gpt-6-luna", copy.modelFor(GptModel.LUNA));
        assertEquals("gpt-6.1-sol", copy.modelFor(GptModel.SOL));
    }

    @Test
    public void missingOrCorruptDataMeansSignedOut() {
        assertNull(ChatGptSession.fromJson(null));
        assertNull(ChatGptSession.fromJson(""));
        assertNull(ChatGptSession.fromJson("{oops"));
        assertNull(ChatGptSession.fromJson("{\"clientId\":\"oaiapp_x\"}"));
    }

    @Test
    public void withTokensKeepsIdentityAndOldIdTokenWhenNoneReturned() {
        ChatGptSession refreshed = session.withTokens("at2", "rt2", "", 999L);
        assertEquals("at2", refreshed.accessToken);
        assertEquals("rt2", refreshed.refreshToken);
        assertEquals("it", refreshed.idToken);
        assertEquals(999L, refreshed.accessTokenExpiresAtMs);
        assertEquals("ola@example.com", refreshed.email);
        assertEquals("gpt-6-luna", refreshed.lunaModel);
        assertEquals("gpt-6.1-sol", refreshed.solModel);
    }
}
