package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class StaleMealSelectorTest {

    private final StaleMealSelector selector = new StaleMealSelector();

    @Test
    public void prefersNeverEatenDish() {
        MealHistory history = MealHistory.empty().record("Pizza", 500L);
        String picked = selector.pickStalest(Arrays.asList("Pizza", "Sałatka"), history);
        assertEquals("Sałatka", picked);
    }

    @Test
    public void picksLeastRecentlyEatenWhenAllSeen() {
        MealHistory history = MealHistory.empty()
                .record("Pizza", 500L)
                .record("Sałatka", 100L)
                .record("Zupa", 300L);
        String picked = selector.pickStalest(
                Arrays.asList("Pizza", "Sałatka", "Zupa"), history);
        assertEquals("Sałatka", picked);
    }

    @Test
    public void returnsNullForEmptyCandidates() {
        assertNull(selector.pickStalest(Collections.<String>emptyList(), MealHistory.empty()));
    }

    @Test
    public void singleCandidateIsReturned() {
        assertEquals("Tost", selector.pickStalest(
                Collections.singletonList("Tost"), MealHistory.empty()));
    }
}
