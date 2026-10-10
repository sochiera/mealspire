package com.mealspire.app.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Propozycje bez czekania na LLM: {@link #take} natychmiast bierze dania z
 * gotowej puli ({@link ReadyDishPool}), braki dopełnia pula offline, a
 * {@link #refill} — wołane w tle — ocenia kolejnych kandydatów jednym
 * wywołaniem {@link DishRater}. W jednej serii (kolejne „Inne propozycje"
 * dla tego samego posiłku) żadne danie nie wraca; gdy brak już nowych dań,
 * wynik jest krótszy albo pusty — nigdy powtórka.
 */
public final class ReadyProposals {

    /** Uzupełniaj, gdy w puli zostało mniej niż tyle zestawów. */
    public static final int LOW_WATER_SETS = 2;
    /** Dania ocenione niżej nie trafiają do puli (jak dawniej: pokazywano tylko czołówkę). */
    public static final int MIN_SCORE = 5;
    /** Z jednej oceny trafia do puli najwyżej tyle najlepszych dań. */
    public static final int KEEP_PER_REFILL = 6;

    /** Stan widoczny dla użytkownika. */
    public enum Source {
        /** Wszystkie propozycje z gotowej puli. */
        READY,
        /** Część z puli, reszta offline — pula się uzupełnia. */
        PARTIAL,
        /** Pula pusta — same propozycje offline, pula uzupełnia się w tle. */
        OFFLINE,
        /** W tej serii pokazano już wszystkie pasujące dania. */
        EXHAUSTED
    }

    /** Wynik {@link #take}: propozycje, powody ("" offline) i pula po zdjęciu pokazanych. */
    public static final class Selection {
        private final List<Recipe> recipes;
        private final List<String> reasons;
        private final ReadyDishPool remaining;
        private final Source source;

        Selection(List<Recipe> recipes, List<String> reasons, ReadyDishPool remaining,
                  Source source) {
            this.recipes = Collections.unmodifiableList(recipes);
            this.reasons = Collections.unmodifiableList(reasons);
            this.remaining = remaining;
            this.source = source;
        }

        public List<Recipe> getRecipes() {
            return recipes;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public ReadyDishPool getRemaining() {
            return remaining;
        }

        public Source getSource() {
            return source;
        }
    }

    /** Wynik {@link #refill}: nowe dania do puli i wszystkie ocenione nazwy. */
    public static final class Refill {
        private final String signature;
        private final List<DishRating> fresh;
        private final List<String> rated;

        Refill(String signature, List<DishRating> fresh, List<String> rated) {
            this.signature = signature;
            this.fresh = Collections.unmodifiableList(fresh);
            this.rated = Collections.unmodifiableList(rated);
        }

        public boolean isEmpty() {
            return rated.isEmpty();
        }

        /** Dołącza wynik do aktualnej (być może już uszczuplonej) puli. */
        public ReadyDishPool applyTo(ReadyDishPool current, Collection<String> shown) {
            return current.merge(signature, fresh, rated, shown);
        }
    }

    private ReadyProposals() {
    }

    /**
     * Natychmiastowy wybór — bez sieci.
     *
     * @param mealPool dania posiłku już po diecie (katalog + książka kucharska);
     *                 wpisy puli spoza niej (usunięte, łamiące dietę) odpadają
     * @param signature aktualny podpis gustu; z puli ocenionej przed zmianą
     *                  reakcji pomijamy dania, których teraz „nie lubię"
     *                  (aktualna pula oceniła je już z tą reakcją — to nie zakaz)
     * @param shown    dania pokazane już w tej serii — nie wracają
     * @param offline  kolejność zapasowa (pipeline offline) na braki
     */
    public static Selection take(ReadyDishPool pool, String signature, List<Recipe> mealPool,
                                 DishReactionLog reactions, Set<String> shown,
                                 List<Recipe> offline, int count) {
        Map<String, Recipe> known = byKey(mealPool);
        Set<String> disliked = pool.isCurrent(signature)
                ? Collections.<String>emptySet() : latestDislikes(reactions);
        Set<String> used = new HashSet<>();
        for (String name : shown) {
            used.add(ReadyDishPool.key(name));
        }
        List<Recipe> recipes = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<String> drop = new ArrayList<>();
        for (DishRating entry : pool.getEntries()) {
            String key = ReadyDishPool.key(entry.getDish());
            Recipe recipe = known.get(key);
            if (recipe == null || disliked.contains(key) || used.contains(key)) {
                drop.add(entry.getDish());
                continue;
            }
            if (recipes.size() >= count) {
                break;
            }
            used.add(key);
            recipes.add(recipe);
            reasons.add(entry.getReason());
            drop.add(entry.getDish());
        }
        int ready = recipes.size();
        for (Recipe recipe : offline) {
            if (recipes.size() >= count) {
                break;
            }
            if (known.containsKey(ReadyDishPool.key(recipe.getTitle()))
                    && used.add(ReadyDishPool.key(recipe.getTitle()))) {
                recipes.add(recipe);
                reasons.add("");
            }
        }
        Source source = recipes.isEmpty() ? Source.EXHAUSTED
                : ready == recipes.size() ? Source.READY
                : ready > 0 ? Source.PARTIAL : Source.OFFLINE;
        return new Selection(recipes, reasons, pool.without(drop), source);
    }

    /** Czy pula wymaga uzupełnienia w tle (nieaktualny gust albo mało dań). */
    public static boolean needsRefill(ReadyDishPool pool, String signature, Set<String> shown,
                                      int count) {
        Set<String> keys = new HashSet<>();
        for (String name : shown) {
            keys.add(ReadyDishPool.key(name));
        }
        return !pool.isCurrent(signature) || pool.available(keys) < LOW_WATER_SETS * count;
    }

    /**
     * Jedno wywołanie LLM (blokujące — tylko w tle): ocenia do
     * {@link DishRecommender#MAX_CANDIDATES} dań z puli posiłku, których przy
     * tym podpisie jeszcze nie oceniono i nie pokazano w serii. Gdy nie ma
     * już kogo oceniać, zwraca pusty wynik bez wywołania.
     */
    public static Refill refill(DishRater rater, String mealType, String signature,
                                ReadyDishPool pool, DishReactionLog reactions,
                                List<Recipe> mealPool, Set<String> shown, DietConstraints diet,
                                MealHistory history, long now, Random random) throws IOException {
        Set<String> skip = new HashSet<>();
        for (String name : shown) {
            skip.add(ReadyDishPool.key(name));
        }
        if (pool.isCurrent(signature)) {
            skip.addAll(pool.getRated());
        }
        List<Recipe> unrated = new ArrayList<>();
        for (Recipe recipe : mealPool) {
            if (!skip.contains(ReadyDishPool.key(recipe.getTitle()))) {
                unrated.add(recipe);
            }
        }
        List<Recipe> candidates = DishRecommender.candidates(unrated, history, now, random);
        if (candidates.isEmpty()) {
            return new Refill(signature, Collections.<DishRating>emptyList(),
                    Collections.<String>emptyList());
        }
        List<DishRating> ratings = rater.rate(mealType,
                reactions.latestPerDish(DishRecommender.MAX_REACTIONS),
                DishRecommender.describe(candidates), diet, now);
        List<DishRating> fresh = new ArrayList<>();
        for (DishRating rating : DishRecommender.topAllowed(ratings, candidates, diet,
                KEEP_PER_REFILL)) {
            if (rating.getScore() >= MIN_SCORE) {
                fresh.add(rating);
            }
        }
        List<String> rated = new ArrayList<>();
        for (Recipe recipe : candidates) {
            rated.add(recipe.getTitle());
        }
        return new Refill(signature, fresh, rated);
    }

    private static Set<String> latestDislikes(DishReactionLog reactions) {
        Set<String> result = new HashSet<>();
        for (DishReaction reaction : reactions.latestPerDish(DishReactionLog.MAX_STORED)) {
            if (!reaction.isLiked()) {
                result.add(ReadyDishPool.key(reaction.getDish()));
            }
        }
        return result;
    }

    private static Map<String, Recipe> byKey(List<Recipe> recipes) {
        Map<String, Recipe> map = new HashMap<>();
        for (Recipe recipe : recipes) {
            map.put(ReadyDishPool.key(recipe.getTitle()), recipe);
        }
        return map;
    }
}
