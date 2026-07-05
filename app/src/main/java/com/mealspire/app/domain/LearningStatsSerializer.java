package com.mealspire.app.domain;

import org.json.JSONException;
import org.json.JSONObject;

/** Serializacja liczników uczenia do JSON-a; śmieci dają puste liczniki. */
public final class LearningStatsSerializer {

    private static final String KEY_SHOWN = "shown";
    private static final String KEY_ENGAGED = "engaged";
    private static final String KEY_REROLLS = "rerolls";
    private static final String KEY_AI = "ai";
    private static final String KEY_AI_UNTAGGED = "aiUntagged";

    public String toJson(LearningStats stats) {
        try {
            JSONObject root = new JSONObject();
            root.put(KEY_SHOWN, stats.getTriosShown());
            root.put(KEY_ENGAGED, stats.getTriosEngaged());
            root.put(KEY_REROLLS, stats.getRerolls());
            root.put(KEY_AI, stats.getAiProposals());
            root.put(KEY_AI_UNTAGGED, stats.getAiProposalsUntagged());
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public LearningStats fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return LearningStats.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            return new LearningStats(
                    root.optInt(KEY_SHOWN, 0),
                    root.optInt(KEY_ENGAGED, 0),
                    root.optInt(KEY_REROLLS, 0),
                    root.optInt(KEY_AI, 0),
                    root.optInt(KEY_AI_UNTAGGED, 0));
        } catch (JSONException e) {
            return LearningStats.empty();
        }
    }
}
