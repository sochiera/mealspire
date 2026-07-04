package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Wybiera cel eksploracji dla reguły 2+1: dwie propozycje w gust, trzecia
 * celowo spoza utartych wyborów. Cel wybiera appka (deterministycznie względem
 * modelu i ziarna), nie model — eksploracja jest decyzją systemu, AI tylko ją
 * realizuje. Heurystyka: najmniej zbadany wymiar, w nim najsłabiej znana
 * dozwolona wartość — bez pełnego bandity, bo sygnałów jest mało, a koszt
 * złej propozycji to jeden reroll.
 */
public final class ExplorationPlanner {

    /** Cel eksploracji: wymiar + wartość + gotowe zdanie do promptu. */
    public static final class ExplorationGoal {
        private final TasteDimension dimension;
        private final String value;

        ExplorationGoal(TasteDimension dimension, String value) {
            this.dimension = dimension;
            this.value = value;
        }

        public TasteDimension getDimension() {
            return dimension;
        }

        public String getValue() {
            return value;
        }

        /** Instrukcja 2+1 dla promptu propozycji. */
        public String promptSentence() {
            return "Pierwsze dwie propozycje dopasuj do profilu użytkownika. "
                    + "Trzecią zaproponuj celowo spoza jego utartych wyborów: danie, "
                    + "którego " + dimension.instrumental() + " jest " + value
                    + " — nadal proste i bezwzględnie zgodne z wymaganiami diety.";
        }
    }

    private final DishTagger tagger = new DishTagger();

    /**
     * @return cel eksploracji albo {@code null}, gdy model nic jeszcze nie wie
     *         (przed personalizacją eksplorację załatwia pula offline)
     */
    public ExplorationGoal plan(TasteModel model, DietConstraints diet, int mealIndex,
                                Random random) {
        if (model == null || model.totalObservations(TasteModel.ALL_MEALS) == 0) {
            return null;
        }
        int slot = mealIndex >= 0 && model.totalObservations(mealIndex)
                >= TasteContextBuilder.MIN_SLOT_OBSERVATIONS
                ? mealIndex : TasteModel.ALL_MEALS;

        // Najmniej zbadane wymiary najpierw; w każdym szukamy dozwolonej wartości.
        for (TasteDimension dimension : dimensionsByObservations(model, slot)) {
            String value = leastKnownAllowedValue(model, dimension, slot, diet, random);
            if (value != null) {
                return new ExplorationGoal(dimension, value);
            }
        }
        return null;
    }

    private List<TasteDimension> dimensionsByObservations(TasteModel model, int slot) {
        List<TasteDimension> dimensions = new ArrayList<>();
        for (TasteDimension dimension : TasteDimension.values()) {
            dimensions.add(dimension);
        }
        dimensions.sort((a, b) -> Integer.compare(
                model.observationCount(a, slot), model.observationCount(b, slot)));
        return dimensions;
    }

    /** Wartość o najniższym wyniku (remisy losowo), z poszanowaniem diety. */
    private String leastKnownAllowedValue(TasteModel model, TasteDimension dimension,
                                          int slot, DietConstraints diet, Random random) {
        double bestScore = Double.MAX_VALUE;
        List<String> best = new ArrayList<>();
        for (String value : tagger.knownValues(dimension)) {
            if (diet != null && !diet.allows(value)) {
                continue;
            }
            double score = model.score(dimension, value, slot);
            if (score < bestScore) {
                bestScore = score;
                best.clear();
                best.add(value);
            } else if (score == bestScore) {
                best.add(value);
            }
        }
        if (best.isEmpty()) {
            return null;
        }
        return best.get(random.nextInt(best.size()));
    }
}
