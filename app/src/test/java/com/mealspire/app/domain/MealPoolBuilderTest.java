package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class MealPoolBuilderTest {

    private final MealPoolBuilder builder = new MealPoolBuilder();

    private final Recipe[] builtIns = {
            new Recipe("Owsianka", "opis"),
            new Recipe("Jajecznica", "opis")
    };

    private boolean has(List<Recipe> pool, String title) {
        for (Recipe r : pool) {
            if (r.getTitle().equalsIgnoreCase(title)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void combinesBuiltInsWithCookbookEntries() {
        Cookbook cookbook = Cookbook.empty()
                .add(new CookbookEntry("Tost francuski", "opis", "zapisane"));
        List<Recipe> pool = builder.build(builtIns, cookbook, UserPreferences.empty());
        assertTrue(has(pool, "Owsianka"));
        assertTrue(has(pool, "Tost francuski"));
    }

    @Test
    public void dedupesByTitleCaseInsensitive() {
        Cookbook cookbook = Cookbook.empty()
                .add(new CookbookEntry("owsianka", "inny opis", "zapisane"));
        List<Recipe> pool = builder.build(builtIns, cookbook, UserPreferences.empty());
        int count = 0;
        for (Recipe r : pool) {
            if (r.getTitle().equalsIgnoreCase("owsianka")) {
                count++;
            }
        }
        assertTrue(count == 1);
    }

    @Test
    public void excludesDislikedFromPool() {
        Cookbook cookbook = Cookbook.empty()
                .add(new CookbookEntry("Tost francuski", "opis", "zapisane"));
        UserPreferences prefs = UserPreferences.empty().withDislike("jajecznica");
        List<Recipe> pool = builder.build(builtIns, cookbook, prefs);
        assertFalse(has(pool, "Jajecznica"));
        assertTrue(has(pool, "Tost francuski"));
    }

    @Test
    public void neverReturnsEmptyWhenBuiltInsExist() {
        UserPreferences prefs = UserPreferences.empty()
                .withDislike("owsianka").withDislike("jajecznica");
        List<Recipe> pool = builder.build(builtIns, Cookbook.empty(), prefs);
        assertFalse(pool.isEmpty());
    }
}
