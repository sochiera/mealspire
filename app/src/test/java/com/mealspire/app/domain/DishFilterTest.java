package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DishFilterTest {

    private final DishFilter filter = new DishFilter();

    private List<Recipe> recipes(String... titles) {
        List<Recipe> list = new ArrayList<>();
        for (String t : titles) {
            list.add(new Recipe(t, "opis"));
        }
        return list;
    }

    private boolean containsTitle(List<Recipe> recipes, String title) {
        for (Recipe r : recipes) {
            if (r.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void excludesDishesMatchingADislikedTerm() {
        UserPreferences prefs = UserPreferences.empty().withDislike("pizza");
        List<Recipe> result = filter.excludeDisliked(
                recipes("Pizza margherita", "Sałatka grecka"), prefs);
        assertFalse(containsTitle(result, "Pizza margherita"));
        assertTrue(containsTitle(result, "Sałatka grecka"));
    }

    @Test
    public void matchIsCaseInsensitive() {
        UserPreferences prefs = UserPreferences.empty().withDislike("GRZYBY");
        List<Recipe> result = filter.excludeDisliked(
                recipes("Risotto z grzybami", "Naleśniki"), prefs);
        assertEquals(1, result.size());
        assertEquals("Naleśniki", result.get(0).getTitle());
    }

    @Test
    public void emptyDislikesKeepsEverything() {
        List<Recipe> input = recipes("A", "B");
        assertEquals(2, filter.excludeDisliked(input, UserPreferences.empty()).size());
    }
}
