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
import static org.junit.Assert.assertTrue;

public class ReadyProposalsTest {

    private static final long NOW = 1_800_000_000_000L;

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

    private static List<String> titles(List<Recipe> recipes) {
        List<String> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            result.add(recipe.getTitle());
        }
        return result;
    }

    @Test
    public void takesReadyDishesInPoolOrderWithReasonsAndRemovesThem() {
        List<Recipe> meal = dishes("A", "B", "C", "D", "E");
        ReadyProposals.Selection selection = ReadyProposals.take(pool("s", "D", "B", "E", "A"), "s",
                meal, DishReactionLog.empty(), new HashSet<String>(), meal, 3);

        assertEquals(Arrays.asList("D", "B", "E"), titles(selection.getRecipes()));
        assertEquals(Arrays.asList("Powód: D", "Powód: B", "Powód: E"), selection.getReasons());
        assertEquals(ReadyProposals.Source.READY, selection.getSource());
        assertEquals(1, selection.getRemaining().size());
        assertEquals("A", selection.getRemaining().getEntries().get(0).getDish());
    }

    @Test
    public void emptyPoolFallsBackToOfflineAtOnceWithExplicitState() {
        List<Recipe> meal = dishes("A", "B", "C");
        ReadyProposals.Selection selection = ReadyProposals.take(ReadyDishPool.empty(), "s", meal,
                DishReactionLog.empty(), new HashSet<String>(), meal, 3);

        assertEquals(Arrays.asList("A", "B", "C"), titles(selection.getRecipes()));
        assertEquals(Arrays.asList("", "", ""), selection.getReasons());
        assertEquals(ReadyProposals.Source.OFFLINE, selection.getSource());
    }

    @Test
    public void shortPoolIsPaddedOfflineAsPartial() {
        List<Recipe> meal = dishes("A", "B", "C", "D");
        ReadyProposals.Selection selection = ReadyProposals.take(pool("s", "C"), "s", meal,
                DishReactionLog.empty(), new HashSet<String>(), meal, 3);

        assertEquals(Arrays.asList("C", "A", "B"), titles(selection.getRecipes()));
        assertEquals(ReadyProposals.Source.PARTIAL, selection.getSource());
    }

    @Test
    public void neverRepeatsWithinSeriesAndEndsExhausted() {
        List<Recipe> meal = dishes("A", "B", "C", "D", "E");
        ReadyDishPool ready = pool("s", "E", "A", "D", "B");
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
            ReadyProposals.Selection selection = ReadyProposals.take(ready, "s", meal,
                    DishReactionLog.empty(), shown, offline, 3);
            ready = selection.getRemaining();
            source = selection.getSource();
            for (Recipe recipe : selection.getRecipes()) {
                seen.add(recipe.getTitle());
                shown.add(recipe.getTitle().toLowerCase());
            }
        } while (source != ReadyProposals.Source.EXHAUSTED);

        assertEquals(5, seen.size());
        assertEquals(5, new HashSet<>(seen).size());
    }

    @Test
    public void skipsShownStaleDislikedAndNoLongerAllowedDishes() {
        // "X" left the catalogue / breaks the diet: not in the meal pool any more.
        // "B" was rated before the "Nie lubię" (stale pool), so it is not served.
        List<Recipe> meal = dishes("A", "B", "C", "D");
        DishReactionLog reactions = DishReactionLog.empty()
                .append(new DishReaction("B", "", false, NOW));
        Set<String> shown = new HashSet<>(Collections.singletonList("c"));
        ReadyProposals.Selection selection = ReadyProposals.take(
                pool("before-dislike", "X", "B", "C", "D", "A"), "after-dislike", meal,
                reactions, shown, Collections.<Recipe>emptyList(), 3);

        assertEquals(Arrays.asList("D", "A"), titles(selection.getRecipes()));
        assertEquals(0, selection.getRemaining().size());
    }

    @Test
    public void dislikeIsNotABanOnceThePoolWasRatedWithIt() {
        List<Recipe> meal = dishes("A", "B");
        DishReactionLog reactions = DishReactionLog.empty()
                .append(new DishReaction("B", "", false, NOW));
        ReadyProposals.Selection selection = ReadyProposals.take(pool("s", "B", "A"), "s",
                meal, reactions, new HashSet<String>(), Collections.<Recipe>emptyList(), 3);

        assertEquals(Arrays.asList("B", "A"), titles(selection.getRecipes()));
    }

    @Test
    public void needsRefillWhenLowOrStale() {
        Set<String> none = new HashSet<>();
        assertTrue(ReadyProposals.needsRefill(ReadyDishPool.empty(), "s", none, 3));
        assertTrue(ReadyProposals.needsRefill(pool("s", "A", "B", "C", "D", "E"), "s", none, 3));
        assertFalse(ReadyProposals.needsRefill(
                pool("s", "A", "B", "C", "D", "E", "F"), "s", none, 3));
        assertTrue(ReadyProposals.needsRefill(
                pool("old", "A", "B", "C", "D", "E", "F"), "s", none, 3));
        assertTrue(ReadyProposals.needsRefill(pool("s", "A", "B", "C", "D", "E", "F"), "s",
                new HashSet<>(Arrays.asList("a")), 3));
    }

    @Test
    public void refillRatesOnlyUnratedUnshownDishesAndKeepsGoodOnes() throws IOException {
        List<Recipe> meal = dishes("A", "B", "C", "D", "E", "F");
        ReadyDishPool current = new ReadyDishPool("s",
                Collections.singletonList(new DishRating("A", 9, "a")),
                Collections.singletonList("B"));
        RecordingRater rater = new RecordingRater(
                new DishRating("D", 9, "świetne"), new DishRating("E", 2, "słabe"),
                new DishRating("Spoza listy", 10, "x"), new DishRating("F", 6, "ok"));

        ReadyProposals.Refill refill = ReadyProposals.refill(rater, "Obiad", "s", current,
                DishReactionLog.empty(), meal, new HashSet<>(Collections.singletonList("c")),
                DietConstraints.empty(), MealHistory.empty(), NOW, new Random(1));

        assertEquals(new HashSet<>(Arrays.asList("D", "E", "F")), new HashSet<>(rater.asked));
        ReadyDishPool merged = refill.applyTo(current, Collections.<String>emptySet());
        List<String> names = new ArrayList<>();
        for (DishRating entry : merged.getEntries()) {
            names.add(entry.getDish());
        }
        assertEquals(Arrays.asList("A", "D", "F"), names);
        assertTrue(merged.getRated().containsAll(Arrays.asList("b", "d", "e", "f")));

        // Everything rated: the next refill does not call the LLM at all.
        RecordingRater idle = new RecordingRater();
        assertTrue(ReadyProposals.refill(idle, "Obiad", "s", merged, DishReactionLog.empty(),
                meal, new HashSet<>(Collections.singletonList("c")), DietConstraints.empty(),
                MealHistory.empty(), NOW, new Random(1)).isEmpty());
        assertEquals(0, idle.calls);
    }

    @Test
    public void staleRefillReplacesPoolAndSkipsDishesShownMeanwhile() throws IOException {
        List<Recipe> meal = dishes("A", "B", "C");
        ReadyDishPool stale = pool("old", "A");
        ReadyProposals.Refill refill = ReadyProposals.refill(
                new RecordingRater(new DishRating("A", 7, "a"), new DishRating("B", 8, "b"),
                        new DishRating("C", 9, "c")),
                "Obiad", "new", stale, DishReactionLog.empty(), meal, new HashSet<String>(),
                DietConstraints.empty(), MealHistory.empty(), NOW, new Random(1));

        ReadyDishPool merged = refill.applyTo(stale, Collections.singletonList("C"));
        assertEquals("new", merged.getSignature());
        List<String> names = new ArrayList<>();
        for (DishRating entry : merged.getEntries()) {
            names.add(entry.getDish());
        }
        assertEquals(Arrays.asList("B", "A"), names);
    }

    @Test
    public void refillDropsDietViolationsEvenIfModelRatesThem() throws IOException {
        List<Recipe> meal = Arrays.asList(new Recipe("Kotlet schabowy", "Składniki: wieprzowina"),
                new Recipe("Sałatka", "Składniki: sałata"));
        DietConstraints vegetarian = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.VEGETARIAN));
        ReadyProposals.Refill refill = ReadyProposals.refill(
                new RecordingRater(new DishRating("Kotlet schabowy", 10, "x"),
                        new DishRating("Sałatka", 7, "y")),
                "Obiad", "s", ReadyDishPool.empty(), DishReactionLog.empty(), meal,
                new HashSet<String>(), vegetarian, MealHistory.empty(), NOW, new Random(1));

        ReadyDishPool merged = refill.applyTo(ReadyDishPool.empty(), Collections.<String>emptySet());
        assertEquals(1, merged.size());
        assertEquals("Sałatka", merged.getEntries().get(0).getDish());
    }

    @Test(expected = IOException.class)
    public void refillPropagatesFailureSoCallerKeepsPool() throws IOException {
        DishRater failing = (mealType, reactions, candidates, diet, now) -> {
            throw new IOException("sieć");
        };
        ReadyProposals.refill(failing, "Obiad", "s", ReadyDishPool.empty(),
                DishReactionLog.empty(), dishes("A"), new HashSet<String>(),
                DietConstraints.empty(), MealHistory.empty(), NOW, new Random(1));
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
}
