package com.mealspire.app.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zamrożony agregat skompaktowanych zdarzeń gustu: sumy wag per
 * (wymiar, wartość, slot), zdyskontowane na moment kompakcji {@code asOf}.
 * Wygaszanie wykładnicze składa się poprawnie, więc przy odczycie wystarczy
 * jeden wspólny mnożnik {@code 0.5^((now - asOf) / półokres)} — agregat jest
 * matematycznie równoważny zdarzeniom, które w nim zniknęły.
 *
 * <p>Uczciwe ograniczenie: „przeliczenie od zera" (np. po zmianie słowników
 * taggera) działa wstecz tylko na zdarzenia wciąż obecne w dzienniku; agregat
 * pozostaje w starej interpretacji i po prostu wygasa.
 */
public final class FrozenTasteAggregate {

    /** Zmiana algorytmu agregacji ⇒ podbij; niezgodny agregat jest porzucany. */
    public static final int SCHEMA_VERSION = 1;

    private final long asOf;
    private final Map<String, Double> scores;
    private final Map<String, Integer> observations;
    private final double explicitPositiveMass;

    public FrozenTasteAggregate(long asOf, Map<String, Double> scores,
                                Map<String, Integer> observations,
                                double explicitPositiveMass) {
        this.asOf = asOf;
        this.scores = Collections.unmodifiableMap(new LinkedHashMap<>(
                scores == null ? Collections.<String, Double>emptyMap() : scores));
        this.observations = Collections.unmodifiableMap(new LinkedHashMap<>(
                observations == null ? Collections.<String, Integer>emptyMap()
                        : observations));
        this.explicitPositiveMass = explicitPositiveMass;
    }

    public static FrozenTasteAggregate empty() {
        return new FrozenTasteAggregate(0L, null, null, 0.0);
    }

    /** Klucz wyniku: wymiar|slot|wartość (slot -1 = wszystkie posiłki). */
    public static String scoreKey(TasteDimension dimension, int mealIndex, String value) {
        return dimension.name() + "|" + mealIndex + "|" + value;
    }

    /** Klucz licznika obserwacji: wymiar|slot. */
    public static String observationKey(TasteDimension dimension, int mealIndex) {
        return dimension.name() + "|" + mealIndex;
    }

    public long getAsOf() {
        return asOf;
    }

    /** Sumy wag zdyskontowane na {@link #getAsOf()}, per {@link #scoreKey}. */
    public Map<String, Double> getScores() {
        return scores;
    }

    /** Liczby pozytywnych obserwacji per {@link #observationKey} (nie wygasają). */
    public Map<String, Integer> getObservations() {
        return observations;
    }

    /** Suma wag jawnych sygnałów pozytywnych, bez dyskonta (do progu gotowości). */
    public double getExplicitPositiveMass() {
        return explicitPositiveMass;
    }

    public boolean isEmpty() {
        return scores.isEmpty() && observations.isEmpty()
                && explicitPositiveMass == 0.0;
    }
}
