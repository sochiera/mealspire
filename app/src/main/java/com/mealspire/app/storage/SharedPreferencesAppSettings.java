package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.AppSettings;
import com.mealspire.app.domain.TasteSurvey;

/**
 * {@link AppSettings} backed by {@link SharedPreferences}, so the default
 * portion size survives app restarts and configuration changes.
 */
public final class SharedPreferencesAppSettings implements AppSettings {

    private static final String PREFS_NAME = "mealspire_settings";
    private static final String KEY_DEFAULT_SERVINGS = "default_servings";
    private static final String KEY_SERVINGS_CHOSEN = "servings_chosen";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_SURVEY_LENGTH = "survey_length";
    private static final int MAX_SERVINGS = 12;

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesAppSettings(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public int loadDefaultServings() {
        int value = sharedPreferences.getInt(KEY_DEFAULT_SERVINGS, DEFAULT_SERVINGS);
        return isValid(value) ? value : DEFAULT_SERVINGS;
    }

    @Override
    public void saveDefaultServings(int servings) {
        if (!isValid(servings)) {
            return;
        }
        sharedPreferences.edit()
                .putInt(KEY_DEFAULT_SERVINGS, servings)
                .putBoolean(KEY_SERVINGS_CHOSEN, true)
                .apply();
    }

    @Override
    public boolean hasChosenServings() {
        return sharedPreferences.getBoolean(KEY_SERVINGS_CHOSEN, false);
    }

    @Override
    public boolean isOnboardingDone() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    @Override
    public void markOnboardingDone() {
        sharedPreferences.edit()
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .apply();
    }

    @Override
    public int loadSurveyLength() {
        int value = sharedPreferences.getInt(KEY_SURVEY_LENGTH, TasteSurvey.DEFAULT_PAIRS);
        return value >= 1 ? value : TasteSurvey.DEFAULT_PAIRS;
    }

    @Override
    public void saveSurveyLength(int pairs) {
        if (pairs < 1) {
            return;
        }
        sharedPreferences.edit()
                .putInt(KEY_SURVEY_LENGTH, pairs)
                .apply();
    }

    private static boolean isValid(int servings) {
        return servings >= 1 && servings <= MAX_SERVINGS;
    }
}
