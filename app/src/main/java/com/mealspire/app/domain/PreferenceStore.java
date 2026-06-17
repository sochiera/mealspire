package com.mealspire.app.domain;

/**
 * Persists {@link UserPreferences} between app launches and across screen
 * rotation. Abstracted so the domain layer doesn't depend on Android storage.
 */
public interface PreferenceStore {
    UserPreferences load();

    void save(UserPreferences preferences);
}
