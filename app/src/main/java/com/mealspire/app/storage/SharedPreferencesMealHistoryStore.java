package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.MealHistory;
import com.mealspire.app.domain.MealHistorySerializer;
import com.mealspire.app.domain.MealHistoryStore;

/**
 * {@link MealHistoryStore} backed by {@link SharedPreferences}, so the
 * "not eaten in a while" suggestions survive restarts and rotation.
 */
public final class SharedPreferencesMealHistoryStore implements MealHistoryStore {

    private static final String PREFS_NAME = "mealspire_history";
    private static final String KEY_HISTORY = "meal_history_json";

    private final SharedPreferences sharedPreferences;
    private final MealHistorySerializer serializer = new MealHistorySerializer();

    public SharedPreferencesMealHistoryStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public MealHistory load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_HISTORY, null));
    }

    @Override
    public void save(MealHistory history) {
        sharedPreferences.edit()
                .putString(KEY_HISTORY, serializer.toJson(history))
                .apply();
    }
}
