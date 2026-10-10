package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ReadyDishPool} ⇄ JSON. Uszkodzone dane dają pustą pulę (albo
 * pomijają pojedynczy wpis) zamiast crasha — pula to tylko przyspieszenie.
 */
public final class ReadyDishPoolSerializer {

    public String toJson(ReadyDishPool pool) {
        try {
            JSONArray entries = new JSONArray();
            for (DishRating entry : pool.getEntries()) {
                entries.put(new JSONObject()
                        .put("dish", entry.getDish())
                        .put("score", entry.getScore())
                        .put("reason", entry.getReason()));
            }
            JSONArray rated = new JSONArray();
            for (String name : pool.getRated()) {
                rated.put(name);
            }
            return new JSONObject()
                    .put("signature", pool.getSignature())
                    .put("entries", entries)
                    .put("rated", rated)
                    .toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public ReadyDishPool fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return ReadyDishPool.empty();
        }
        try {
            JSONObject object = new JSONObject(json);
            List<DishRating> entries = new ArrayList<>();
            JSONArray array = object.optJSONArray("entries");
            for (int i = 0; array != null && i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                String dish = item == null ? "" : item.optString("dish", "").trim();
                if (!dish.isEmpty()) {
                    entries.add(new DishRating(dish, item.optInt("score", 0),
                            item.optString("reason", "")));
                }
            }
            List<String> rated = new ArrayList<>();
            JSONArray names = object.optJSONArray("rated");
            for (int i = 0; names != null && i < names.length(); i++) {
                rated.add(names.optString(i, ""));
            }
            return new ReadyDishPool(object.optString("signature", ""), entries, rated);
        } catch (JSONException e) {
            return ReadyDishPool.empty();
        }
    }
}
