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
        private final boolean broad;

        ExplorationGoal(TasteDimension dimension, String value, boolean broad) {
            this.dimension = dimension;
            this.value = value;
            this.broad = broad;
        }

        public TasteDimension getDimension() {
            return dimension;
        }

        public String getValue() {
            return value;
        }

        /**
         * Slot z małą historią odwraca proporcje: zamiast 2+1 jest 1+2 —
         * cienki slot ma się szybko douczać, nie udawać, że coś wie.
         */
        public boolean isBroad() {
            return broad;
        }

        /** Instrukcja 2+1 (albo 1+2 dla cienkiego slotu) do promptu propozycji. */
        public String promptSentence() {
            if (broad) {
                return "Użytkownik ma dla tej pory posiłku mało historii. Pierwszą "
                        + "propozycję dopasuj do jego ogólnego profilu, a dwie "
                        + "pozostałe zaproponuj celowo różnorodne, spoza utartych "
                        + "wyborów — jedną z nich jako danie, którego "
                        + dimension.instrumental() + " jest " + value
                        + " — nadal proste i bezwzględnie zgodne z wymaganiami diety.";
            }
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
        boolean thinSlot = mealIndex >= 0 && model.totalObservations(mealIndex)
                < TasteContextBuilder.MIN_SLOT_OBSERVATIONS;
        int slot = thinSlot || mealIndex < 0 ? TasteModel.ALL_MEALS : mealIndex;

        // Najmniej zbadane wymiary najpierw; w każdym szukamy dozwolonej wartości.
        for (TasteDimension dimension : dimensionsByObservations(model, slot)) {
            String value = leastKnownAllowedValue(model, dimension, slot, diet, random);
            if (value != null) {
                return new ExplorationGoal(dimension, value, thinSlot);
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
