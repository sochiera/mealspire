package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DishReactionLog} ⇄ tablica JSON. Uszkodzone dane dają pustą listę
 * (albo pomijają pojedynczy wpis) zamiast crasha.
 */
public final class DishReactionSerializer {

    public String toJson(DishReactionLog log) {
        JSONArray array = new JSONArray();
        try {
            for (DishReaction reaction : log.all()) {
                JSONObject item = new JSONObject();
                item.put("dish", reaction.getDish());
                item.put("description", reaction.getDescription());
                item.put("liked", reaction.isLiked());
                item.put("time", reaction.getTimeMillis());
                array.put(item);
            }
        } catch (JSONException e) {
            return "[]";
        }
        return array.toString();
    }

    public DishReactionLog fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return DishReactionLog.empty();
        }
        try {
            JSONArray array = new JSONArray(json);
            List<DishReaction> reactions = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null || !item.has("liked")) {
                    continue;
                }
                reactions.add(new DishReaction(item.optString("dish", ""),
                        item.optString("description", ""), item.optBoolean("liked"),
                        item.optLong("time", 0L)));
            }
            return new DishReactionLog(reactions);
        } catch (JSONException e) {
            return DishReactionLog.empty();
        }
    }
}
