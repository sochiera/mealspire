package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wymiarowy model gustu: wagi zdarzeń rozpisane na wymiary przez tagger,
 * wygaszanie wykładnicze (półokres 60 dni), profile per slot i łączne,
 * sufit sygnału ujemnego oraz masa jawnych sygnałów.
 */
public class TasteModelTest {

    private static final long DAY = 24 * 60 * 60 * 1000L;
    private static final long NOW = 1000 * DAY;

    private final DishTagger tagger = new DishTagger();

    private static TasteEvent event(TasteEvent.Type type, String dish, int meal, long ts) {
        return new TasteEvent(type, dish, meal, ts);
    }

    private TasteModel model(TasteEvent... events) {
        return TasteModel.build(new TasteEventLog(Arrays.asList(events)),
                FrozenTasteAggregate.empty(), tagger, null, NOW);
    }

    @Test
    public void lajkiSumujaSieWWymiarachPerSlotILacznie() {
        TasteModel model = model(
                event(TasteEvent.Type.LIKED, "Kurczak z ryżem", 1, NOW),
                event(TasteEvent.Type.LIKED, "Kurczak pieczony", 1, NOW),
                event(TasteEvent.Type.LIKED, "Omlet z serem", 0, NOW));

        assertEquals(6.0, model.score(TasteDimension.BASE, "kurczak", 1), 0.001);
        assertEquals(6.0, model.score(TasteDimension.BASE, "kurczak",
                TasteModel.ALL_MEALS), 0.001);
        // Slot śniadania nie widzi obiadowego kurczaka.
        assertEquals(0.0, model.score(TasteDimension.BASE, "kurczak", 0), 0.001);
        assertEquals(3.0, model.score(TasteDimension.BASE, "jajka", 0), 0.001);
    }

    @Test
    public void wygaszaniePolowiWkladPoPolokresie() {
        TasteModel model = model(
                event(TasteEvent.Type.LIKED, "Kurczak z ryżem", 1, NOW - 60 * DAY));
        assertEquals(1.5, model.score(TasteDimension.BASE, "kurczak", 1), 0.01);
    }

    @Test
    public void sufitUjemnyNieBanujeSkladnika() {
        TasteEvent[] rerolls = new TasteEvent[20];
        for (int i = 0; i < rerolls.length; i++) {
            rerolls[i] = event(TasteEvent.Type.REROLLED, "Kurczak z ryżem", 1, NOW);
        }
        TasteModel model = model(rerolls);
        // 20 × (−0.3) = −6, ale sufit trzyma wynik na −1.
        assertEquals(TasteModel.MIN_VALUE_SCORE,
                model.score(TasteDimension.BASE, "kurczak", 1), 0.001);
    }

    @Test
    public void topValuesZwracaTylkoDodatnieodNajmocniejszych() {
        TasteModel model = model(
                event(TasteEvent.Type.LIKED, "Kurczak z ryżem", 1, NOW),
                event(TasteEvent.Type.LIKED, "Kurczak pieczony", 1, NOW),
                event(TasteEvent.Type.RECIPE_VIEWED, "Łosoś z piekarnika", 1, NOW),
                event(TasteEvent.Type.REROLLED, "Gulasz wieprzowy", 1, NOW));

        List<String> top = model.topValues(TasteDimension.BASE, 1, 5);
        assertEquals(Arrays.asList("kurczak", "ryba"), top);
    }

    @Test
    public void obserwacjeLiczaTylkoPozytywneZdarzenia() {
        TasteModel model = model(
                event(TasteEvent.Type.LIKED, "Kurczak z ryżem", 1, NOW),
                event(TasteEvent.Type.REROLLED, "Kurczak w curry", 1, NOW));
        assertEquals(1, model.observationCount(TasteDimension.BASE, 1));
        assertEquals(1, model.totalObservations(1)
                - model.observationCount(TasteDimension.CUISINE, 1)
                - model.observationCount(TasteDimension.CHARACTER, 1));
    }

    @Test
    public void masaJawnychSygnalowNieWygasa() {
        TasteModel model = model(
                event(TasteEvent.Type.LIKED, "Kurczak z ryżem", 1, NOW - 600 * DAY),
                event(TasteEvent.Type.RECIPE_VIEWED, "Omlet", 0, NOW));
        // Lajk sprzed 10 półokresów nadal liczy się do masy (bez dyskonta);
        // RECIPE_VIEWED jest dorozumiany i do masy nie wchodzi.
        assertEquals(TasteEvent.Type.LIKED.weight(),
                model.explicitPositiveMass(), 0.001);
    }

    @Test
    public void szczegolyDaniaPomagajaTagowacTytulyBezSlowKluczowych() {
        Map<String, String> details = new HashMap<>();
        details.put("Miska mocy", "Składniki: ciecierzyca, papryka, ryż.");
        TasteModel model = TasteModel.build(
                new TasteEventLog(Collections.singletonList(
                        event(TasteEvent.Type.LIKED, "Miska mocy", 1, NOW))),
                FrozenTasteAggregate.empty(), tagger, details, NOW);
        assertEquals(3.0, model.score(TasteDimension.BASE, "strączki", 1), 0.001);
    }

    @Test
    public void zamrozonyAgregatWchodziZDyskontem() {
        Map<String, Double> scores = new HashMap<>();
        scores.put(FrozenTasteAggregate.scoreKey(TasteDimension.BASE,
                TasteModel.ALL_MEALS, "ryba"), 4.0);
        Map<String, Integer> observations = new HashMap<>();
        observations.put(FrozenTasteAggregate.observationKey(TasteDimension.BASE,
                TasteModel.ALL_MEALS), 2);
        FrozenTasteAggregate frozen = new FrozenTasteAggregate(
                NOW - 60 * DAY, scores, observations, 6.0);

        TasteModel model = TasteModel.build(TasteEventLog.empty(), frozen,
                tagger, null, NOW);

        assertEquals(2.0, model.score(TasteDimension.BASE, "ryba",
                TasteModel.ALL_MEALS), 0.01);
        assertEquals(2, model.observationCount(TasteDimension.BASE,
                TasteModel.ALL_MEALS));
        assertEquals(6.0, model.explicitPositiveMass(), 0.001);
    }
}
