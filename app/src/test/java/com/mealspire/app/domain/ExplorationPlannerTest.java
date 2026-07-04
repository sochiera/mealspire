package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Cel eksploracji 2+1: najmniej zbadany wymiar i najsłabiej znana dozwolona
 * wartość — wybierane przez appkę, z poszanowaniem diety; pusty model nie
 * eksploruje (offline robi to sam).
 */
public class ExplorationPlannerTest {

    private static final long NOW = 1000L * 24 * 60 * 60 * 1000;

    private final ExplorationPlanner planner = new ExplorationPlanner();
    private final DishTagger tagger = new DishTagger();

    private TasteModel modelOf(String... likedDishes) {
        TasteEventLog log = TasteEventLog.empty();
        long ts = NOW;
        for (String dish : likedDishes) {
            log = log.append(new TasteEvent(TasteEvent.Type.LIKED, dish, 1, ts++));
        }
        return TasteModel.build(log, FrozenTasteAggregate.empty(), tagger, null, NOW);
    }

    @Test
    public void pustyModelNieDajeCeluEksploracji() {
        assertNull(planner.plan(modelOf(), DietConstraints.empty(), 1, new Random(1)));
        assertNull(planner.plan(null, DietConstraints.empty(), 1, new Random(1)));
    }

    @Test
    public void celOmijaWartosciKtoreUzytkownikJuzZna() {
        // Dużo kurczaka: eksploracja nie może celować w kurczaka.
        TasteModel model = modelOf("Kurczak z ryżem", "Kurczak pieczony",
                "Kurczak w curry");
        for (long seed = 0; seed < 10; seed++) {
            ExplorationPlanner.ExplorationGoal goal = planner.plan(
                    model, DietConstraints.empty(), 1, new Random(seed));
            assertNotNull(goal);
            if (goal.getDimension() == TasteDimension.BASE) {
                assertTrue(!"kurczak".equals(goal.getValue()));
            }
        }
    }

    @Test
    public void celRespektujeWykluczeniaDiety() {
        TasteModel model = modelOf("Omlet z warzywami", "Sałatka z jajkiem");
        DietConstraints veg = DietConstraints.of(Arrays.asList(
                DietConstraints.Exclusion.VEGETARIAN));
        for (long seed = 0; seed < 10; seed++) {
            ExplorationPlanner.ExplorationGoal goal = planner.plan(
                    model, veg, 1, new Random(seed));
            assertNotNull(goal);
            assertTrue("cel łamie dietę: " + goal.getValue(),
                    veg.allows(goal.getValue()));
        }
    }

    @Test
    public void zdanieMaStrukture2Plus1() {
        TasteModel model = modelOf("Kurczak z ryżem");
        ExplorationPlanner.ExplorationGoal goal = planner.plan(
                model, DietConstraints.empty(), 1, new Random(3));
        assertNotNull(goal);
        String sentence = goal.promptSentence();
        assertTrue(sentence.contains("Pierwsze dwie propozycje"));
        assertTrue(sentence.contains("Trzecią"));
        assertTrue(sentence.contains(goal.getValue()));
        assertTrue(sentence.contains("diety"));
    }

    @Test
    public void preferujeNajmniejZbadanyWymiar() {
        // Baza ma dużo obserwacji, kuchnia i charakter zero — cel nie powinien
        // wypaść w bazie.
        TasteModel model = modelOf("Kurczak z ryżem", "Wołowina duszona",
                "Łosoś z pieca", "Omlet", "Leczo z cieciorką");
        ExplorationPlanner.ExplorationGoal goal = planner.plan(
                model, DietConstraints.empty(), 1, new Random(5));
        assertNotNull(goal);
        assertTrue(goal.getDimension() == TasteDimension.CUISINE
                || goal.getDimension() == TasteDimension.CHARACTER);
    }

    @Test
    public void deterministycznyDlaTegoSamegoZiarna() {
        TasteModel model = modelOf("Kurczak z ryżem", "Zupa pomidorowa");
        ExplorationPlanner.ExplorationGoal first = planner.plan(
                model, DietConstraints.empty(), 1, new Random(7));
        ExplorationPlanner.ExplorationGoal second = planner.plan(
                model, DietConstraints.empty(), 1, new Random(7));
        assertEquals(first.getDimension(), second.getDimension());
        assertEquals(first.getValue(), second.getValue());
    }
}
