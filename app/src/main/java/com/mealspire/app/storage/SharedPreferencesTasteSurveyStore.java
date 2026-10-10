package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.TasteSurvey;
import com.mealspire.app.domain.TasteSurveySerializer;
import com.mealspire.app.domain.TasteSurveyStore;

/** {@link TasteSurveyStore} backed by {@link SharedPreferences}; stays on the phone. */
public final class SharedPreferencesTasteSurveyStore implements TasteSurveyStore {

    private static final String PREFS_NAME = "mealspire_taste_survey";
    private static final String KEY_SURVEY = "taste_survey_json";

    private final SharedPreferences sharedPreferences;
    private final TasteSurveySerializer serializer = new TasteSurveySerializer();

    public SharedPreferencesTasteSurveyStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public TasteSurvey load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_SURVEY, null));
    }

    @Override
    public void save(TasteSurvey survey) {
        // commit(), nie apply(): odpowiedź ma przetrwać zabicie procesu tuż po kliknięciu.
        sharedPreferences.edit()
                .putString(KEY_SURVEY, serializer.toJson(survey))
                .commit();
    }
}
