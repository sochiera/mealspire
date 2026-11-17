package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.FoodLog;
import com.mealspire.app.domain.FoodLogSerializer;
import com.mealspire.app.domain.FoodLogStore;

/** {@link FoodLogStore} backed by {@link SharedPreferences}; stays on the phone. */
public final class SharedPreferencesFoodLogStore implements FoodLogStore {

    private static final String PREFS_NAME = "mealspire_food_log";
    private static final String KEY_LOG = "food_log_json";

    private final SharedPreferences sharedPreferences;
    private final FoodLogSerializer serializer = new FoodLogSerializer();

    public SharedPreferencesFoodLogStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public FoodLog load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_LOG, null));
    }

    @Override
    public void save(FoodLog log) {
        // commit(): odpowiedź z powiadomienia zapisuje receiver, którego proces może zaraz zniknąć.
        sharedPreferences.edit()
                .putString(KEY_LOG, serializer.toJson(log))
                .commit();
    }
}
