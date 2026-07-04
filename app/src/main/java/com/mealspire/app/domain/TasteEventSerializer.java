package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializuje dziennik zdarzeń gustu do JSON-a (SharedPreferences). Odporny na
 * null/śmieci/nieznane typy (np. z nowszej wersji appki) — takie wpisy są
 * pomijane zamiast wywracać cały dziennik.
 */
public final class TasteEventSerializer {

    private static final String KEY_EVENTS = "events";
    private static final String KEY_TYPE = "t";
    private static final String KEY_DISH = "d";
    private static final String KEY_MEAL = "m";
    private static final String KEY_TIMESTAMP = "ts";
    private static final String KEY_EXPLORATORY = "x";

    public String toJson(TasteEventLog log) {
        try {
            JSONArray events = new JSONArray();
            for (TasteEvent event : log.events()) {
                JSONObject entry = new JSONObject();
                entry.put(KEY_TYPE, event.getType().name());
                entry.put(KEY_DISH, event.getDishTitle());
                entry.put(KEY_MEAL, event.getMealIndex());
                entry.put(KEY_TIMESTAMP, event.getTimestamp());
                if (event.isExploratory()) {
                    entry.put(KEY_EXPLORATORY, true);
                }
                events.put(entry);
            }
            JSONObject root = new JSONObject();
            root.put(KEY_EVENTS, events);
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public TasteEventLog fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return TasteEventLog.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONArray events = root.optJSONArray(KEY_EVENTS);
            List<TasteEvent> parsed = new ArrayList<>();
            if (events != null) {
                for (int i = 0; i < events.length(); i++) {
                    TasteEvent event = readEvent(events.optJSONObject(i));
                    if (event != null) {
                        parsed.add(event);
                    }
                }
            }
            return new TasteEventLog(parsed);
        } catch (JSONException e) {
            return TasteEventLog.empty();
        }
    }

    private static TasteEvent readEvent(JSONObject entry) {
        if (entry == null) {
            return null;
        }
        TasteEvent.Type type;
        try {
            type = TasteEvent.Type.valueOf(entry.optString(KEY_TYPE, ""));
        } catch (IllegalArgumentException e) {
            return null; // nieznany typ (nowsza wersja appki) — pomiń wpis
        }
        TasteEvent event = new TasteEvent(type,
                entry.optString(KEY_DISH, ""),
                entry.optInt(KEY_MEAL, TasteEvent.NO_MEAL),
                entry.optLong(KEY_TIMESTAMP, 0L),
                entry.optBoolean(KEY_EXPLORATORY, false));
        return event.isValid() ? event : null;
    }
}
