package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Wymiarowy model gustu — pochodna dziennika zdarzeń (i zamrożonego agregatu
 * po kompakcji), nigdy źródło prawdy. Każde zdarzenie wnosi wagę swojego typu
 * wygaszaną wykładniczo (półokres {@link #HALF_LIFE_DAYS} dni), rozpisaną na
 * wymiary dania przez {@link DishTagger}; profil liczony jest osobno per slot
 * posiłku i łącznie. Wynik wartości nie spada poniżej
 * {@link #MIN_VALUE_SCORE} — kilka rerolli nie „banuje" składnika.
 */
public final class TasteModel {

    /** Półokres wygaszania: świeże zdarzenia ważą najwięcej, gust „płynie". */
    public static final double HALF_LIFE_DAYS = 60.0;

    /** Sufit sygnału ujemnego per wartość wymiaru. */
    public static final double MIN_VALUE_SCORE = -1.0;

    /** Slot „wszystkie posiłki". */
    public static final int ALL_MEALS = -1;

    private static final double HALF_LIFE_MILLIS = HALF_LIFE_DAYS * 24 * 60 * 60 * 1000;

    private final Map<String, Double> scores;
    private final Map<String, Integer> observations;
    private final double explicitPositiveMass;

    private TasteModel(Map<String, Double> scores, Map<String, Integer> observations,
                       double explicitPositiveMass) {
        this.scores = scores;
        this.observations = observations;
        this.explicitPositiveMass = explicitPositiveMass;
    }

    /** Mnożnik wygaszania dla zdarzenia sprzed {@code ageMillis}. */
    public static double decayFactor(long ageMillis) {
        if (ageMillis <= 0) {
            return 1.0;
        }
        return Math.pow(0.5, ageMillis / HALF_LIFE_MILLIS);
    }

    public static TasteModel build(TasteEventLog log, FrozenTasteAggregate frozen,
                                   DishTagger tagger, Map<String, String> detailsByTitle,
                                   long now) {
        TasteAccumulator accumulator = new TasteAccumulator();
        accumulator.absorb(frozen, now);
        if (log != null) {
            for (TasteEvent event : log.events()) {
                accumulator.absorb(event, tagger, detailsByTitle, now);
            }
        }
        return new TasteModel(accumulator.scores, accumulator.observations,
                accumulator.explicitPositiveMass);
    }

    private static int count(Map<String, Integer> map, String key) {
        Integer current = map.get(key);
        return current == null ? 0 : current;
    }

    /** Wynik wartości wymiaru w slocie, z sufitem ujemnym. */
    public double score(TasteDimension dimension, String value, int mealIndex) {
        Double raw = scores.get(FrozenTasteAggregate.scoreKey(dimension, mealIndex, value));
        return Math.max(raw == null ? 0.0 : raw, MIN_VALUE_SCORE);
    }

    /** Liczba pozytywnych obserwacji wymiaru w slocie (miara pewności). */
    public int observationCount(TasteDimension dimension, int mealIndex) {
        return count(observations,
                FrozenTasteAggregate.observationKey(dimension, mealIndex));
    }

    /** Suma pozytywnych obserwacji wszystkich wymiarów w slocie. */
    public int totalObservations(int mealIndex) {
        int total = 0;
        for (TasteDimension dimension : TasteDimension.values()) {
            total += observationCount(dimension, mealIndex);
        }
        return total;
    }

    /** Wartości wymiaru o dodatnim wyniku, najmocniejsze pierwsze. */
    public List<String> topValues(TasteDimension dimension, int mealIndex, int limit) {
        String prefix = dimension.name() + "|" + mealIndex + "|";
        List<Map.Entry<String, Double>> matching = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue() > 0) {
                matching.add(entry);
            }
        }
        Collections.sort(matching, (a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, Double> entry : matching) {
            if (values.size() >= limit) {
                break;
            }
            values.add(entry.getKey().substring(prefix.length()));
        }
        return values;
    }

    /**
     * Suma wag jawnych sygnałów pozytywnych, bez wygaszania — stabilna,
     * monotoniczna miara „ile system naprawdę wie" (m.in. do diagnostyki).
     */
    public double explicitPositiveMass() {
        return explicitPositiveMass;
    }
}
