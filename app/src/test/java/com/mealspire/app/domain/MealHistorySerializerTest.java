package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MealHistorySerializerTest {

    private final MealHistorySerializer serializer = new MealHistorySerializer();

    @Test
    public void roundTripPreservesEntries() {
        MealHistory history = MealHistory.empty()
                .record("Pierogi", 100L)
                .record("Naleśniki", 200L);

        MealHistory restored = serializer.fromJson(serializer.toJson(history));

        assertEquals(100L, restored.lastEatenAt("Pierogi"));
        assertEquals(200L, restored.lastEatenAt("Naleśniki"));
    }

    @Test
    public void nullOrBlankYieldsEmptyHistory() {
        assertTrue(serializer.fromJson(null).recentTitles(10).isEmpty());
        assertTrue(serializer.fromJson("").recentTitles(10).isEmpty());
    }

    @Test
    public void malformedJsonYieldsEmptyHistory() {
        assertTrue(serializer.fromJson("nonsense").recentTitles(10).isEmpty());
    }
}
