package com.mealspire.app.domain;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializuje zamrożony agregat gustu do JSON-a. Wersjonowany: agregat
 * zapisany inną wersją schematu jest porzucany (świadomie — nie da się go
 * przeliczyć, a błędna interpretacja jest gorsza niż wolniejszy start modelu).
 */
public final class FrozenTasteAggregateSerializer {

    private static final String KEY_VERSION = "v";
    private static final String KEY_AS_OF = "asOf";
    private static final String KEY_SCORES = "scores";
    private static final String KEY_OBSERVATIONS = "obs";
    private static final String KEY_EXPLICIT_MASS = "explicitMass";

    public String toJson(FrozenTasteAggregate aggregate) {
        try {
            JSONObject root = new JSONObject();
            root.put(KEY_VERSION, FrozenTasteAggregate.SCHEMA_VERSION);
            root.put(KEY_AS_OF, aggregate.getAsOf());
            JSONObject scores = new JSONObject();
            for (Map.Entry<String, Double> entry : aggregate.getScores().entrySet()) {
                scores.put(entry.getKey(), entry.getValue());
            }
            root.put(KEY_SCORES, scores);
            JSONObject observations = new JSONObject();
            for (Map.Entry<String, Integer> entry
                    : aggregate.getObservations().entrySet()) {
                observations.put(entry.getKey(), entry.getValue());
            }
            root.put(KEY_OBSERVATIONS, observations);
            root.put(KEY_EXPLICIT_MASS, aggregate.getExplicitPositiveMass());
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public FrozenTasteAggregate fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return FrozenTasteAggregate.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            if (root.optInt(KEY_VERSION, -1) != FrozenTasteAggregate.SCHEMA_VERSION) {
                return FrozenTasteAggregate.empty();
            }
            Map<String, Double> scores = new LinkedHashMap<>();
            JSONObject scoresJson = root.optJSONObject(KEY_SCORES);
            if (scoresJson != null) {
                Iterator<String> keys = scoresJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    scores.put(key, scoresJson.optDouble(key, 0.0));
                }
            }
            Map<String, Integer> observations = new LinkedHashMap<>();
            JSONObject observationsJson = root.optJSONObject(KEY_OBSERVATIONS);
            if (observationsJson != null) {
                Iterator<String> keys = observationsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    observations.put(key, observationsJson.optInt(key, 0));
                }
            }
            return new FrozenTasteAggregate(root.optLong(KEY_AS_OF, 0L), scores,
                    observations, root.optDouble(KEY_EXPLICIT_MASS, 0.0));
        } catch (JSONException e) {
            return FrozenTasteAggregate.empty();
        }
    }
}
