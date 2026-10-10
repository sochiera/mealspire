package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TasteSurvey} ⇄ JSON, żeby przerwaną ankietę dało się wznowić.
 * Uszkodzone dane dają null (brak ankiety) zamiast crasha.
 */
public final class TasteSurveySerializer {

    public String toJson(TasteSurvey survey) {
        try {
            JSONArray pairs = new JSONArray();
            for (TasteSurvey.Pair pair : survey.pairs()) {
                pairs.put(new JSONArray().put(pair.getDishA()).put(pair.getDishB()));
            }
            JSONArray answers = new JSONArray();
            for (int answer : survey.answers()) {
                answers.put(answer);
            }
            return new JSONObject()
                    .put("pairs", pairs)
                    .put("answers", answers)
                    .put("position", survey.position())
                    .put("committed", survey.committed())
                    .put("diet", survey.dietKey())
                    .toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public TasteSurvey fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(json);
            JSONArray pairsJson = object.getJSONArray("pairs");
            JSONArray answersJson = object.optJSONArray("answers");
            List<TasteSurvey.Pair> pairs = new ArrayList<>();
            List<Integer> answers = new ArrayList<>();
            for (int i = 0; i < pairsJson.length(); i++) {
                JSONArray pair = pairsJson.getJSONArray(i);
                pairs.add(new TasteSurvey.Pair(pair.getString(0), pair.getString(1)));
                answers.add(answersJson == null
                        ? TasteSurvey.NOT_ANSWERED : answersJson.optInt(i, TasteSurvey.NOT_ANSWERED));
            }
            return new TasteSurvey(pairs, answers, object.optInt("position", 0),
                    object.optInt("committed", 0), object.optString("diet", ""));
        } catch (JSONException e) {
            return null;
        }
    }
}
