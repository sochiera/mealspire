package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.PreferenceStore;
import com.mealspire.app.domain.PreferencesSerializer;
import com.mealspire.app.domain.UserPreferences;

/**
 * {@link PreferenceStore} backed by {@link SharedPreferences}, so tastes survive
 * app restarts and configuration changes (e.g. screen rotation).
 */
public final class SharedPreferencesPreferenceStore implements PreferenceStore {

    private static final String PREFS_NAME = "mealspire_preferences";
    private static final String KEY_PREFERENCES = "user_preferences_json";

    private final SharedPreferences sharedPreferences;
    private final PreferencesSerializer serializer = new PreferencesSerializer();

    public SharedPreferencesPreferenceStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public UserPreferences load() {
        return serializer.fromJson(sharedPreferences.getString(KEY_PREFERENCES, null));
    }

    @Override
    public void save(UserPreferences preferences) {
        sharedPreferences.edit()
                .putString(KEY_PREFERENCES, serializer.toJson(preferences))
                .apply();
    }
}
