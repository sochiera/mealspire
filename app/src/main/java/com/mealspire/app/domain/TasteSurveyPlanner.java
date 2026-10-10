package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Układa pary dań do ankiety gustu tak, żeby każdy wybór niósł jak najwięcej
 * informacji: dania w parze kontrastują w kolejnym wymiarze gustu (baza →
 * charakter → kuchnia, na zmianę), żadna para się nie powtarza, a dania
 * pojawiają się równomiernie (najpierw te jeszcze niepokazane), więc długa
 * ankieta przegląda całą pulę. Losowa kolejność puli daje różne ankiety
 * między instalacjami. Wykluczenia diety obowiązują bezwzględnie.
 */
public final class TasteSurveyPlanner {

    private static final TasteDimension[] DIMENSIONS = {
            TasteDimension.BASE, TasteDimension.CHARACTER, TasteDimension.CUISINE};

    private final DishTagger tagger = new DishTagger();

    /**
     * @param pools pule dań (wszystkie pory posiłku naraz)
     * @param count żądana liczba par; mniej, gdy pula nie wystarcza na różne pary
     */
    public List<TasteSurvey.Pair> plan(Recipe[][] pools, DietConstraints diet, int count,
                                       java.util.Random random) {
        List<Recipe> candidates = allowedCandidates(pools, diet);
        Collections.shuffle(candidates, random);
        Map<String, DishTags> tags = new HashMap<>();
        Map<String, Integer> uses = new HashMap<>();
        for (Recipe recipe : candidates) {
            tags.put(recipe.getTitle(), tagger.tag(recipe.getTitle(), recipe.getDetails()));
            uses.put(recipe.getTitle(), 0);
        }

        Set<String> usedPairs = new HashSet<>();
        List<TasteSurvey.Pair> result = new ArrayList<>();
        String previousA = null;
        String previousB = null;
        for (int i = 0; i < count; i++) {
            TasteDimension dimension = DIMENSIONS[i % DIMENSIONS.length];
            TasteSurvey.Pair pair = nextPair(candidates, tags, uses, usedPairs, dimension,
                    previousA, previousB);
            if (pair == null) {
                break;
            }
            result.add(pair);
            usedPairs.add(pairKey(pair.getDishA(), pair.getDishB()));
            uses.put(pair.getDishA(), uses.get(pair.getDishA()) + 1);
            uses.put(pair.getDishB(), uses.get(pair.getDishB()) + 1);
            previousA = pair.getDishA();
            previousB = pair.getDishB();
        }
        return result;
    }

    private static TasteSurvey.Pair nextPair(List<Recipe> candidates, Map<String, DishTags> tags,
                                             Map<String, Integer> uses, Set<String> usedPairs,
                                             TasteDimension dimension,
                                             String previousA, String previousB) {
        // A: spośród najrzadziej pokazanych dań to, dla którego jest najlepsze B;
        // dania z poprzedniej pary odpadają, żeby ekran zawsze się zmieniał.
        List<String> byUse = new ArrayList<>();
        for (Recipe recipe : candidates) {
            byUse.add(recipe.getTitle());
        }
        Collections.sort(byUse, (x, y) -> uses.get(x) - uses.get(y)); // stabilne
        int level = -1;
        String bestA = null;
        String bestB = null;
        int bestScore = Integer.MAX_VALUE;
        for (String a : byUse) {
            if (a.equals(previousA) || a.equals(previousB)) {
                continue;
            }
            if (level >= 0 && uses.get(a) > level && bestA != null) {
                break; // rzadziej pokazane A z dobrą parą już jest
            }
            level = uses.get(a);
            for (String b : byUse) {
                if (b.equals(a) || b.equals(previousA) || b.equals(previousB)
                        || usedPairs.contains(pairKey(a, b))) {
                    continue;
                }
                // Najpierw równomierność (mniej pokazane), potem kontrast w wymiarze pary.
                int score = uses.get(b) * 2 + (contrasts(tags.get(a), tags.get(b), dimension)
                        ? 0 : 1);
                if (score < bestScore) {
                    bestA = a;
                    bestB = b;
                    bestScore = score;
                }
            }
        }
        return bestA == null ? null : new TasteSurvey.Pair(bestA, bestB);
    }

    private static boolean contrasts(DishTags a, DishTags b, TasteDimension dimension) {
        String valueA = a.get(dimension);
        String valueB = b.get(dimension);
        return valueA != null && valueB != null && !valueA.equals(valueB);
    }

    private static String pairKey(String a, String b) {
        String x = a.toLowerCase();
        String y = b.toLowerCase();
        return x.compareTo(y) < 0 ? x + "\n" + y : y + "\n" + x;
    }

    private static List<Recipe> allowedCandidates(Recipe[][] pools, DietConstraints diet) {
        List<Recipe> candidates = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();
        if (pools != null) {
            for (Recipe[] pool : pools) {
                for (Recipe recipe : pool) {
                    if (recipe.getTitle().trim().isEmpty()
                            || !seenTitles.add(recipe.getTitle().toLowerCase())) {
                        continue;
                    }
                    if (diet != null && !diet.allows(
                            recipe.getTitle() + "\n" + recipe.getDetails())) {
                        continue;
                    }
                    candidates.add(recipe);
                }
            }
        }
        return candidates;
    }
}
