package com.mealspire.app.domain;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReadyProposalsTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final RecipeRequest LUNCH = new RecipeRequest("Obiad", UserPreferences.empty(),
            Collections.<String>emptyList(), Collections.<String>emptyList());

    private static List<Recipe> dishes(String... titles) {
        List<Recipe> result = new ArrayList<>();
        for (String title : titles) {
            result.add(new Recipe(title, "Składniki: " + title.toLowerCase()));
        }
        return result;
    }

    private static ReadyDishPool pool(String signature, String... names) {
        List<DishRating> entries = new ArrayList<>();
        for (String name : names) {
            entries.add(new DishRating(name, 8, "Powód: " + name));
        }
        return new ReadyDishPool(signature, entries, Collections.<String>emptyList());
    }

    private static DishProposal idea(String name, String... ingredients) {
        return new DishProposal(name, "Nowy pomysł: " + name, "30 min", Arrays.asList(ingredients));
    }

    private static List<String> titles(ReadyProposals.Selection selection) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < selection.size(); i++) {
            Recipe recipe = selection.getRecipes().get(i);
            result.add(recipe != null ? recipe.getTitle()
                    : selection.getGenerated().get(i).getName());
        }
        return result;
    }

    private static List<String> names(ReadyDishPool pool) {
        List<String> result = new ArrayList<>();
        for (DishRating entry : pool.getEntries()) {
            result.add(entry.getDish());
        }
        return result;
    }

    private static ReadyProposals.Selection take(ReadyDishPool pool, String signature,
                                                 List<Recipe> meal, DishReactionLog reactions,
                                                 Set<String> shown, List<Recipe> offline) {
        return ReadyProposals.take(pool, signature, meal, DietConstraints.empty(), reactions,
                shown, offline, 3);
    }

    private static ReadyProposals.Refill refill(DishRater rater, RecipeOperations generator,
                                                String signature, ReadyDishPool pool,
                                                List<Recipe> meal, Set<String> shown,
                                                DietConstraints diet) throws IOException {
        return ReadyProposals.refill(rater, generator, LUNCH, signature, pool,
                DishReactionLog.empty(), meal, shown, diet, MealHistory.empty(), NOW,
                new Random(1));
    }

    // ----- take: instant, no network ------------------------------------------

    @Test
    public void takesReadyDishesInPoolOrderWithReasonsAndRemovesThem() {
        List<Recipe> meal = dishes("A", "B", "C", "D", "E");
        ReadyProposals.Selection selection = take(pool("s", "D", "B", "E", "A"), "s", meal,
                DishReactionLog.empty(), new HashSet<String>(), meal);

        assertEquals(Arrays.asList("D", "B", "E"), titles(selection));
        assertEquals(Arrays.asList("Powód: D", "Powód: B", "Powód: E"), selection.getReasons());
        assertEquals(ReadyProposals.Source.READY, selection.getSource());
        assertEquals(Collections.singletonList("A"), names(selection.getRemaining()));
    }

    @Test
    public void emptyPoolFallsBackToOfflineAtOnceWithExplicitState() {
        List<Recipe> meal = dishes("A", "B", "C");
        ReadyProposals.Selection selection = take(ReadyDishPool.empty(), "s", meal,
                DishReactionLog.empty(), new HashSet<String>(), meal);

        assertEquals(Arrays.asList("A", "B", "C"), titles(selection));
        assertEquals(Arrays.asList("", "", ""), selection.getReasons());
        assertEquals(ReadyProposals.Source.OFFLINE, selection.getSource());
    }

    @Test
    public void shortPoolIsPaddedOfflineAsPartial() {
        List<Recipe> meal = dishes("A", "B", "C", "D");
        ReadyProposals.Selection selection = take(pool("s", "C"), "s", meal,
                DishReactionLog.empty(), new HashSet<String>(), meal);

        assertEquals(Arrays.asList("C", "A", "B"), titles(selection));
        assertEquals(ReadyProposals.Source.PARTIAL, selection.getSource());
    }

    @Test
    public void servesGeneratedDishesWithoutRecipeAndRespectsDiet() {
        List<Recipe> meal = dishes("A");
        DishProposal curry = idea("Curry z ciecierzycą", "ciecierzyca");
        DishProposal pork = idea("Schab pieczony", "wieprzowina");
        ReadyDishPool ready = new ReadyDishPool("s",
                Arrays.asList(new DishRating("Schab pieczony", 5, ""),
                        new DishRating("Curry z ciecierzycą", 5, ""), new DishRating("A", 8, "a")),
                Collections.<String>emptyList(), Arrays.asList(curry, pork));
        DietConstraints vegetarian = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.VEGETARIAN));

        ReadyProposals.Selection selection = ReadyProposals.take(ready, "s", meal, vegetarian,
                DishReactionLog.empty(), new HashSet<String>(), Collections.<Recipe>emptyList(), 3);

        assertEquals(Arrays.asList("Curry z ciecierzycą", "A"), titles(selection));
        assertNull("new dish: recipe on demand", selection.getRecipes().get(0));
        assertSame(curry, selection.getGenerated().get(0));
        assertNull(selection.getGenerated().get(1));
        assertEquals(0, selection.getRemaining().size());
    }

    @Test
    public void neverRepeatsWithinSeriesAndEndsExhausted() {
        List<Recipe> meal = dishes("A", "B", "C", "D", "E");
        ReadyDishPool ready = new ReadyDishPool("s",
                Arrays.asList(new DishRating("E", 8, ""), new DishRating("Nowe", 5, ""),
                        new DishRating("A", 8, ""), new DishRating("D", 8, ""),
                        new DishRating("B", 8, "")),
                Collections.<String>emptyList(), Collections.singletonList(idea("Nowe")));
        Set<String> shown = new HashSet<>();
        List<String> seen = new ArrayList<>();
        ReadyProposals.Source source;
        do {
            List<Recipe> offline = new ArrayList<>();
            for (Recipe recipe : meal) {
                if (!shown.contains(recipe.getTitle().toLowerCase())) {
                    offline.add(recipe);
                }
            }
            ReadyProposals.Selection selection = take(ready, "s", meal,
                    DishReactionLog.empty(), shown, offline);
            ready = selection.getRemaining();
            source = selection.getSource();
            for (String title : titles(selection)) {
                seen.add(title);
                shown.add(title.toLowerCase());
            }
        } while (source != ReadyProposals.Source.EXHAUSTED);

        assertEquals(6, seen.size());
        assertEquals(6, new HashSet<>(seen).size());
    }

    @Test
    public void skipsShownStaleDislikedAndNoLongerAllowedDishes() {
        // "X" left the catalogue / breaks the diet: not in the meal pool any more.
        // "B" was rated before the "Nie lubię" (stale pool), so it is not served.
        List<Recipe> meal = dishes("A", "B", "C", "D");
        DishReactionLog reactions = DishReactionLog.empty()
                .append(new DishReaction("B", "", false, NOW));
        Set<String> shown = new HashSet<>(Collections.singletonList("c"));
        ReadyProposals.Selection selection = take(pool("before-dislike", "X", "B", "C", "D", "A"),
                "after-dislike", meal, reactions, shown, Collections.<Recipe>emptyList());

        assertEquals(Arrays.asList("D", "A"), titles(selection));
        assertEquals(0, selection.getRemaining().size());
    }

    @Test
    public void dislikeIsNotABanOnceThePoolWasRatedWithIt() {
        List<Recipe> meal = dishes("A", "B");
        DishReactionLog reactions = DishReactionLog.empty()
                .append(new DishReaction("B", "", false, NOW));
        ReadyProposals.Selection selection = take(pool("s", "B", "A"), "s", meal, reactions,
                new HashSet<String>(), Collections.<Recipe>emptyList());

        assertEquals(Arrays.asList("B", "A"), titles(selection));
    }

    // ----- when to refill: before the pool runs out ---------------------------

    @Test
    public void needsRefillWhenEmptyLowOrStaleBeforeItRunsOut() {
        Set<String> none = new HashSet<>();
        assertTrue("empty pool", ReadyProposals.needsRefill(ReadyDishPool.empty(), "s", none, 3));
        assertTrue("fewer than two sets left",
                ReadyProposals.needsRefill(pool("s", "A", "B", "C", "D", "E"), "s", none, 3));
        assertFalse(ReadyProposals.needsRefill(
                pool("s", "A", "B", "C", "D", "E", "F"), "s", none, 3));
        assertTrue("taste changed", ReadyProposals.needsRefill(
                pool("old", "A", "B", "C", "D", "E", "F"), "s", none, 3));
        assertTrue(ReadyProposals.needsRefill(pool("s", "A", "B", "C", "D", "E", "F"), "s",
                new HashSet<>(Arrays.asList("a")), 3));
    }

    @Test
    public void avoidListCoversPoolSeriesAndRecentOnceAndIsCapped() {
        List<String> avoid = ReadyProposals.avoidList(pool("s", "A", "B"),
                Arrays.asList("c", "a"), Arrays.asList("D", "B"));
        assertEquals(Arrays.asList("A", "B", "c", "D"), avoid);

        List<String> many = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            many.add("Danie " + i);
        }
        assertEquals(ReadyProposals.MAX_AVOID, ReadyProposals.avoidList(ReadyDishPool.empty(),
                Collections.<String>emptyList(), many).size());
    }

    // ----- refill: LLM generates new dishes (and rates unrated catalogue ones) -----

    @Test
    public void refillOfEmptyPoolGeneratesNewDishesWhenCatalogueIsAllRated() throws IOException {
        List<Recipe> meal = dishes("A", "B");
        ReadyDishPool allRated = new ReadyDishPool("s", Collections.<DishRating>emptyList(),
                Arrays.asList("A", "B"));
        RecordingRater rater = new RecordingRater();
        RecordingGenerator generator = new RecordingGenerator(idea("Shakshuka", "jajka"),
                idea("Leczo", "papryka"));

        ReadyProposals.Refill refill = refill(rater, generator, "s", allRated, meal,
                new HashSet<String>(), DietConstraints.empty());

        assertEquals("no catalogue dish left to rate", 0, rater.calls);
        assertEquals(1, generator.calls);
        assertEquals(ReadyProposals.GENERATE_PER_REFILL, generator.lastCount);
        assertEquals(2, refill.generatedCount());
        ReadyDishPool merged = refill.applyTo(allRated, Collections.<String>emptySet());
        assertEquals(Arrays.asList("Shakshuka", "Leczo"), names(merged));
        assertEquals("Nowy pomysł: Leczo", merged.generated("leczo").getDescription());
    }

    @Test
    public void refillRatesUnratedCatalogueAndGeneratesSkippingKnownShownAndDiet()
            throws IOException {
        List<Recipe> meal = dishes("A", "B", "C", "D", "E", "F");
        ReadyDishPool current = new ReadyDishPool("s",
                Collections.singletonList(new DishRating("A", 9, "a")),
                Collections.singletonList("B"));
        RecordingRater rater = new RecordingRater(
                new DishRating("D", 9, "świetne"), new DishRating("E", 2, "słabe"),
                new DishRating("Spoza listy", 10, "x"), new DishRating("F", 6, "ok"));
        RecordingGenerator generator = new RecordingGenerator(
                idea("A"), idea("C"), idea("D"), idea("Kotlet schabowy", "wieprzowina"),
                idea("E"), idea("Pierogi z kapustą", "kapusta"));
        DietConstraints vegetarian = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.VEGETARIAN));

        ReadyProposals.Refill refill = refill(rater, generator, "s", current, meal,
                new HashSet<>(Collections.singletonList("c")), vegetarian);

        assertEquals(new HashSet<>(Arrays.asList("D", "E", "F")), new HashSet<>(rater.asked));
        ReadyDishPool merged = refill.applyTo(current, Collections.<String>emptySet());
        // A: already in pool; C: shown in series; D: just rated; schabowy: diet;
        // E: just rated too low — generation is no back door for it.
        assertEquals(Arrays.asList("A", "D", "F", "Pierogi z kapustą"), names(merged));
        assertNull("catalogue dish keeps its recipe", merged.generated("d"));
        assertEquals(1, refill.generatedCount());
        assertTrue(merged.getRated().containsAll(Arrays.asList("b", "d", "e", "f")));
    }

    @Test
    public void staleRefillReplacesPoolAndSkipsDishesShownMeanwhile() throws IOException {
        List<Recipe> meal = dishes("A", "B", "C");
        ReadyDishPool stale = pool("old", "A");
        ReadyProposals.Refill refill = refill(
                new RecordingRater(new DishRating("A", 7, "a"), new DishRating("B", 8, "b"),
                        new DishRating("C", 9, "c")),
                new RecordingGenerator(), "new", stale, meal, new HashSet<String>(),
                DietConstraints.empty());

        ReadyDishPool merged = refill.applyTo(stale, Collections.singletonList("C"));
        assertEquals("new", merged.getSignature());
        assertEquals(Arrays.asList("B", "A"), names(merged));
    }

    @Test
    public void ratingFailureStillKeepsGeneratedDishes() throws IOException {
        DishRater failing = (mealType, reactions, candidates, diet, now) -> {
            throw new IOException("sieć");
        };
        ReadyProposals.Refill refill = refill(failing, new RecordingGenerator(idea("Leczo")), "s",
                ReadyDishPool.empty(), dishes("A"), new HashSet<String>(),
                DietConstraints.empty());

        assertEquals(Collections.singletonList("Leczo"),
                names(refill.applyTo(ReadyDishPool.empty(), Collections.<String>emptySet())));
    }

    @Test(expected = IOException.class)
    public void refillPropagatesFailureWhenNothingWorkedSoCallerKeepsPool() throws IOException {
        DishRater failing = (mealType, reactions, candidates, diet, now) -> {
            throw new IOException("sieć");
        };
        RecordingGenerator generator = new RecordingGenerator();
        generator.failure = new IOException("sieć");
        refill(failing, generator, "s", ReadyDishPool.empty(), dishes("A"),
                new HashSet<String>(), DietConstraints.empty());
    }

    private static final class RecordingRater implements DishRater {
        final List<DishRating> answer;
        final List<String> asked = new ArrayList<>();
        int calls;

        RecordingRater(DishRating... answer) {
            this.answer = Arrays.asList(answer);
        }

        @Override
        public List<DishRating> rate(String mealType, List<DishReaction> reactions,
                                     List<DishProposal> candidates, DietConstraints diet,
                                     long now) {
            calls++;
            for (DishProposal candidate : candidates) {
                asked.add(candidate.getName());
            }
            return answer;
        }
    }

    private static final class RecordingGenerator implements RecipeOperations {
        final List<DishProposal> answer;
        IOException failure;
        int calls;
        int lastCount;

        RecordingGenerator(DishProposal... answer) {
            this.answer = Arrays.asList(answer);
        }

        @Override
        public List<DishProposal> proposeDishes(RecipeRequest request, int count)
                throws IOException {
            calls++;
            lastCount = count;
            if (failure != null) {
                throw failure;
            }
            return answer;
        }

        @Override
        public Recipe generateRecipeFor(String dishName, RecipeRequest request) {
            throw new AssertionError("refill never fetches full recipes");
        }

        @Override
        public Recipe modifyRecipe(Recipe current, String instruction, HouseholdProfile profile) {
            throw new AssertionError("not used");
        }
    }
}
