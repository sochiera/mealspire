package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DishRecommenderTest {

    private static final long NOW = 1_000_000_000L;

    /** Counts calls and returns a canned answer (or throws, like no network). */
    private static class FakeLlm implements LlmClient {
        int calls;
        String lastUser;
        String answer;
        IOException failure;

        @Override
        public String complete(String systemPrompt, String userPrompt) throws IOException {
            calls++;
            lastUser = userPrompt;
            if (failure != null) {
                throw failure;
            }
            return answer;
        }
    }

    private final FakeLlm llm = new FakeLlm();
    private final DishRecommender recommender = new DishRecommender(llm,
            new DishRatingPromptBuilder(), new DishRatingParser());

    private final List<Recipe> candidates = Arrays.asList(
            new Recipe("Pierogi ruskie", "Składniki: ziemniaki, twaróg, cebula\n\n1. Ulep."),
            new Recipe("Schabowy", "Składniki: schab wieprzowy, jajko, bułka tarta\n\n1. Smaż."),
            new Recipe("Zupa pomidorowa", "Składniki: pomidory, makaron\n\n1. Gotuj."),
            new Recipe("Omlet", "Składniki: jajka, szczypiorek\n\n1. Ubij."));
    private final List<Recipe> offline = Arrays.asList(
            new Recipe("Owsianka", "Składniki: płatki owsiane, mleko"),
            new Recipe("Kanapki", "Składniki: chleb, ser"),
            new Recipe("Jajecznica", "Składniki: jajka, masło"));

    private static String ratings(String... items) {
        return "{\"oceny\":[" + String.join(",", items) + "]}";
    }

    private static String rating(String dish, int score, String reason) {
        return "{\"danie\":\"" + dish + "\",\"ocena\":" + score + ",\"powod\":\"" + reason + "\"}";
    }

    private DishRecommender.Recommendation recommend(boolean signedIn, DietConstraints diet) {
        DishReactionLog reactions = DishReactionLog.empty()
                .append(new DishReaction("Placki ziemniaczane", "ziemniaki", true, NOW));
        return recommender.recommend(signedIn, "Obiad", reactions, candidates, diet, offline,
                3, NOW);
    }

    @Test
    public void signedOutNeverCallsLlmAndUsesOfflinePool() {
        llm.answer = ratings(rating("Pierogi ruskie", 10, "x"));

        DishRecommender.Recommendation result = recommend(false, DietConstraints.empty());

        assertEquals(0, llm.calls);
        assertEquals("Owsianka", result.getRecipes().get(0).getTitle());
        assertEquals(3, result.getRecipes().size());
        assertEquals(Arrays.asList("", "", ""), result.getReasons());
        assertFalse(result.isRated());
        assertFalse(result.isFailed());
    }

    @Test
    public void signedInMakesOneCallAndShowsTopThreeWithReasons() {
        llm.answer = ratings(rating("Omlet", 4, "Mało podobny."),
                rating("Pierogi ruskie", 9, "Lubi ziemniaczane dania."),
                rating("Zupa pomidorowa", 6, "Prosta i domowa."),
                rating("Schabowy", 7, "Smażone jak placki."));

        DishRecommender.Recommendation result = recommend(true, DietConstraints.empty());

        assertEquals(1, llm.calls);
        assertTrue(result.isRated());
        assertEquals("Pierogi ruskie", result.getRecipes().get(0).getTitle());
        assertEquals("Schabowy", result.getRecipes().get(1).getTitle());
        assertEquals("Zupa pomidorowa", result.getRecipes().get(2).getTitle());
        assertEquals("Lubi ziemniaczane dania.", result.getReasons().get(0));
        assertTrue(llm.lastUser.contains("lubi: Placki ziemniaczane"));
        assertTrue(llm.lastUser.contains("Omlet"));
    }

    @Test
    public void dietViolationIsDroppedBeforeShowingEvenIfRatedHighest() {
        llm.answer = ratings(rating("Schabowy", 10, "Mięsny."),
                rating("Pierogi ruskie", 8, "a"), rating("Omlet", 5, "b"),
                rating("Zupa pomidorowa", 2, "c"));
        DietConstraints noPork = DietConstraints.of(
                Collections.singletonList(DietConstraints.Exclusion.NO_PORK));

        DishRecommender.Recommendation result = recommend(true, noPork);

        List<String> titles = new ArrayList<>();
        for (Recipe recipe : result.getRecipes()) {
            titles.add(recipe.getTitle());
        }
        assertEquals(Arrays.asList("Pierogi ruskie", "Omlet", "Zupa pomidorowa"), titles);
    }

    @Test
    public void unknownAndDuplicateDishesAreIgnoredAndGapFilledOffline() {
        llm.answer = ratings(rating("Sushi", 10, "Spoza listy."),
                rating("omlet", 8, "Jajka."), rating("Omlet", 7, "Duplikat."));

        DishRecommender.Recommendation result = recommend(true, DietConstraints.empty());

        assertEquals(3, result.getRecipes().size());
        assertEquals("Omlet", result.getRecipes().get(0).getTitle());
        assertEquals("Jajka.", result.getReasons().get(0));
        assertEquals("Owsianka", result.getRecipes().get(1).getTitle());
        assertEquals("", result.getReasons().get(1));
        assertTrue(result.isRated());
    }

    @Test
    public void badJsonFallsBackOfflineWithoutCrash() {
        llm.answer = "Przepraszam, nie mogę.";

        DishRecommender.Recommendation result = recommend(true, DietConstraints.empty());

        assertEquals(1, llm.calls);
        assertTrue(result.isFailed());
        assertFalse(result.isRated());
        assertEquals("Owsianka", result.getRecipes().get(0).getTitle());
    }

    @Test
    public void networkErrorFallsBackOffline() {
        llm.failure = new IOException("Brak połączenia");

        DishRecommender.Recommendation result = recommend(true, DietConstraints.empty());

        assertTrue(result.isFailed());
        assertEquals(3, result.getRecipes().size());
        assertEquals("Owsianka", result.getRecipes().get(0).getTitle());
    }

    @Test
    public void emptyReactionsAskModelToRateByPopularity() {
        llm.answer = ratings(rating("Pierogi ruskie", 9, "Klasyka."));

        recommender.recommend(true, "Obiad", DishReactionLog.empty(), candidates,
                DietConstraints.empty(), offline, 3, NOW);

        assertEquals(1, llm.calls);
        assertTrue(llm.lastUser.contains("Brak reakcji"));
        assertTrue(llm.lastUser.contains("popularne"));
    }

    @Test
    public void promptListsNewestReactionsFirstWithAge() {
        llm.answer = ratings(rating("Omlet", 5, "x"));
        long day = 24L * 60 * 60 * 1000;
        DishReactionLog reactions = DishReactionLog.empty()
                .append(new DishReaction("Flaki", "flaki wołowe", false, NOW - 10 * day))
                .append(new DishReaction("Naleśniki", "", true, NOW));

        recommender.recommend(true, "Obiad", reactions, candidates, DietConstraints.empty(),
                offline, 3, NOW);

        int newer = llm.lastUser.indexOf("lubi: Naleśniki — dziś");
        int older = llm.lastUser.indexOf("nie lubi: Flaki (flaki wołowe) — 10 dni temu");
        assertTrue(newer >= 0);
        assertTrue(older > newer);
    }

    @Test
    public void candidatesAreCappedAndPreferNotRecentlyShown() {
        List<Recipe> pool = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            pool.add(new Recipe("Danie " + i, ""));
        }
        MealHistory history = MealHistory.empty().record("Danie 0", NOW);

        List<Recipe> picked = DishRecommender.candidates(pool, history, NOW, new Random(1));

        assertEquals(DishRecommender.MAX_CANDIDATES, picked.size());
        for (Recipe recipe : picked) {
            assertFalse(recipe.getTitle().equals("Danie 0"));
        }
    }
}
