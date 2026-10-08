package com.mealspire.app.domain;
import org.json.*;
import org.junit.Test;
import java.security.SecureRandom;
import java.util.*;
import java.io.IOException;
import static org.junit.Assert.*;
public class BackendApiTest {
    private final long now=1800000000000L;
    private final InMemoryChatGptSessionStore store=new InMemoryChatGptSessionStore();
    private final FakeHttpTransport http=new FakeHttpTransport();
    private BackendApi api() {
        store.save(new ChatGptSession("oaiapp_x","access","refresh-secret","id",now+3600000,"sub","e","gpt-6-luna","gpt-6.1-sol"));
        return new BackendApi(new ChatGptAccount(store,http,new ChatGptOAuth(new SecureRandom()),new IdTokenVerifier(),()->now),http,"https://51.83.199.206:8443");
    }
    private RecipeRequest request() { return new RecipeRequest("Obiad",UserPreferences.empty().withLike("Zupa"),Collections.emptyList(),Collections.emptyList()); }
    @Test public void sendsDomainDataNotPromptsOrRefreshToken() throws Exception {
        BackendRecipeService service=new BackendRecipeService(api());
        http.respond(200,"{\"proposals\":[{\"name\":\"Zupa\",\"description\":\"Lekka\",\"time\":\"20\",\"ingredients\":[\"woda\"]}]}");
        assertEquals("Zupa",service.proposeDishes(request(),3).get(0).getName());
        FakeHttpTransport.Call call=http.calls.get(0);
        assertEquals("https://51.83.199.206:8443/v1/proposals",call.url);
        assertEquals("access",call.bearer);
        assertFalse(call.json.contains("refresh-secret"));
        JSONObject j=new JSONObject(call.json);
        assertFalse(j.has("input")); assertFalse(j.has("prompt"));
        assertEquals("Zupa",j.getJSONArray("likes").getString(0));
    }
    @Test public void unauthorizedRefreshesOnlyOnPhone() throws Exception {
        BackendApi api=api();
        http.respond(401,"{}").respond(200,"{\"access_token\":\"new\",\"refresh_token\":\"new-refresh\",\"expires_in\":3600}").respond(200,"{}");
        api.call("taste",BackendWire.request(request()));
        assertEquals("new",http.calls.get(2).bearer);
        assertFalse(http.calls.get(2).json.contains("new-refresh"));
    }
    @Test(expected=IOException.class) public void backendFailureIsRecoverableIOException() throws Exception {
        BackendApi api=api(); http.respond(502,"{}"); api.call("taste",BackendWire.request(request()));
    }
    @Test public void requestRoundTripPreservesDietAndContextInputs() throws Exception {
        RecipeRequest original=request().withTasteContext(new TasteContext(Collections.singletonList("local"),null));
        RecipeRequest decoded=BackendWire.request(BackendWire.request(original));
        assertEquals(original.getPreferences().getLikes(),decoded.getPreferences().getLikes());
        assertTrue(decoded.getTasteContext().isEmpty());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsPlainHttp() { new BackendApi(null,http,"http://51.83.199.206"); }
}
