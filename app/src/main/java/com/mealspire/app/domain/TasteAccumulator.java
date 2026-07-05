package com.mealspire.app.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Wspólna arytmetyka modelu gustu: jak zdarzenie (i stary agregat) rozkłada
 * się na wyniki per (wymiar, slot, wartość), liczniki obserwacji i masę
 * jawnych sygnałów. Jedna definicja dla {@link TasteModel} (odczyt) i
 * {@link TasteEventCompactor} (kompakcja) — dzięki temu model policzony
 * z pełnego dziennika i z (przycięty dziennik + agregat) wychodzi ten sam.
 */
final class TasteAccumulator {

    final Map<String, Double> scores = new HashMap<>();
    final Map<String, Integer> observations = new HashMap<>();
    double explicitPositiveMass;

    /** Wchłania stary agregat, dyskontując jego wyniki do {@code now}. */
    void absorb(FrozenTasteAggregate aggregate, long now) {
        if (aggregate == null || aggregate.isEmpty()) {
            return;
        }
        double factor = TasteModel.decayFactor(now - aggregate.getAsOf());
        for (Map.Entry<String, Double> entry : aggregate.getScores().entrySet()) {
            merge(entry.getKey(), entry.getValue() * factor);
        }
        for (Map.Entry<String, Integer> entry : aggregate.getObservations().entrySet()) {
            Integer current = observations.get(entry.getKey());
            observations.put(entry.getKey(),
                    (current == null ? 0 : current) + entry.getValue());
        }
        explicitPositiveMass += aggregate.getExplicitPositiveMass();
    }

    /** Wchłania jedno zdarzenie, z wygaszaniem względem {@code now}. */
    void absorb(TasteEvent event, DishTagger tagger, Map<String, String> detailsByTitle,
                long now) {
        if (event.getType().isExplicit() && event.getType().weight() > 0) {
            explicitPositiveMass += event.getType().weight();
        }
        double weight = event.effectiveWeight();
        if (weight == 0.0) {
            return;
        }
        double contribution = weight * TasteModel.decayFactor(
                now - event.getTimestamp());
        String details = detailsByTitle == null
                ? null : detailsByTitle.get(event.getDishTitle());
        DishTags tags = tagger.tag(event.getDishTitle(), details);
        for (Map.Entry<TasteDimension, String> tag : tags.asMap().entrySet()) {
            addScore(tag.getKey(), event.getMealIndex(), tag.getValue(), contribution);
            if (weight > 0) {
                addObservation(tag.getKey(), event.getMealIndex());
            }
        }
    }

    private void addScore(TasteDimension dimension, int mealIndex, String value,
                          double contribution) {
        merge(FrozenTasteAggregate.scoreKey(dimension, TasteModel.ALL_MEALS, value),
                contribution);
        if (mealIndex >= 0) {
            merge(FrozenTasteAggregate.scoreKey(dimension, mealIndex, value),
                    contribution);
        }
    }

    private void addObservation(TasteDimension dimension, int mealIndex) {
        bump(FrozenTasteAggregate.observationKey(dimension, TasteModel.ALL_MEALS));
        if (mealIndex >= 0) {
            bump(FrozenTasteAggregate.observationKey(dimension, mealIndex));
        }
    }

    private void bump(String key) {
        Integer current = observations.get(key);
        observations.put(key, (current == null ? 0 : current) + 1);
    }

    private void merge(String key, double delta) {
        Double current = scores.get(key);
        scores.put(key, (current == null ? 0.0 : current) + delta);
    }
}
