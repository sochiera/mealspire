package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public class BuiltInRecipesTest {

    @Test
    public void hasThreeMealsWithThreeDishesEach() {
        assertEquals(3, BuiltInRecipes.mealCount());
        for (int i = 0; i < BuiltInRecipes.mealCount(); i++) {
            assertEquals("meal " + i + " should have 3 dishes",
                    3, BuiltInRecipes.forMeal(i).length);
        }
    }

    @Test
    public void breakfastContainsKnownDish() {
        Recipe[] breakfast = BuiltInRecipes.forMeal(0);
        boolean found = false;
        for (Recipe recipe : breakfast) {
            if (recipe.getTitle().equals("Owsianka z jabłkiem")) {
                found = true;
            }
        }
        assertTrue("breakfast should include the porridge", found);
    }

    @Test
    public void detailsByTitleCoversEveryBuiltInDish() {
        Map<String, String> details = BuiltInRecipes.detailsByTitle(null);
        assertEquals(9, details.size());
        assertTrue(details.containsKey("Kurczak z kaszą"));
    }

    @Test
    public void detailsByTitleMergesCookbook() {
        Cookbook cookbook = Cookbook.empty().add(
                new CookbookEntry("Pierogi", "Składniki: mąka, ziemniaki.", "lubiane"));
        Map<String, String> details = BuiltInRecipes.detailsByTitle(cookbook);
        assertTrue(details.containsKey("Pierogi"));
        assertEquals(10, details.size());
    }
}
