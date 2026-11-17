package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** {@link FoodLog} ⇄ JSON; uszkodzone dane dają pusty dziennik zamiast crasha. */
public final class FoodLogSerializer {

    public String toJson(FoodLog log) {
        try {
            JSONArray entries = new JSONArray();
            for (FoodLog.Entry entry : log.entries()) {
                entries.put(new JSONObject()
                        .put("text", entry.getText())
                        .put("time", entry.getTimeMillis()));
            }
            return new JSONObject().put("entries", entries).toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public FoodLog fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return FoodLog.empty();
        }
        try {
            JSONArray array = new JSONObject(json).optJSONArray("entries");
            List<FoodLog.Entry> entries = new ArrayList<>();
            for (int i = 0; array != null && i < array.length(); i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry != null) {
                    entries.add(new FoodLog.Entry(entry.optString("text", ""),
                            entry.optLong("time", 0L)));
                }
            }
            return new FoodLog(entries);
        } catch (JSONException e) {
            return FoodLog.empty();
        }
    }
}
