package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.SecretStore;

/**
 * {@link SecretStore} backed by app-private {@link SharedPreferences}, so the
 * unlocked API key survives app restarts and the password is only asked once.
 */
public final class SharedPreferencesSecretStore implements SecretStore {

    private static final String PREFS_NAME = "mealspire_secrets";
    private static final String KEY_API_KEY = "api_key";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesSecretStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public String loadApiKey() {
        return sharedPreferences.getString(KEY_API_KEY, "");
    }

    @Override
    public boolean hasApiKey() {
        return !loadApiKey().isEmpty();
    }

    @Override
    public void saveApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            clear();
            return;
        }
        sharedPreferences.edit().putString(KEY_API_KEY, key.trim()).apply();
    }

    @Override
    public void clear() {
        sharedPreferences.edit().remove(KEY_API_KEY).apply();
    }
}
