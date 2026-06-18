package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes a {@link Cookbook} to/from a JSON array of entries. Tolerant of
 * null/blank/malformed input (returns an empty cookbook).
 */
public final class CookbookSerializer {

    private static final String KEY_TITLE = "title";
    private static final String KEY_RECIPE = "recipe";
    private static final String KEY_SOURCE = "source";

    public String toJson(Cookbook cookbook) {
        JSONArray array = new JSONArray();
        for (CookbookEntry entry : cookbook.getEntries()) {
            try {
                JSONObject object = new JSONObject();
                object.put(KEY_TITLE, entry.getTitle());
                object.put(KEY_RECIPE, entry.getRecipe());
                object.put(KEY_SOURCE, entry.getSource());
                array.put(object);
            } catch (JSONException ignored) {
                // Skip an entry that somehow can't be serialized.
            }
        }
        return array.toString();
    }

    public Cookbook fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Cookbook.empty();
        }
        try {
            JSONArray array = new JSONArray(json);
            List<CookbookEntry> entries = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    entries.add(new CookbookEntry(
                            object.optString(KEY_TITLE, ""),
                            object.optString(KEY_RECIPE, ""),
                            object.optString(KEY_SOURCE, "")));
                }
            }
            return new Cookbook(entries);
        } catch (JSONException e) {
            return Cookbook.empty();
        }
    }
}
