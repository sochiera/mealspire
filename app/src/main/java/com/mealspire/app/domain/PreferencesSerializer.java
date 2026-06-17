package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes {@link UserPreferences} to and from JSON for persistence. Tolerant
 * of null/blank/malformed input, returning empty preferences instead of throwing.
 */
public final class PreferencesSerializer {

    private static final String KEY_LIKES = "likes";
    private static final String KEY_DISLIKES = "dislikes";

    public String toJson(UserPreferences prefs) {
        try {
            JSONObject root = new JSONObject();
            root.put(KEY_LIKES, new JSONArray(prefs.getLikes()));
            root.put(KEY_DISLIKES, new JSONArray(prefs.getDislikes()));
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public UserPreferences fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return UserPreferences.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            return new UserPreferences(
                    readArray(root.optJSONArray(KEY_LIKES)),
                    readArray(root.optJSONArray(KEY_DISLIKES)));
        } catch (JSONException e) {
            return UserPreferences.empty();
        }
    }

    private static List<String> readArray(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, null);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }
}
