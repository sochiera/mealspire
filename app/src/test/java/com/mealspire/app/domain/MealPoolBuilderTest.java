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

    @Test
    public void dietFiltersPoolByTitleAndDetails() {
        Recipe[] mixed = {
                new Recipe("Kotlet schabowy", "Składniki: schab, bułka tarta."),
                new Recipe("Zapiekanka", "Składniki: ziemniaki, mięso mielone."),
                new Recipe("Ryż z warzywami", "Składniki: ryż, marchew, papryka.")
        };
        DietConstraints veg = DietConstraints.of(java.util.Arrays.asList(
                DietConstraints.Exclusion.VEGETARIAN));

        List<Recipe> pool = builder.build(mixed, Cookbook.empty(),
                UserPreferences.empty(), veg);

        assertTrue(has(pool, "Ryż z warzywami"));
        assertFalse(has(pool, "Kotlet schabowy"));
        assertFalse("mięso w składnikach też wyklucza", has(pool, "Zapiekanka"));
    }

    @Test
    public void dietFilterIsAbsoluteEvenWhenItEmptiesThePool() {
        // W odróżnieniu od niechcianych dań dieta nie ma fallbacku — pusta
        // lista jest lepsza niż wieprzowina u wegetarianina.
        Recipe[] meatOnly = {new Recipe("Schabowy", "Składniki: schab.")};
        DietConstraints veg = DietConstraints.of(java.util.Arrays.asList(
                DietConstraints.Exclusion.VEGETARIAN));

        List<Recipe> pool = builder.build(meatOnly, Cookbook.empty(),
                UserPreferences.empty(), veg);

        assertTrue(pool.isEmpty());
    }
}
