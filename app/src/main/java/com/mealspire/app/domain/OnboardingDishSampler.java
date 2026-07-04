package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Losuje trójki dań do rund onboardingowego testu gustu. Każda runda zawiera
 * po jednym daniu z każdej puli (pory posiłku), więc zestawy są z natury
 * różnorodne, a dania nie powtarzają się między rundami. {@link Random} jest
 * wstrzykiwany, żeby testy były deterministyczne.
 */
public final class OnboardingDishSampler {

    /**
     * @param pools  pule dań (np. {@link BuiltInRecipes#forMeal(int)} dla
     *               każdej pory posiłku); jedna pozycja rundy pochodzi z jednej puli
     * @param rounds liczba rund
     * @return lista rund; każda runda ma po jednym daniu z każdej puli,
     *         w losowej kolejności, bez powtórek tytułów między rundami
     */
    public List<List<Recipe>> sample(Recipe[][] pools, int rounds, Random random) {
        // Per-pool candidates, shuffled once; drawing from the front gives
        // no-replacement picks, and the used-title set guards against the same
        // dish appearing in two pools.
        List<List<Recipe>> candidates = new ArrayList<>();
        for (Recipe[] pool : pools) {
            List<Recipe> shuffled = new ArrayList<>();
            Collections.addAll(shuffled, pool);
            Collections.shuffle(shuffled, random);
            candidates.add(shuffled);
        }

        List<String> usedTitles = new ArrayList<>();
        List<List<Recipe>> result = new ArrayList<>();
        for (int round = 0; round < rounds; round++) {
            List<Recipe> picks = new ArrayList<>();
            for (List<Recipe> pool : candidates) {
                Recipe pick = takeUnused(pool, usedTitles);
                if (pick != null) {
                    picks.add(pick);
                    usedTitles.add(pick.getTitle().toLowerCase());
                }
            }
            Collections.shuffle(picks, random);
            result.add(picks);
        }
        return result;
    }

    private static Recipe takeUnused(List<Recipe> pool, List<String> usedTitles) {
        while (!pool.isEmpty()) {
            Recipe candidate = pool.remove(0);
            if (!usedTitles.contains(candidate.getTitle().toLowerCase())) {
                return candidate;
            }
        }
        return null;
    }
}
