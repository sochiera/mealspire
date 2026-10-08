package com.mealspire.app.backend;

import com.mealspire.app.domain.*;
import org.json.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/** Task-level HTTP client. OAuth, refresh tokens and APK updates stay on the device. */
public final class BackendClient implements RecipeOperations, DishImporter {
    public interface Configuration { String baseUrl(); }
    private final Configuration configuration;
    private final ChatGptAccount account;
    private final HttpTransport transport;
    public BackendClient(Configuration configuration, ChatGptAccount account, HttpTransport transport) {
        this.configuration = configuration; this.account = account; this.transport = transport;
    }
    public static String validateUrl(String raw) {
        String url = raw.trim(); if (url.isEmpty()) return "";
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException("Podaj adres HTTPS serwera Mealspire.");
        while (url.endsWith("/")) url = url.substring(0, url.length()-1);
        return url;
    }
    private String url(String path) throws IOException {
        String base;
        try { base = validateUrl(configuration.baseUrl()); }
        catch (IllegalArgumentException e) { throw new IOException("Nieprawidłowy adres serwera Mealspire.", e); }
        if (base.isEmpty()) throw new IOException("Ustaw serwer Mealspire w „Więcej…” → „Serwer Mealspire”.");
        return base + path;
    }
    private JSONObject call(String path, JSONObject body) throws IOException {
        String target = url(path); // Validate before obtaining credentials.
        try {
            ChatGptSession session = account.activeSession(false);
            String model = session.modelFor(account.modelChoice());
            if (model.isEmpty()) throw new IOException("Wybrany model nie jest dostępny na Twoim koncie ChatGPT.");
            body.put("apiVersion", 1).put("model", model);
            HttpTransport.Response r = transport.postJson(target, session.accessToken, body.toString());
            if (r.status == 401) {
                session = account.activeSession(true);
                r = transport.postJson(target, session.accessToken, body.toString());
            }
            if (!r.isSuccess()) {
                if (r.status == 426) throw new IOException("Backend wymaga aktualizacji APK.");
                throw new IOException("Serwer Mealspire: HTTP " + r.status + ". Spróbuj ponownie za chwilę.");
            }
            return BackendCodec.response(r.body);
        } catch (JSONException e) { throw new IOException("Nieprawidłowe dane backendu.", e); }
    }
    public JSONObject catalog() throws IOException {
        HttpTransport.Response r = transport.get(url("/v1/catalog"), null);
        if (!r.isSuccess()) throw new IOException("Katalog Mealspire: HTTP " + r.status);
        JSONObject catalog = BackendCodec.response(r.body);
        try { BackendCodec.catalog(catalog); } catch (JSONException e) { throw new IOException("Nieprawidłowy katalog.", e); }
        return catalog;
    }
    @Override public List<DishProposal> proposeDishes(RecipeRequest request, int count) throws IOException {
        try {
            JSONArray a = call("/v1/proposals", new JSONObject().put("request", BackendCodec.request(request)).put("count", count)).getJSONArray("proposals");
            if (a.length() > count) throw new JSONException("Too many proposals");
            List<DishProposal> result = new ArrayList<>();
            for (int i=0; i<a.length(); i++) result.add(BackendCodec.proposal(a.getJSONObject(i)));
            return result;
        } catch (JSONException e) { throw new IOException("Nieprawidłowe propozycje backendu.", e); }
    }
    @Override public Recipe generateRecipeFor(String name, RecipeRequest request) throws IOException {
        try { return BackendCodec.recipe(call("/v1/recipe", new JSONObject().put("dishName", name)
                .put("request", BackendCodec.request(request))).getJSONObject("recipe")); }
        catch (JSONException e) { throw new IOException("Nieprawidłowy przepis backendu.", e); }
    }
    @Override public Recipe modifyRecipe(Recipe current, String instruction, HouseholdProfile profile) throws IOException {
        try { return BackendCodec.recipe(call("/v1/modify", new JSONObject().put("recipe", BackendCodec.recipe(current))
                .put("instruction", instruction).put("household", new JSONObject(new HouseholdProfileSerializer()
                        .toJson(profile == null ? HouseholdProfile.empty() : profile)))).getJSONObject("recipe")); }
        catch (JSONException e) { throw new IOException("Nieprawidłowy przepis backendu.", e); }
    }
    @Override public CookbookEntry importDish(String input) throws IOException {
        try {
            JSONObject response = call("/v1/import", new JSONObject().put("input", input));
            Recipe recipe = BackendCodec.recipe(response.getJSONObject("recipe"));
            return new CookbookEntry(recipe.getTitle(), recipe.getDetails(), response.optString("source", "opis"));
        } catch (JSONException e) { throw new IOException("Nieprawidłowy import backendu.", e); }
    }
}
