package com.mealspire.app.domain;

import com.mealspire.app.backend.*;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class BackendClientTest {
    private ChatGptAccount account(FakeHttpTransport transport) {
        InMemoryChatGptSessionStore store = new InMemoryChatGptSessionStore();
        store.save(new ChatGptSession("client", "access", "refresh-secret", "id-secret", 9999999999999L,
                "subject", "email", "gpt-6-luna", "gpt-6.1-sol"));
        return new ChatGptAccount(store, transport, new ChatGptOAuth(new java.security.SecureRandom()), new IdTokenVerifier(), () -> 1000);
    }
    @Test public void sendsTaskDataAndOnlyAccessToken() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport().respond(200, "{\"apiVersion\":1,\"proposals\":[{\"name\":\"Ryż\",\"ingredients\":[]}]}");
        BackendClient client = new BackendClient(() -> "https://trusted.example/api", account(transport), transport);
        assertEquals("Ryż", client.proposeDishes(new RecipeRequest("Obiad", UserPreferences.empty(), null, null), 3).get(0).getName());
        FakeHttpTransport.Call call = transport.calls.get(0);
        assertEquals("https://trusted.example/api/v1/proposals", call.url);
        assertEquals("access", call.bearer);
        assertFalse(call.json.contains("refresh-secret")); assertFalse(call.json.contains("id-secret"));
        assertFalse(call.json.contains("systemPrompt")); assertFalse(call.json.contains("responses"));
        assertEquals("Obiad", new JSONObject(call.json).getJSONObject("request").getString("mealType"));
    }
    @Test public void refreshesOnceAfter401() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport().respond(401, "{}")
                .respond(200, "{\"access_token\":\"new-access\",\"refresh_token\":\"rotated\",\"expires_in\":3600}")
                .respond(200, "{\"apiVersion\":1,\"recipe\":{\"title\":\"Ryż\",\"details\":\"Ugotuj\"}}");
        BackendClient client = new BackendClient(() -> "https://trusted.example/api", account(transport), transport);
        client.generateRecipeFor("Ryż", new RecipeRequest("Obiad", UserPreferences.empty(), null, null));
        assertEquals(3, transport.calls.size());
        assertEquals(ChatGptOAuth.TOKEN_URL, transport.calls.get(1).url);
        assertEquals("new-access", transport.calls.get(2).bearer);
    }
    @Test public void rejectsUnsafeEndpointsBeforeObtainingCredentials() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport();
        for (String endpoint : Arrays.asList("http://trusted.example", "https://user:pass@example.com", "https://example.com?x=1", "")) {
            BackendClient client = new BackendClient(() -> endpoint, account(transport), transport);
            try { client.generateRecipeFor("Ryż", new RecipeRequest("Obiad", UserPreferences.empty(), null, null)); fail(); }
            catch (java.io.IOException expected) { assertEquals(0, transport.calls.size()); }
        }
    }
    @Test public void catalogIsPublicAndMalformedOrNewMajorIsRejected() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport().respond(200, "{\"apiVersion\":2,\"meals\":[]}");
        BackendClient client = new BackendClient(() -> "https://trusted.example", account(transport), transport);
        try { client.catalog(); fail(); } catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("APK")); }
        assertNull(transport.calls.get(0).bearer);
    }
}
