package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Odczytuje odpowiedź modelu {@code {"oceny":[{"danie","ocena","powod"}]}}.
 * Toleruje płot markdown, tekst wokół JSON-a, gołą tablicę i ocenę jako
 * napis; pomija wadliwe pozycje. Gdy nie da się odczytać żadnej oceny —
 * {@link IOException}, a wołający wraca do propozycji offline.
 */
public final class DishRatingParser {

    static final int MAX_REASON = 200;

    public List<DishRating> parse(String text) throws IOException {
        if (text == null) {
            throw unreadable();
        }
        int start = firstJsonStart(text);
        if (start < 0) {
            throw unreadable();
        }
        String json = text.substring(start);
        JSONArray items;
        try {
            if (json.charAt(0) == '[') {
                items = new JSONArray(trimAfterLast(json, ']'));
            } else {
                items = ratingsArray(new JSONObject(trimAfterLast(json, '}')));
            }
        } catch (JSONException e) {
            throw unreadable();
        }
        List<DishRating> ratings = new ArrayList<>();
        for (int i = 0; items != null && i < items.length(); i++) {
            DishRating rating = toRating(items.optJSONObject(i));
            if (rating != null) {
                ratings.add(rating);
            }
        }
        if (ratings.isEmpty()) {
            throw unreadable();
        }
        return ratings;
    }

    private static JSONArray ratingsArray(JSONObject root) {
        JSONArray named = root.optJSONArray("oceny");
        if (named != null) {
            return named;
        }
        java.util.Iterator<String> keys = root.keys();
        while (keys.hasNext()) {
            JSONArray any = root.optJSONArray(keys.next());
            if (any != null) {
                return any;
            }
        }
        return null;
    }

    private static DishRating toRating(JSONObject item) {
        if (item == null) {
            return null;
        }
        String dish = item.optString("danie", "").trim();
        double score = item.optDouble("ocena", Double.NaN);
        if (dish.isEmpty() || Double.isNaN(score)) {
            return null;
        }
        String reason = item.optString("powod", item.optString("powód", "")).trim();
        if (reason.isEmpty()) {
            return null; // ocena bez jednozdaniowego powodu nie spełnia kontraktu
        }
        if (reason.length() > MAX_REASON) {
            reason = reason.substring(0, MAX_REASON - 1).trim() + "…";
        }
        return new DishRating(dish, (int) Math.round(score), reason);
    }

    private static int firstJsonStart(String text) {
        int brace = text.indexOf('{');
        int bracket = text.indexOf('[');
        if (brace < 0) {
            return bracket;
        }
        return bracket < 0 ? brace : Math.min(brace, bracket);
    }

    private static String trimAfterLast(String json, char closing) {
        int end = json.lastIndexOf(closing);
        return end < 0 ? json : json.substring(0, end + 1);
    }

    private static IOException unreadable() {
        return new IOException("Nieczytelna odpowiedź AI z ocenami dań.");
    }
}
