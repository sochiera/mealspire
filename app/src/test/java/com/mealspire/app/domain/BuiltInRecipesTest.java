package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuiltInRecipesTest {

    // The everyday breakfast staples explicitly requested for the bundled catalogue.
    private static final List<String> EXPECTED_BREAKFAST_DISHES = Arrays.asList(
            "Jajecznica", "Jajka sadzone", "Omlet", "Owsianka z dodatkami", "Tosty",
            "Grzanki z serem", "Płatki z mlekiem", "Pancakes", "Naleśniki",
            "Placki z twarożku");

    @Test
    public void hasThreeMeals() {
        assertEquals(3, BuiltInRecipes.mealCount());
    }

    @Test
    public void catalogueIsLargeEnoughForRealDailyVariety() {
        // A handful of dishes repeats within days; the bundled catalogue needs to be
        // large enough that offline users still see fresh sets for weeks.
        for (int i = 0; i < BuiltInRecipes.mealCount(); i++) {
            assertTrue("meal " + i + " should have at least 18 bundled dishes",
                    BuiltInRecipes.forMeal(i).length >= 18);
        }
    }

    @Test
    public void breakfastContainsAllRequestedEverydayDishes() {
        Set<String> titles = new HashSet<>();
        for (Recipe recipe : BuiltInRecipes.forMeal(0)) {
            titles.add(recipe.getTitle());
        }
        for (String expected : EXPECTED_BREAKFAST_DISHES) {
            assertTrue("breakfast should include: " + expected, titles.contains(expected));
        }
    }

    @Test
    public void oatmealOffersExtrasAsAnOptionNotTheBase() {
        Recipe oatmeal = findByTitle(BuiltInRecipes.forMeal(0), "Owsianka z dodatkami");
        String details = oatmeal.getDetails().toLowerCase();
        assertTrue("extras should be marked optional, not mandatory",
                details.contains("dodatki") && details.contains("opcjonalnie"));
    }

    @Test
    public void everyDishTitleIsUnique() {
        Set<String> titles = new HashSet<>();
        int total = 0;
        for (int i = 0; i < BuiltInRecipes.mealCount(); i++) {
            for (Recipe recipe : BuiltInRecipes.forMeal(i)) {
                titles.add(recipe.getTitle());
                total++;
            }
        }
        assertEquals("no duplicate titles across meals", total, titles.size());
    }

    @Test
    public void detailsByTitleCoversEveryBuiltInDish() {
        int total = 0;
        for (int i = 0; i < BuiltInRecipes.mealCount(); i++) {
            total += BuiltInRecipes.forMeal(i).length;
        }
        Map<String, String> details = BuiltInRecipes.detailsByTitle(null);
        assertEquals(total, details.size());
        assertTrue(details.containsKey("Kurczak z kaszą"));
    }

    @Test
    public void detailsByTitleMergesCookbook() {
        int before = BuiltInRecipes.detailsByTitle(null).size();
        Cookbook cookbook = Cookbook.empty().add(
                new CookbookEntry("Pierogi", "Składniki: mąka, ziemniaki.", "lubiane"));
        Map<String, String> details = BuiltInRecipes.detailsByTitle(cookbook);
        assertTrue(details.containsKey("Pierogi"));
        assertEquals(before + 1, details.size());
    }

    private static Recipe findByTitle(Recipe[] recipes, String title) {
        for (Recipe recipe : recipes) {
            if (recipe.getTitle().equals(title)) {
                return recipe;
            }
        }
        throw new AssertionError("not found: " + title);
    }
}
