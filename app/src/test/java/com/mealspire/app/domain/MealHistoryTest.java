package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class MealHistoryTest {

    @Test
    public void recordStoresTimestamp() {
        MealHistory history = MealHistory.empty().record("Pizza", 100L);
        assertEquals(100L, history.lastEatenAt("Pizza"));
    }

    @Test
    public void neverEatenReturnsZero() {
        assertEquals(0L, MealHistory.empty().lastEatenAt("Cokolwiek"));
    }

    @Test
    public void recordIsCaseInsensitiveAndKeepsLatest() {
        MealHistory history = MealHistory.empty()
                .record("Pizza", 100L)
                .record("pizza", 250L);
        assertEquals(250L, history.lastEatenAt("PIZZA"));
        assertEquals(1, history.recentTitles(10).size());
    }

    @Test
    public void recentTitlesOrderedByMostRecentFirst() {
        MealHistory history = MealHistory.empty()
                .record("A", 100L)
                .record("B", 300L)
                .record("C", 200L);
        List<String> recent = history.recentTitles(2);
        assertEquals("B", recent.get(0));
        assertEquals("C", recent.get(1));
    }

    @Test
    public void recordReturnsNewInstance() {
        MealHistory original = MealHistory.empty();
        original.record("Zupa", 1L);
        assertEquals(0L, original.lastEatenAt("Zupa"));
    }

    @Test
    public void blankTitleIsIgnored() {
        MealHistory history = MealHistory.empty().record("  ", 100L);
        assertTrue(history.recentTitles(10).isEmpty());
    }
}
