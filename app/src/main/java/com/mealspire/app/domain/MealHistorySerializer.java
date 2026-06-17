package com.mealspire.app.domain;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes {@link MealHistory} to/from a JSON object of title → timestamp.
 * Tolerant of null/blank/malformed input (returns empty history).
 */
public final class MealHistorySerializer {

    public String toJson(MealHistory history) {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, Long> entry : history.asMap().entrySet()) {
                root.put(entry.getKey(), entry.getValue());
            }
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public MealHistory fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return MealHistory.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            Map<String, Long> entries = new LinkedHashMap<>();
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                entries.put(key, root.optLong(key, 0L));
            }
            return new MealHistory(entries);
        } catch (JSONException e) {
            return MealHistory.empty();
        }
    }
}
