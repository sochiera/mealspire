package com.mealspire.app.domain;

/**
 * Decides whether the app has learned enough about the user to switch from the
 * bundled, hand-picked catalogue ({@link BuiltInRecipes}) to AI-personalized
 * proposals. A fresh install has no taste data to draw conclusions from, so it
 * sticks to the built-in dishes even when an API key is available; only once the
 * user has liked a handful of dishes — meaning {@link TasteProfiler} actually has
 * something to work with — does the app start "drawing conclusions" and
 * proposing new, AI-invented dishes.
 */
public final class PersonalizationReadiness {

    /** Liked dishes needed before AI-personalized proposals kick in. */
    public static final int MIN_LIKED_DISHES = 5;

    public boolean isReady(UserPreferences preferences) {
        return preferences != null && preferences.getLikes().size() >= MIN_LIKED_DISHES;
    }
}
