package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class OfflineProposalGeneratorTest {

    private static final long HOUR = 60 * 60 * 1000L;
    private static final long DAY = 24 * HOUR;

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
        int poolSize = builtIns.length;
        List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(),
                UserPreferences.empty(), new TasteProfile(Collections.emptyList()),
                poolSize + 10, new Random(2));
        assertTrue("cannot return more than the pool holds", chosen.size() <= poolSize);
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

    @Test
    public void avoidsDishesShownWithinTheRecencyWindow() {
        Recipe[] builtIns = {
                new Recipe("Pizza", "opis"), new Recipe("Sałatka", "opis"), new Recipe("Zupa", "opis")
        };
        long now = 100 * DAY;
        MealHistory history = MealHistory.empty()
                .record("Pizza", now - HOUR)
                .record("Sałatka", now - HOUR);

        List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(), UserPreferences.empty(),
                new TasteProfile(Collections.emptyList()), 1, new Random(1), history, now);

        assertEquals(1, chosen.size());
        assertEquals("just shown today; only the untouched dish should come up",
                "Zupa", chosen.get(0).getTitle());
    }

    @Test
    public void bringsBackADishOnceItsRecencyWindowHasPassed() {
        Recipe[] builtIns = {new Recipe("Pizza", "opis"), new Recipe("Sałatka", "opis")};
        long now = 100 * DAY;
        MealHistory history = MealHistory.empty()
                .record("Pizza", now - OfflineProposalGenerator.RECENCY_WINDOW_MILLIS - HOUR)
                .record("Sałatka", now - HOUR);

        List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(), UserPreferences.empty(),
                new TasteProfile(Collections.emptyList()), 1, new Random(1), history, now);

        assertEquals("Pizza's window passed; it should be suggested again", "Pizza",
                chosen.get(0).getTitle());
    }

    @Test
    public void stillReturnsAFullSetWhenTheWholePoolWasRecentlyShown() {
        // Small pool, every dish shown very recently: the recency filter must not
        // starve the picker down to nothing — repeats are fine here.
        Recipe[] builtIns = {new Recipe("Pizza", "opis"), new Recipe("Sałatka", "opis")};
        long now = 100 * DAY;
        MealHistory history = MealHistory.empty()
                .record("Pizza", now - HOUR)
                .record("Sałatka", now - 2 * HOUR);

        List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(), UserPreferences.empty(),
                new TasteProfile(Collections.emptyList()), 2, new Random(1), history, now);

        assertEquals(2, chosen.size());
    }

    @Test
    public void dailyUseRotatesThroughMostOfTheCatalogueInsteadOfRepeatingTheSameFew() {
        Recipe[] builtIns = BuiltInRecipes.forMeal(0); // 20 bundled breakfast dishes
        MealHistory history = MealHistory.empty();
        Set<String> everShown = new HashSet<>();
        List<Set<String>> dailySets = new ArrayList<>();

        for (int day = 0; day < 10; day++) {
            long now = day * DAY;
            List<Recipe> chosen = generator.generate(builtIns, Cookbook.empty(),
                    UserPreferences.empty(), new TasteProfile(Collections.emptyList()), 3,
                    new Random(day), history, now);

            Set<String> todaysTitles = new HashSet<>();
            for (Recipe recipe : chosen) {
                todaysTitles.add(recipe.getTitle());
                everShown.add(recipe.getTitle());
                history = history.record(recipe.getTitle(), now);
            }
            dailySets.add(todaysTitles);
        }

        assertTrue("ten days of proposals from a 20-dish pool should cover most of it, "
                        + "not loop on the same handful",
                everShown.size() >= 12);

        // Immediate day-to-day repeats should be rare: the very next day's set should
        // not be identical to the day before while fresher dishes are still available.
        for (int day = 1; day < dailySets.size(); day++) {
            assertFalse("day " + day + " repeated the exact same set as the day before",
                    dailySets.get(day).equals(dailySets.get(day - 1)));
        }
    }
}
