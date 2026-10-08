package com.mealspire.backend;

import com.mealspire.app.domain.*;
import org.json.*;
import org.junit.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class BackendServerTest {
    private BackendServer server;
    private String base;
    private final AtomicInteger calls = new AtomicInteger();
    @Before public void start() throws Exception {
        server = new BackendServer(new InetSocketAddress("127.0.0.1", 0), (token, model) -> {
            if (!"test-access".equals(token)) throw new BackendServer.ApiError(401, "unauthorized");
            return (system, user) -> {
                calls.incrementAndGet();
                if (system.contains("Oceń gust")) return "{\"profile\":[\"Lubi owsiankę\"]}";
                if (system.contains("doradcą kulinarnym")) return user.contains("Zepsuty") ? "nie wiem"
                        : "{\"oceny\":[{\"danie\":\"Schabowy\",\"ocena\":10,\"powod\":\"Mięso.\"},"
                        + "{\"danie\":\"Omlet\",\"ocena\":8,\"powod\":\"Lubi jajka.\"},{\"danie\":\"Obcy\",\"ocena\":9}]}";
                if (system.contains("pomysł") || user.contains("kurczak")) return "Nazwa: Kurczak\nOPIS: Kurczak z ryżem\nCZAS: 20 minut\nSKŁADNIKI: kurczak, ryż";
                return "Owsianka\nSkładniki:\n- płatki owsiane\nPrzygotowanie:\nUgotuj.";
            };
        });
        server.start(); base = "http://127.0.0.1:" + server.port();
    }
    @After public void stop() { server.close(); }
    private HttpResponse<String> send(String path, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body == null) request.GET(); else request.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test public void realHttpCatalogAndCompatibility() throws Exception {
        HttpResponse<String> r = send("/v1/catalog", null, null);
        assertEquals(200, r.statusCode());
        JSONObject catalog = new JSONObject(r.body());
        assertEquals(1, catalog.getInt("apiVersion"));
        assertEquals(3, catalog.getJSONArray("meals").length());
        assertTrue(catalog.getJSONArray("meals").getJSONArray(0).length() > 10);
        assertEquals("no-store", r.headers().firstValue("Cache-Control").get());
        assertEquals(404, send("/v2/catalog", null, null).statusCode());
    }
    @Test public void authBadJsonAndSizeNeverCallModel() throws Exception {
        assertEquals(401, send("/v1/recipe", "{}", null).statusCode());
        assertEquals(400, send("/v1/recipe", "broken", "test-access").statusCode());
        assertEquals(413, send("/v1/recipe", "x".repeat(270000), "test-access").statusCode());
        assertEquals(0, calls.get());
    }
    @Test public void recipeAndImportUseServerPrompts() throws Exception {
        String body = "{\"model\":\"gpt-6-luna\",\"request\":{\"mealType\":\"Śniadanie\"},\"futureField\":true}";
        JSONObject recipe = new JSONObject(send("/v1/recipe", body, "test-access").body()).getJSONObject("recipe");
        assertEquals("Owsianka", recipe.getString("title"));
        assertEquals(200, send("/v1/import", "{\"model\":\"gpt-6-luna\",\"input\":\"Owsianka\"}", "test-access").statusCode());
        assertEquals(2, calls.get());
    }
    @Test public void serverRejectsUnknownDietAndFiltersProposals() throws Exception {
        String body = "{\"model\":\"gpt-6-luna\",\"request\":{\"mealType\":\"Obiad\",\"household\":{\"exclusions\":[\"FUTURE_ALLERGY\"]}}}";
        assertEquals(400, send("/v1/proposals", body, "test-access").statusCode());
        body = body.replace("FUTURE_ALLERGY", "VEGETARIAN").replace("Obiad", "kurczak");
        // Unknown meal is rejected before inference.
        assertEquals(400, send("/v1/proposals", body, "test-access").statusCode());
    }
    @Test public void tasteIsAssessedOnServerAndSsrfIsNotFetched() throws Exception {
        assertEquals(200, send("/v1/taste", "{\"model\":\"gpt-6-luna\",\"request\":{\"preferences\":{\"likes\":[\"Owsianka\"]}}}", "test-access").statusCode());
        assertEquals(200, send("/v1/import", "{\"model\":\"gpt-6-luna\",\"input\":\"http://127.0.0.1/private\"}", "test-access").statusCode());
    }
    @Test public void validProposalsModifyAndVersionErrors() throws Exception {
        String body = "{\"model\":\"gpt-6-luna\",\"request\":{\"mealType\":\"Obiad\",\"household\":{\"exclusions\":[\"VEGETARIAN\"]}}}";
        HttpResponse<String> response = send("/v1/proposals", body, "test-access");
        assertEquals(200, response.statusCode());
        JSONArray proposals = new JSONObject(response.body()).getJSONArray("proposals");
        assertEquals(3, proposals.length());
        for (int i=0; i<proposals.length(); i++) assertTrue(DietConstraints.of(Arrays.asList(DietConstraints.Exclusion.VEGETARIAN)).allows(proposals.getJSONObject(i).toString()));
        assertEquals(200, send("/v1/modify", "{\"model\":\"gpt-6-luna\",\"recipe\":{\"title\":\"Ryż\",\"details\":\"Gotuj\"},\"instruction\":\"Dodaj warzywa\"}", "test-access").statusCode());
        assertEquals(426, send("/v1/recipe", "{\"apiVersion\":2,\"model\":\"gpt-6-luna\"}", "test-access").statusCode());
        assertEquals(400, send("/v1/recipe", "{\"model\":\"gpt-6-luna\",\"request\":{\"household\":{\"exclusions\":[7]}}}", "test-access").statusCode());
    }
    @Test public void tasteCacheAvoidsRepeatInference() throws Exception {
        String body = "{\"model\":\"gpt-6-luna\",\"request\":{\"preferences\":{\"likes\":[\"Owsianka\"]}}}";
        assertEquals(200, send("/v1/taste", body, "test-access").statusCode());
        assertEquals(200, send("/v1/taste", body, "test-access").statusCode());
        assertEquals(1, calls.get());
    }

    @Test public void rateIsOneCallFilteredByDietAndBounded() throws Exception {
        String candidates = "[{\"name\":\"Schabowy\",\"description\":\"wieprzowina\"},{\"name\":\"Omlet\",\"description\":\"jajka\"}]";
        String body = "{\"model\":\"gpt-6-luna\",\"mealType\":\"Obiad\",\"household\":{\"exclusions\":[\"NO_PORK\"]},"
                + "\"reactions\":[{\"dish\":\"Jajecznica\",\"liked\":true,\"time\":1}],\"candidates\":" + candidates + "}";
        HttpResponse<String> r = send("/v1/rate", body, "test-access");
        assertEquals(200, r.statusCode());
        JSONArray ratings = new JSONObject(r.body()).getJSONArray("ratings");
        assertEquals(1, ratings.length());
        assertEquals("Omlet", ratings.getJSONObject(0).getString("dish"));
        assertEquals(8, ratings.getJSONObject(0).getInt("score"));
        assertEquals(1, calls.get());
        assertEquals(502, send("/v1/rate", body.replace("Omlet", "Zepsuty"), "test-access").statusCode());
        StringBuilder many = new StringBuilder("[");
        for (int i = 0; i <= DishRecommender.MAX_CANDIDATES; i++) many.append(i == 0 ? "" : ",").append("{\"name\":\"D").append(i).append("\"}");
        int before = calls.get();
        assertEquals(400, send("/v1/rate", "{\"model\":\"gpt-6-luna\",\"mealType\":\"Obiad\",\"candidates\":" + many + "]}", "test-access").statusCode());
        assertEquals(401, send("/v1/rate", body, null).statusCode());
        // Dieta faktycznie używana przez endpoint musi przejść walidację, nawet obok "request".
        assertEquals(400, send("/v1/rate", body.replace("[\"NO_PORK\"]", "\"NO_PORK\"").replace("{\"model\"", "{\"request\":{},\"model\""), "test-access").statusCode());
        assertEquals(400, send("/v1/rate", body.replace("NO_PORK", "FUTURE_ALLERGY").replace("{\"model\"", "{\"request\":{},\"model\""), "test-access").statusCode());
        assertEquals(before, calls.get());
    }
    @Test public void androidTaskClientUsesRealHttpForAllOperations() throws Exception {
        class Store implements ChatGptSessionStore {
            ChatGptSession session = new ChatGptSession("client", "test-access", "local-refresh", "local-id", Long.MAX_VALUE,
                    "subject", "email", "gpt-6-luna", "gpt-6.1-sol");
            public ChatGptSession load() { return session; }
            public void save(ChatGptSession value) { session=value; }
            public void clear() { session=null; }
            public GptModel modelChoice() { return GptModel.LUNA; }
            public void saveModelChoice(GptModel value) {}
            public String hostId() { return "test-host"; }
        }
        HttpTransport wire = new HttpTransport() {
            private Response request(String url, String json, String token) throws java.io.IOException {
                try {
                    HttpResponse<String> r = send(url.substring("https://trusted.example".length()), json, token);
                    return new Response(r.statusCode(), r.body());
                } catch (Exception e) { throw new java.io.IOException(e); }
            }
            public Response get(String url, String token) throws java.io.IOException { return request(url, null, token); }
            public Response postJson(String url, String token, String body) throws java.io.IOException {
                assertFalse(body.contains("local-refresh")); assertFalse(body.contains("local-id"));
                return request(url, body, token);
            }
            public Response postForm(String url, Map<String,String> form) { throw new AssertionError("OAuth must stay local"); }
        };
        ChatGptAccount account = new ChatGptAccount(new Store(), wire, new ChatGptOAuth(new java.security.SecureRandom()), new IdTokenVerifier(), () -> 1000);
        com.mealspire.app.backend.BackendClient client = new com.mealspire.app.backend.BackendClient(() -> "https://trusted.example", account, wire);
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(), null, null);
        assertEquals(3, client.proposeDishes(request, 3).size());
        assertEquals("Owsianka", client.generateRecipeFor("Owsianka", request).getTitle());
        assertEquals("Owsianka", client.modifyRecipe(new Recipe("Ryż", "Gotuj"), "Dodaj warzywa", HouseholdProfile.empty()).getTitle());
        assertEquals("Owsianka", client.importDish("Owsianka").getTitle());
        assertEquals(3, com.mealspire.app.backend.BackendCodec.catalog(client.catalog()).length);
        List<DishRating> rated = client.rate("Obiad", Collections.emptyList(), Arrays.asList(
                new DishProposal("Omlet", "jajka", "", null), new DishProposal("Schabowy", "wieprzowina", "", null)),
                DietConstraints.empty(), 1000);
        assertEquals(2, rated.size());
        assertEquals("Schabowy", rated.get(0).getDish());
    }

    @Test public void malformedDietObjectCannotSilentlyDropConstraints() throws Exception {
        assertEquals(400, send("/v1/recipe", "{\"model\":\"gpt-6-luna\",\"request\":{\"household\":\"VEGETARIAN\"}}", "test-access").statusCode());
        assertEquals(400, send("/v1/recipe", "{\"model\":\"gpt-6-luna\",\"request\":true}", "test-access").statusCode());
        assertEquals(0, calls.get());
    }

}
