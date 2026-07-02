package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RecentlyShownFilterTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long WINDOW = 3 * DAY;

    private final RecentlyShownFilter filter = new RecentlyShownFilter();

    private final Recipe pizza = new Recipe("Pizza", "opis");
    private final Recipe salad = new Recipe("Sałatka", "opis");
    private final Recipe soup = new Recipe("Zupa", "opis");

    @Test
    public void neverShownDishesAreAlwaysFresh() {
        long now = 10 * DAY;
        List<Recipe> result = filter.apply(
                Arrays.asList(pizza, salad, soup), MealHistory.empty(), WINDOW, now, 2);
        assertEquals(3, result.size());
    }

    @Test
    public void excludesADishShownWithinTheWindow() {
        long now = 10 * DAY;
        MealHistory history = MealHistory.empty().record("Pizza", now - DAY);
        List<Recipe> result = filter.apply(
                Arrays.asList(pizza, salad, soup), history, WINDOW, now, 2);
        assertFalse("shown yesterday, still inside the 3-day window", containsTitle(result, "Pizza"));
        assertTrue(containsTitle(result, "Sałatka"));
        assertTrue(containsTitle(result, "Zupa"));
    }

    @Test
    public void bringsBackADishOnceItsWindowHasPassed() {
        long now = 10 * DAY;
        MealHistory history = MealHistory.empty().record("Pizza", now - 4 * DAY);
        List<Recipe> result = filter.apply(
                Arrays.asList(pizza, salad, soup), history, WINDOW, now, 2);
        assertTrue("shown 4 days ago, past the 3-day window", containsTitle(result, "Pizza"));
    }

    @Test
    public void fallsBackToStalestRecentDishWhenPoolTooSmall() {
        long now = 10 * DAY;
        // Both dishes were shown recently; only two candidates exist for three slots
        // worth of freshness, so the filter must still return enough to choose from.
        MealHistory history = MealHistory.empty()
                .record("Pizza", now - DAY)
                .record("Sałatka", now - 2 * DAY);
        List<Recipe> result = filter.apply(Arrays.asList(pizza, salad), history, WINDOW, now, 2);
        assertEquals(2, result.size());
    }

    @Test
    public void prefersTheLeastRecentlyShownWhenPaddingIsNeeded() {
        long now = 10 * DAY;
        // Three candidates, all shown recently, but we only need 1 result: the
        // filter should bring back the stalest (longest-ago) one first.
        MealHistory history = MealHistory.empty()
                .record("Pizza", now - DAY)
                .record("Sałatka", now - 2 * DAY)
                .record("Zupa", now - 12 * 60 * 60 * 1000L);
        List<Recipe> result = filter.apply(Arrays.asList(pizza, salad, soup), history, WINDOW, now, 1);
        assertEquals("Sałatka", result.get(0).getTitle());
    }

    @Test
    public void emptyCandidatesGiveEmptyResult() {
        assertTrue(filter.apply(Arrays.asList(), MealHistory.empty(), WINDOW, 0L, 3).isEmpty());
    }

    private static boolean containsTitle(List<Recipe> recipes, String title) {
        for (Recipe recipe : recipes) {
            if (recipe.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }
}
