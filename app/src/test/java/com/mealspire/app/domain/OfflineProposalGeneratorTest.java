package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class OfflineProposalGeneratorTest {

    private final OfflineProposalGenerator generator = new OfflineProposalGenerator();

    @Test
    public void picksRequestedCountOfDistinctBuiltInDishes() {
        Recipe[] builtIns = BuiltInRecipes.forMeal(1);
        List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(),
                UserPreferences.empty(), new TasteProfile(Collections.emptyList()), 3, new Random(1));

        assertEquals(3, chosen.size());
        Set<String> titles = new HashSet<>();
        for (Recipe recipe : chosen) {
            titles.add(recipe.getTitle());
        }
        assertEquals("dishes should be distinct", 3, titles.size());
    }

    @Test
    public void neverExceedsAvailablePool() {
        Recipe[] builtIns = BuiltInRecipes.forMeal(0);
        List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(),
                UserPreferences.empty(), new TasteProfile(Collections.emptyList()), 10, new Random(2));
        assertTrue("cannot return more than the pool holds", chosen.size() <= 3);
    }

    @Test
    public void includesCookbookDishesInPool() {
        Cookbook cookbook = Cookbook.empty().add(
                new CookbookEntry("Pierogi ruskie", "Składniki: mąka, twaróg, ziemniaki.", "lubiane"));
        Recipe[] builtIns = BuiltInRecipes.forMeal(2);
        boolean sawCookbookDish = false;
        // Across several seeds the cookbook dish should surface at least once.
        for (int seed = 0; seed < 20 && !sawCookbookDish; seed++) {
            List<Recipe> chosen = generator.generate(builtIns, cookbook,
                    UserPreferences.empty(), new TasteProfile(Collections.emptyList()), 3, new Random(seed));
            for (Recipe recipe : chosen) {
                if (recipe.getTitle().equals("Pierogi ruskie")) {
                    sawCookbookDish = true;
                }
            }
        }
        assertTrue("cookbook dishes belong in the offline pool", sawCookbookDish);
    }
}
