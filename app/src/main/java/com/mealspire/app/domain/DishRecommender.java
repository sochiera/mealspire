package com.mealspire.app.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Decyzja „co pokazać" po zalogowaniu: jedno wywołanie LLM (przez backend)
 * ocenia kandydatów względem jawnych reakcji, aplikacja odrzuca naruszenia
 * diety i bierze najwyżej ocenione. Bez logowania, przy błędzie sieci albo złym JSON-ie —
 * gotowa pula offline, bez udawania, że oceniał ją model.
 */
public final class DishRecommender {

    /** Ile najnowszych reakcji (po jednej na danie) trafia do promptu. */
    public static final int MAX_REACTIONS = 30;
    /** Ilu kandydatów model ocenia w jednym wywołaniu. */
    public static final int MAX_CANDIDATES = 12;

    private final DishRater rater;

    public DishRecommender(DishRater rater) {
        this.rater = rater;
    }

    /** Wynik: przepisy do pokazania i (równolegle) powód od modelu, "" gdy offline. */
    public static final class Recommendation {
        private final List<Recipe> recipes;
        private final List<String> reasons;
        private final boolean rated;
        private final boolean failed;

        Recommendation(List<Recipe> recipes, List<String> reasons, boolean rated,
                       boolean failed) {
            this.recipes = Collections.unmodifiableList(recipes);
            this.reasons = Collections.unmodifiableList(reasons);
            this.rated = rated;
            this.failed = failed;
        }

        public List<Recipe> getRecipes() {
            return recipes;
        }

        public List<String> getReasons() {
            return reasons;
        }

        /** Czy choć jedno danie wybrał model. */
        public boolean isRated() {
            return rated;
        }

        /** Czy próbowano ocenić przez LLM i się nie udało (sieć, zły JSON). */
        public boolean isFailed() {
            return failed;
        }
    }

    /**
     * Kandydaci do oceny: pula (już po diecie) w losowej kolejności, z
     * ostatnio pokazanymi odłożonymi na koniec, przycięta do
     * {@link #MAX_CANDIDATES}. Bez profilu gustu — gust ocenia model.
     */
    public static List<Recipe> candidates(List<Recipe> pool, MealHistory history, long now,
                                          Random random) {
        List<Recipe> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        List<Recipe> fresh = new RecentlyShownFilter().apply(shuffled, history,
                OfflineProposalGenerator.RECENCY_WINDOW_MILLIS, now, MAX_CANDIDATES);
        return fresh.size() > MAX_CANDIDATES
                ? new ArrayList<>(fresh.subList(0, MAX_CANDIDATES)) : fresh;
    }

    /**
     * @param signedIn   bez logowania LLM nie jest wołany wcale
     * @param candidates dania do oceny (z przepisami z katalogu)
     * @param diet       twardy filtr stosowany jeszcze raz przed pokazaniem
     * @param offline    gotowy wybór offline na fallback i uzupełnienie
     */
    public Recommendation recommend(boolean signedIn, String mealType, DishReactionLog reactions,
                                    List<Recipe> candidates, DietConstraints diet,
                                    List<Recipe> offline, int count, long now) {
        if (!signedIn || candidates.isEmpty()) {
            return fill(new ArrayList<Recipe>(), new ArrayList<String>(), offline, count,
                    false);
        }
        List<DishRating> ratings;
        try {
            ratings = rater.rate(mealType, reactions.latestPerDish(MAX_REACTIONS),
                    describe(candidates), diet, now);
        } catch (IOException | RuntimeException e) {
            return fill(new ArrayList<Recipe>(), new ArrayList<String>(), offline, count, true);
        }
        Map<String, Recipe> known = byTitle(candidates);
        List<Recipe> recipes = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        for (DishRating rating : topAllowed(ratings, candidates, diet, count)) {
            recipes.add(known.get(rating.getDish().toLowerCase()));
            reasons.add(rating.getReason());
        }
        return fill(recipes, reasons, offline, count, recipes.isEmpty());
    }

    /**
     * Najwyżej ocenione dania spośród kandydatów (nazwy spoza listy i
     * duplikaty odpadają), z odrzuconymi naruszeniami diety.
     */
    static List<DishRating> topAllowed(List<DishRating> ratings, List<Recipe> candidates,
                                       DietConstraints diet, int count) {
        Map<String, Recipe> known = byTitle(candidates);
        List<DishRating> sorted = new ArrayList<>(ratings);
        // Stabilne sortowanie: przy remisie wygrywa kolejność modelu.
        Collections.sort(sorted, new Comparator<DishRating>() {
            @Override
            public int compare(DishRating a, DishRating b) {
                return Integer.compare(b.getScore(), a.getScore());
            }
        });
        List<DishRating> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (DishRating rating : sorted) {
            String key = rating.getDish().toLowerCase();
            Recipe recipe = known.get(key);
            if (result.size() >= count || recipe == null || !used.add(key)) {
                continue;
            }
            if (diet != null && !diet.allows(recipe.getTitle() + "\n" + recipe.getDetails())) {
                continue;
            }
            result.add(new DishRating(recipe.getTitle(), rating.getScore(),
                    rating.getReason()));
        }
        return result;
    }

    /** Kandydat do promptu: tylko nazwa i krótki skład, bez pełnego przepisu. */
    static List<DishProposal> describe(List<Recipe> candidates) {
        List<DishProposal> result = new ArrayList<>();
        for (Recipe recipe : candidates) {
            result.add(new DishProposal(recipe.getTitle(),
                    DishReaction.describe(recipe.getDetails()), "", null));
        }
        return result;
    }

    private static Map<String, Recipe> byTitle(List<Recipe> recipes) {
        Map<String, Recipe> map = new HashMap<>();
        for (Recipe recipe : recipes) {
            map.put(recipe.getTitle().trim().toLowerCase(), recipe);
        }
        return map;
    }

    private static Recommendation fill(List<Recipe> recipes, List<String> reasons,
                                       List<Recipe> offline, int count, boolean failed) {
        boolean rated = !recipes.isEmpty();
        Set<String> used = new HashSet<>();
        for (Recipe recipe : recipes) {
            used.add(recipe.getTitle().toLowerCase());
        }
        for (Recipe recipe : offline) {
            if (recipes.size() >= count) {
                break;
            }
            if (used.add(recipe.getTitle().toLowerCase())) {
                recipes.add(recipe);
                reasons.add("");
            }
        }
        return new Recommendation(recipes, reasons, rated, failed);
    }
}
