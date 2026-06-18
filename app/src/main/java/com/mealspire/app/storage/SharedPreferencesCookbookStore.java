package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.Cookbook;
import com.mealspire.app.domain.CookbookSerializer;
import com.mealspire.app.domain.CookbookStore;

/**
 * {@link CookbookStore} backed by {@link SharedPreferences}, so the user's known
 * dishes survive restarts and rotation.
 */
public final class SharedPreferencesCookbookStore implements CookbookStore {

    private static final String PREFS_NAME = "mealspire_cookbook";
    private static final String KEY_COOKBOOK = "cookbook_json";

    private final SharedPreferences sharedPreferences;
    private final CookbookSerializer serializer = new CookbookSerializer();

    public SharedPreferencesCookbookStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public Cookbook load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_COOKBOOK, null));
    }

    @Override
    public void save(Cookbook cookbook) {
        sharedPreferences.edit()
                .putString(KEY_COOKBOOK, serializer.toJson(cookbook))
                .apply();
    }
}
