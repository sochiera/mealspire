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

    /**
     * Remembers the number of people for next time. Out-of-range values are
     * ignored. A valid value also marks the choice as made, so the app never has
     * to ask again ({@link #hasChosenServings()}).
     */
    void saveDefaultServings(int servings);

    /**
     * Whether the user has ever picked the number of people. Once true, the app
     * stops asking and just reuses {@link #loadDefaultServings()}.
     */
    boolean hasChosenServings();

    /**
     * Whether the first-launch onboarding quiz was finished or skipped. Once
     * true, the quiz never shows again (independently of the saved profile).
     */
    boolean isOnboardingDone();

    /** Marks the onboarding quiz as finished or skipped, permanently. */
    void markOnboardingDone();

    /**
     * How many A/B pairs the taste survey asks; {@link TasteSurvey#DEFAULT_PAIRS}
     * until the user picks a length in "Więcej…".
     */
    int loadSurveyLength();

    /** Remembers the survey length; values below 1 are ignored. */
    void saveSurveyLength(int pairs);
}
