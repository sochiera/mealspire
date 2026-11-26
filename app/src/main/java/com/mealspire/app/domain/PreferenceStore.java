package com.mealspire.app.domain;

/**
 * Persists {@link UserPreferences} between app launches and across screen
 * rotation. Abstracted so the domain layer doesn't depend on Android storage.
 */
public interface PreferenceStore {
    UserPreferences load();

    void save(UserPreferences preferences);

    /**
     * Zapis, który mówi, czy dane naprawdę są trwałe (np. {@code commit()} zamiast
     * {@code apply()}); zapis wyborów z ankiety zatwierdza ją dopiero przy {@code true}.
     */
    default boolean saveDurably(UserPreferences preferences) {
        save(preferences);
        return true;
    }
}
