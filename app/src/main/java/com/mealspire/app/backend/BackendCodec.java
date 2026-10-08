package com.mealspire.app.backend;

import com.mealspire.app.domain.*;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Stable v1 data contract. No system prompts or provider URLs cross the client boundary. */
public final class BackendCodec {
    private BackendCodec() {}
    public static JSONObject envelope() throws JSONException { return new JSONObject().put("apiVersion", 1); }
    public static JSONObject response(String body) throws IOException {
        try {
            JSONObject data = new JSONObject(body);
            if (data.getInt("apiVersion") != 1) throw new IOException("Backend wymaga aktualizacji APK.");
            return data;
        } catch (JSONException e) { throw new IOException("Nieprawidłowa odpowiedź backendu.", e); }
    }
    public static JSONObject request(RecipeRequest r) throws JSONException {
        TasteContext t = r.getTasteContext();
        return new JSONObject().put("mealType", r.getMealType())
                .put("preferences", new JSONObject(new PreferencesSerializer().toJson(r.getPreferences())))
                .put("household", new JSONObject(new HouseholdProfileSerializer().toJson(r.getHouseholdProfile())))
                .put("recent", new JSONArray(r.getRecentToAvoid())).put("choices", new JSONArray(r.getChoiceFragments()))
                .put("known", new JSONArray(r.getKnownDishes())).put("affinities", new JSONArray(r.getTasteAffinities()))
                .put("taste", new JSONObject().put("profile", new JSONArray(t.getProfileSentences()))
                        .put("examples", new JSONArray(t.getExampleDishes())).put("exploration", t.getExplorationSentence())
                        .put("antiMonotony", t.getAntiMonotonySentence()));
    }
    public static RecipeRequest request(JSONObject o) {
        JSONObject t = o.optJSONObject("taste"); if (t == null) t = new JSONObject();
        return new RecipeRequest(o.optString("mealType", "Śniadanie"),
                new PreferencesSerializer().fromJson(o.optJSONObject("preferences") == null ? "{}" : o.optJSONObject("preferences").toString()),
                strings(o.optJSONArray("recent")), strings(o.optJSONArray("choices")), strings(o.optJSONArray("known")),
                strings(o.optJSONArray("affinities")),
                new HouseholdProfileSerializer().fromJson(o.optJSONObject("household") == null ? "{}" : o.optJSONObject("household").toString()))
                .withTasteContext(new TasteContext(strings(t.optJSONArray("profile")), strings(t.optJSONArray("examples")))
                        .withExploration(t.optString("exploration")).withAntiMonotony(t.optString("antiMonotony")));
    }
    public static List<String> strings(JSONArray a) {
        List<String> result = new ArrayList<>();
        if (a != null) for (int i=0; i<a.length(); i++) if (a.opt(i) instanceof String) result.add(a.optString(i));
        return result;
    }
    public static JSONObject recipe(Recipe r) throws JSONException {
        return new JSONObject().put("title", r.getTitle()).put("details", r.getDetails());
    }
    public static Recipe recipe(JSONObject o) throws JSONException {
        String title = o.getString("title"), details = o.getString("details");
        if (title.trim().isEmpty() || details.trim().isEmpty()) throw new JSONException("Empty recipe");
        return new Recipe(title, details);
    }
    public static JSONObject proposal(DishProposal p) throws JSONException {
        return new JSONObject().put("name", p.getName()).put("description", p.getDescription())
                .put("time", p.getTime()).put("ingredients", new JSONArray(p.getKeyIngredients()));
    }
    public static DishProposal proposal(JSONObject o) throws JSONException {
        String name = o.getString("name"); if (name.trim().isEmpty()) throw new JSONException("Empty proposal");
        return new DishProposal(name, o.optString("description"), o.optString("time"), strings(o.optJSONArray("ingredients")));
    }
    public static JSONObject reaction(DishReaction r) throws JSONException {
        return new JSONObject().put("dish", r.getDish()).put("description", r.getDescription())
                .put("liked", r.isLiked()).put("time", r.getTimeMillis());
    }
    public static DishReaction reaction(JSONObject o) throws JSONException {
        String dish = o.getString("dish"); if (dish.trim().isEmpty() || dish.length() > 200) throw new JSONException("Bad reaction");
        return new DishReaction(dish, o.optString("description"), o.getBoolean("liked"), o.getLong("time"));
    }
    public static JSONObject rating(DishRating r) throws JSONException {
        return new JSONObject().put("dish", r.getDish()).put("score", r.getScore()).put("reason", r.getReason());
    }
    public static DishRating rating(JSONObject o) throws JSONException {
        String dish = o.getString("dish"); if (dish.trim().isEmpty()) throw new JSONException("Empty rating");
        return new DishRating(dish, o.getInt("score"), o.optString("reason"));
    }
    public static Recipe[][] catalog(JSONObject o) throws JSONException {
        JSONArray meals = o.getJSONArray("meals"); if (meals.length() != 3) throw new JSONException("Invalid catalog");
        Recipe[][] result = new Recipe[3][];
        for (int i=0; i<3; i++) {
            JSONArray entries = meals.getJSONArray(i); if (entries.length()>500) throw new JSONException("Catalog too large");
            result[i] = new Recipe[entries.length()];
            for (int j=0; j<entries.length(); j++) result[i][j] = recipe(entries.getJSONObject(j));
        }
        return result;
    }
}
