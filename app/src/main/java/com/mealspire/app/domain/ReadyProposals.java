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
 * {@link #refill} — wołane w tle, zanim pula się wyczerpie, i gdy jest pusta —
 * generuje nowe dania przez LLM i ocenia nieocenione jeszcze dania katalogu.
 * W jednej serii (kolejne „Inne propozycje" dla tego samego posiłku) żadne
 * danie nie wraca; gdy brak już nowych dań, wynik jest krótszy albo pusty —
 * nigdy powtórka.
 */
public final class ReadyProposals {

    /**
     * Uzupełniaj, gdy w puli zostało mniej niż tyle zestawów: przy 3 kartach
     * to 6 dań, czyli dwa kolejne „Inne propozycje" — z zapasem na czas
     * jednego wywołania LLM, zanim pula się wyczerpie.
     */
    public static final int LOW_WATER_SETS = 2;
    /** Dania ocenione niżej nie trafiają do puli (jak dawniej: pokazywano tylko czołówkę). */
    public static final int MIN_SCORE = 5;
    /** Z jednej oceny trafia do puli najwyżej tyle najlepszych dań. */
    public static final int KEEP_PER_REFILL = 6;
    /** Ile nowych dań generuje LLM w jednym uzupełnieniu (limit `/v1/proposals`). */
    public static final int GENERATE_PER_REFILL = 6;
    /** Ile nazw do unikania (pula, seria, historia) trafia do promptu generacji. */
    public static final int MAX_AVOID = 40;

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

    /**
     * Wynik {@link #take}: równoległe listy — przepis z katalogu albo
     * {@code null}, opis nowego dania albo {@code null}, powód ("" offline) —
     * i pula po zdjęciu pokazanych.
     */
    public static final class Selection {
        private final List<Recipe> recipes;
        private final List<DishProposal> generated;
        private final List<String> reasons;
        private final ReadyDishPool remaining;
        private final Source source;

        Selection(List<Recipe> recipes, List<DishProposal> generated, List<String> reasons,
                  ReadyDishPool remaining, Source source) {
            this.recipes = Collections.unmodifiableList(recipes);
            this.generated = Collections.unmodifiableList(generated);
            this.reasons = Collections.unmodifiableList(reasons);
            this.remaining = remaining;
            this.source = source;
        }

        /** Przepis z katalogu; {@code null} dla nowego dania (przepis na żądanie). */
        public List<Recipe> getRecipes() {
            return recipes;
        }

        /** Opis nowego dania od LLM; {@code null} dla dania z katalogu. */
        public List<DishProposal> getGenerated() {
            return generated;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public int size() {
            return reasons.size();
        }

        public ReadyDishPool getRemaining() {
            return remaining;
        }

        public Source getSource() {
            return source;
        }
    }

    /** Wynik {@link #refill}: nowe wpisy do puli i wszystkie ocenione nazwy. */
    public static final class Refill {
        private final String signature;
        private final List<DishRating> fresh;
        private final List<DishProposal> newDishes;
        private final List<String> rated;

        Refill(String signature, List<DishRating> fresh, List<DishProposal> newDishes,
               List<String> rated) {
            this.signature = signature;
            this.fresh = Collections.unmodifiableList(fresh);
            this.newDishes = Collections.unmodifiableList(newDishes);
            this.rated = Collections.unmodifiableList(rated);
        }

        public boolean isEmpty() {
            return rated.isEmpty() && fresh.isEmpty();
        }

        /** Ile nowych dań (spoza katalogu) wygenerował LLM. */
        public int generatedCount() {
            return newDishes.size();
        }

        /** Dołącza wynik do aktualnej (być może już uszczuplonej) puli. */
        public ReadyDishPool applyTo(ReadyDishPool current, Collection<String> shown) {
            return current.merge(signature, fresh, rated, newDishes, shown);
        }
    }

    private ReadyProposals() {
    }

    /**
     * Natychmiastowy wybór — bez sieci.
     *
     * @param signature aktualny podpis gustu; z puli ocenionej przed zmianą
     *                  reakcji pomijamy dania, których teraz „nie lubię"
     *                  (aktualna pula oceniła je już z tą reakcją — to nie zakaz)
     * @param mealPool dania posiłku już po diecie (katalog + książka kucharska);
     *                 wpisy katalogowe spoza niej (usunięte, łamiące dietę) odpadają
     * @param diet     twardy filtr nowych dań (katalog przefiltrowano już w mealPool)
     * @param shown    dania pokazane już w tej serii — nie wracają
     * @param offline  kolejność zapasowa (pipeline offline) na braki
     */
    public static Selection take(ReadyDishPool pool, String signature, List<Recipe> mealPool,
                                 DietConstraints diet, DishReactionLog reactions,
                                 Set<String> shown, List<Recipe> offline, int count) {
        Map<String, Recipe> known = byKey(mealPool);
        Set<String> disliked = pool.isCurrent(signature)
                ? Collections.<String>emptySet() : latestDislikes(reactions);
        Set<String> used = new HashSet<>();
        for (String name : shown) {
            used.add(ReadyDishPool.key(name));
        }
        List<Recipe> recipes = new ArrayList<>();
        List<DishProposal> generated = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<String> drop = new ArrayList<>();
        for (DishRating entry : pool.getEntries()) {
            String key = ReadyDishPool.key(entry.getDish());
            Recipe recipe = known.get(key);
            DishProposal dish = recipe == null ? pool.generated(key) : null;
            boolean usable = recipe != null || (dish != null && allows(diet, dish));
            if (!usable || disliked.contains(key) || used.contains(key)) {
                drop.add(entry.getDish());
                continue;
            }
            if (reasons.size() >= count) {
                break;
            }
            used.add(key);
            recipes.add(recipe);
            generated.add(dish);
            reasons.add(entry.getReason());
            drop.add(entry.getDish());
        }
        int ready = reasons.size();
        for (Recipe recipe : offline) {
            if (reasons.size() >= count) {
                break;
            }
            if (known.containsKey(ReadyDishPool.key(recipe.getTitle()))
                    && used.add(ReadyDishPool.key(recipe.getTitle()))) {
                recipes.add(recipe);
                generated.add(null);
                reasons.add("");
            }
        }
        Source source = reasons.isEmpty() ? Source.EXHAUSTED
                : ready == reasons.size() ? Source.READY
                : ready > 0 ? Source.PARTIAL : Source.OFFLINE;
        return new Selection(recipes, generated, reasons, pool.without(drop), source);
    }

    /**
     * Czy pula wymaga uzupełnienia w tle: nieaktualny gust, pusta albo mniej
     * niż {@link #LOW_WATER_SETS} zestawów — czyli zanim się wyczerpie.
     */
    public static boolean needsRefill(ReadyDishPool pool, String signature, Set<String> shown,
                                      int count) {
        Set<String> keys = new HashSet<>();
        for (String name : shown) {
            keys.add(ReadyDishPool.key(name));
        }
        return !pool.isCurrent(signature) || pool.available(keys) < LOW_WATER_SETS * count;
    }

    /**
     * Nazwy, których generacja ma unikać: dania z puli, pokazane w serii,
     * ostatnio jedzone — najwyżej {@link #MAX_AVOID}.
     */
    public static List<String> avoidList(ReadyDishPool pool, Collection<String> shown,
                                         List<String> recent) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<String> all = new ArrayList<>();
        for (DishRating entry : pool.getEntries()) {
            all.add(entry.getDish());
        }
        all.addAll(shown);
        all.addAll(recent);
        for (String name : all) {
            if (result.size() < MAX_AVOID && seen.add(ReadyDishPool.key(name))) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * Uzupełnienie (blokujące — tylko w tle): jedno wywołanie generacji
     * {@link #GENERATE_PER_REFILL} nowych dań (request niesie już listę do
     * unikania) oraz, dopóki są, ocena do {@link DishRecommender#MAX_CANDIDATES}
     * nieocenionych dań katalogu. Wynik bez nazw już obecnych, pokazanych w
     * serii i łamiących dietę. Wyjątek tylko wtedy, gdy nie udało się nic.
     */
    public static Refill refill(DishRater rater, RecipeOperations generator,
                                RecipeRequest request, String signature, ReadyDishPool pool,
                                DishReactionLog reactions, List<Recipe> mealPool,
                                Set<String> shown, DietConstraints diet, MealHistory history,
                                long now, Random random) throws IOException {
        Set<String> skip = new HashSet<>();
        for (String name : shown) {
            skip.add(ReadyDishPool.key(name));
        }
        boolean current = pool.isCurrent(signature);
        if (current) {
            skip.addAll(pool.getRated());
        }
        IOException failure = null;
        List<DishRating> fresh = new ArrayList<>();
        List<String> rated = new ArrayList<>();
        List<Recipe> unrated = new ArrayList<>();
        for (Recipe recipe : mealPool) {
            if (!skip.contains(ReadyDishPool.key(recipe.getTitle()))) {
                unrated.add(recipe);
            }
        }
        List<Recipe> candidates = DishRecommender.candidates(unrated, history, now, random);
        if (!candidates.isEmpty()) {
            try {
                List<DishRating> ratings = rater.rate(request.getMealType(),
                        reactions.latestPerDish(DishRecommender.MAX_REACTIONS),
                        DishRecommender.describe(candidates), diet, now);
                for (DishRating rating : DishRecommender.topAllowed(ratings, candidates, diet,
                        KEEP_PER_REFILL)) {
                    if (rating.getScore() >= MIN_SCORE) {
                        fresh.add(rating);
                    }
                }
                for (Recipe recipe : candidates) {
                    rated.add(recipe.getTitle());
                }
            } catch (IOException | RuntimeException e) {
                failure = asIo(e);
            }
        }

        Set<String> present = new HashSet<>(skip);
        for (DishRating entry : current ? pool.getEntries() : Collections.<DishRating>emptyList()) {
            present.add(ReadyDishPool.key(entry.getDish()));
        }
        for (String name : rated) {
            present.add(ReadyDishPool.key(name)); // just rated (also too low): no back door
        }
        Map<String, Recipe> catalogue = byKey(mealPool);
        List<DishProposal> newDishes = new ArrayList<>();
        try {
            for (DishProposal dish : generator.proposeDishes(request, GENERATE_PER_REFILL)) {
                String key = ReadyDishPool.key(dish.getName());
                if (key.isEmpty() || !allows(diet, dish) || !present.add(key)) {
                    continue;
                }
                // Nazwa z katalogu wraca jako zwykły wpis z gotowym przepisem.
                fresh.add(new DishRating(dish.getName(), MIN_SCORE, ""));
                if (!catalogue.containsKey(key)) {
                    newDishes.add(dish);
                }
            }
        } catch (IOException | RuntimeException e) {
            if (failure == null) {
                failure = asIo(e);
            }
        }
        if (failure != null && fresh.isEmpty() && rated.isEmpty()) {
            throw failure;
        }
        return new Refill(signature, fresh, newDishes, rated);
    }

    private static boolean allows(DietConstraints diet, DishProposal dish) {
        return diet == null || diet.allows(dish.getName() + "\n" + dish.getDescription()
                + "\n" + String.join(", ", dish.getKeyIngredients()));
    }

    private static IOException asIo(Exception e) {
        return e instanceof IOException ? (IOException) e : new IOException(e.getMessage(), e);
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
