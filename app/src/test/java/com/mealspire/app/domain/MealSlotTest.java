package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class MealSlotTest {

    @Test
    public void hasThreeSlotsAtEightTwelveAndSix() {
        assertEquals(3, MealSlot.values().length);
        assertEquals(8, MealSlot.BREAKFAST.hour());
        assertEquals(12, MealSlot.LUNCH.hour());
        assertEquals(18, MealSlot.DINNER.hour());
    }

    @Test
    public void labelsMatchTheMealButtons() {
        assertEquals("Śniadanie", MealSlot.BREAKFAST.label());
        assertEquals("Obiad", MealSlot.LUNCH.label());
        assertEquals("Kolacja", MealSlot.DINNER.label());
    }

    @Test
    public void mealIndexMapsBackToSlot() {
        assertEquals(0, MealSlot.BREAKFAST.mealIndex());
        assertEquals(1, MealSlot.LUNCH.mealIndex());
        assertEquals(2, MealSlot.DINNER.mealIndex());
        assertSame(MealSlot.LUNCH, MealSlot.byMealIndex(1));
        assertNull(MealSlot.byMealIndex(7));
    }
}
