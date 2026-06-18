package com.mealspire.app.domain;

/**
 * Small, persisted app-level settings that survive app restarts and screen
 * rotation. Currently just the default number of people a meal is cooked for,
 * so the user does not have to re-pick it every time.
 */
public interface AppSettings {

    int DEFAULT_SERVINGS = 2;

    /** Returns the remembered number of people, or {@link #DEFAULT_SERVINGS} if none. */
    int loadDefaultServings();

    /** Remembers the number of people for next time. Out-of-range values are ignored. */
    void saveDefaultServings(int servings);
}
