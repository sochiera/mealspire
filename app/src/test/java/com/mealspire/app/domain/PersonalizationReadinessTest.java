package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PersonalizationReadinessTest {

    private final PersonalizationReadiness readiness = new PersonalizationReadiness();

    @Test
    public void freshInstallIsNotReadyForAiPersonalization() {
        assertFalse(readiness.isReady(UserPreferences.empty()));
    }

    @Test
    public void aFewLikesAreStillNotEnough() {
        UserPreferences preferences = UserPreferences.empty()
                .withLike("Naleśniki").withLike("Owsianka z dodatkami");
        assertFalse(readiness.isReady(preferences));
    }

    @Test
    public void becomesReadyOnceEnoughDishesAreLiked() {
        UserPreferences preferences = UserPreferences.empty();
        for (int i = 0; i < PersonalizationReadiness.MIN_LIKED_DISHES; i++) {
            preferences = preferences.withLike("Danie " + i);
        }
        assertTrue(readiness.isReady(preferences));
    }

    @Test
    public void staysReadyWithMoreLikesThanTheThreshold() {
        UserPreferences preferences = UserPreferences.empty();
        for (int i = 0; i < PersonalizationReadiness.MIN_LIKED_DISHES + 10; i++) {
            preferences = preferences.withLike("Danie " + i);
        }
        assertTrue(readiness.isReady(preferences));
    }
}
