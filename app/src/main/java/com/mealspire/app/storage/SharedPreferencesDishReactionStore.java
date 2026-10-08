package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.DishReactionLog;
import com.mealspire.app.domain.DishReactionSerializer;
import com.mealspire.app.domain.DishReactionStore;

/** {@link DishReactionStore} backed by {@link SharedPreferences}; stays on the phone. */
public final class SharedPreferencesDishReactionStore implements DishReactionStore {

    private static final String PREFS_NAME = "mealspire_reactions";
    private static final String KEY_REACTIONS = "dish_reactions_json";

    private final SharedPreferences sharedPreferences;
    private final DishReactionSerializer serializer = new DishReactionSerializer();

    public SharedPreferencesDishReactionStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public DishReactionLog load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_REACTIONS, null));
    }

    @Override
    public void save(DishReactionLog log) {
        sharedPreferences.edit()
                .putString(KEY_REACTIONS, serializer.toJson(log))
                .apply();
    }
}
