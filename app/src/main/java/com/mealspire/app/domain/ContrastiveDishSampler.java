package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Dobiera trójki dań do rund quizu tak, żeby każdy wybór niósł maksimum
 * informacji (active learning): każda runda rozstrzyga jeden wymiar gustu —
 * dania w rundzie pochodzą z kontrastujących „przegródek" tego wymiaru
 * (mięso/ryba/bezmięsne, zupa/zapiekane/świeże, polskie/śródziemnomorskie/
 * azjatyckie). Losowość w obrębie przegródki utrzymuje różnorodność między
 * instalacjami; pusta przegródka (np. ryby po wykluczeniu) dostaje losowe
 * danie spoza schematu, żeby runda zawsze była pełna. Wykluczenia diety
 * obowiązują bezwzględnie także tutaj.
 */
public final class ContrastiveDishSampler {

    public static final int ROUND_SIZE = 3;

    /** Jedna runda: wymiar + kontrastujące przegródki wartości. */
    private static final class RoundSpec {
        final TasteDimension dimension;
        final String[][] buckets;

        RoundSpec(TasteDimension dimension, String[][] buckets) {
            this.dimension = dimension;
            this.buckets = buckets;
        }
    }

    private static final RoundSpec[] ROUND_SPECS = {
            new RoundSpec(TasteDimension.BASE, new String[][]{
                    {"kurczak", "wieprzowina", "wołowina", "mięso mielone"},
                    {"ryba"},
                    {"jajka", "strączki", "warzywa", "nabiał"}}),
            new RoundSpec(TasteDimension.CHARACTER, new String[][]{
                    {"zupa"},
                    {"zapiekanka", "z patelni"},
                    {"sałatka", "kanapki"}}),
            new RoundSpec(TasteDimension.CUISINE, new String[][]{
                    {"polska"},
                    {"włoska", "śródziemnomorska"},
                    {"azjatycka", "meksykańska"}}),
    };

    private final DishTagger tagger = new DishTagger();

    /** Liczba rund — stała designu: 3 (AI nie ma przejąć propozycji po quizie). */
    public static int rounds() {
        return ROUND_SPECS.length;
    }

    /**
     * @param pools pule dań (wszystkie pory posiłku); rundy losują ze
     *              wszystkich naraz — kontrast wymiaru jest ważniejszy niż pora
     * @param diet  twarde wykluczenia z wcześniejszego pytania quizu
     */
    public List<List<Recipe>> sample(Recipe[][] pools, DietConstraints diet,
                                     Random random) {
        List<Recipe> candidates = allowedCandidates(pools, diet);
        Collections.shuffle(candidates, random);

        List<String> usedTitles = new ArrayList<>();
        List<List<Recipe>> result = new ArrayList<>();
        for (RoundSpec spec : ROUND_SPECS) {
            List<Recipe> round = new ArrayList<>();
            for (String[] bucket : spec.buckets) {
                Recipe pick = takeMatching(candidates, usedTitles, spec.dimension,
                        bucket);
                if (pick != null) {
                    round.add(pick);
                    usedTitles.add(pick.getTitle().toLowerCase());
                }
            }
            // Pusta przegródka (mała pula, wykluczenia) — dopełnij czymkolwiek,
            // żeby użytkownik zawsze miał z czego wybierać.
            while (round.size() < ROUND_SIZE) {
                Recipe filler = takeAnyUnused(candidates, usedTitles);
                if (filler == null) {
                    break;
                }
                round.add(filler);
                usedTitles.add(filler.getTitle().toLowerCase());
            }
            Collections.shuffle(round, random);
            result.add(round);
        }
        return result;
    }

    private List<Recipe> allowedCandidates(Recipe[][] pools, DietConstraints diet) {
        List<Recipe> candidates = new ArrayList<>();
        List<String> seenTitles = new ArrayList<>();
        if (pools != null) {
            for (Recipe[] pool : pools) {
                for (Recipe recipe : pool) {
                    String key = recipe.getTitle().toLowerCase();
                    if (seenTitles.contains(key)) {
                        continue;
                    }
                    if (diet != null && !diet.allows(
                            recipe.getTitle() + "\n" + recipe.getDetails())) {
                        continue;
                    }
                    seenTitles.add(key);
                    candidates.add(recipe);
                }
            }
        }
        return candidates;
    }

    private Recipe takeMatching(List<Recipe> candidates, List<String> usedTitles,
                                TasteDimension dimension, String[] bucket) {
        List<String> values = Arrays.asList(bucket);
        for (Recipe recipe : candidates) {
            if (usedTitles.contains(recipe.getTitle().toLowerCase())) {
                continue;
            }
            String value = tagger.tag(recipe.getTitle(), recipe.getDetails())
                    .get(dimension);
            if (value != null && values.contains(value)) {
                return recipe;
            }
        }
        return null;
    }

    private static Recipe takeAnyUnused(List<Recipe> candidates, List<String> usedTitles) {
        for (Recipe recipe : candidates) {
            if (!usedTitles.contains(recipe.getTitle().toLowerCase())) {
                return recipe;
            }
        }
        return null;
    }
}
