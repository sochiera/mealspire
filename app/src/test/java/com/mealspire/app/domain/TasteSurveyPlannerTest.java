package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TasteSurveyPlannerTest {

    private static Recipe[][] builtInPools() {
        Recipe[][] pools = new Recipe[BuiltInRecipes.mealCount()][];
        for (int i = 0; i < pools.length; i++) {
            pools[i] = BuiltInRecipes.forMeal(i);
        }
        return pools;
    }

    private final TasteSurveyPlanner planner = new TasteSurveyPlanner();

    @Test
    public void ukladaZadanaLiczbeRoznychParBezPowtorzen() {
        for (int count : new int[]{5, 20, 40}) {
            List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), null, count,
                    new Random(7));
            assertEquals(count, pairs.size());
            Set<String> keys = new HashSet<>();
            for (TasteSurvey.Pair pair : pairs) {
                assertNotEquals(pair.getDishA(), pair.getDishB());
                String a = pair.getDishA();
                String b = pair.getDishB();
                assertTrue("para powtórzona: " + a + " / " + b,
                        keys.add(a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a));
            }
        }
    }

    @Test
    public void kolejnaParaZawszeMaInneDania() {
        List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), null, 40, new Random(3));
        for (int i = 1; i < pairs.size(); i++) {
            Set<String> previous = new HashSet<>();
            previous.add(pairs.get(i - 1).getDishA());
            previous.add(pairs.get(i - 1).getDishB());
            assertFalse(previous.contains(pairs.get(i).getDishA()));
            assertFalse(previous.contains(pairs.get(i).getDishB()));
        }
    }

    @Test
    public void dlugaAnkietaPrzegladaPuleRownomiernie() {
        // 30 par = 60 miejsc na 60 dań: każde danie dokładnie raz.
        List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), null, 30, new Random(11));
        Map<String, Integer> uses = new HashMap<>();
        for (TasteSurvey.Pair pair : pairs) {
            uses.merge(pair.getDishA(), 1, Integer::sum);
            uses.merge(pair.getDishB(), 1, Integer::sum);
        }
        assertEquals(60, uses.size());
        assertEquals(Collections.singleton(1), new HashSet<>(uses.values()));
    }

    @Test
    public void wiekszoscParKontrastujeWWymiarzeGustu() {
        DishTagger tagger = new DishTagger();
        Map<String, String> details = BuiltInRecipes.detailsByTitle(null);
        TasteDimension[] dims = {TasteDimension.BASE, TasteDimension.CHARACTER,
                TasteDimension.CUISINE};
        List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), null, 20, new Random(5));
        int contrasting = 0;
        for (int i = 0; i < pairs.size(); i++) {
            String a = tagger.tag(pairs.get(i).getDishA(), details.get(pairs.get(i).getDishA()))
                    .get(dims[i % 3]);
            String b = tagger.tag(pairs.get(i).getDishB(), details.get(pairs.get(i).getDishB()))
                    .get(dims[i % 3]);
            if (a != null && b != null && !a.equals(b)) {
                contrasting++;
            }
        }
        assertTrue("kontrastowych par: " + contrasting, contrasting >= 15);
    }

    @Test
    public void wykluczeniaDietyObowiazujaBezwzglednie() {
        DietConstraints veg = DietConstraints.of(Collections.singletonList(
                DietConstraints.Exclusion.VEGETARIAN));
        Map<String, String> details = BuiltInRecipes.detailsByTitle(null);
        List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), veg, 40, new Random(1));
        assertFalse(pairs.isEmpty());
        for (TasteSurvey.Pair pair : pairs) {
            assertTrue(pair.getDishA(), veg.allows(pair.getDishA() + "\n"
                    + details.get(pair.getDishA())));
            assertTrue(pair.getDishB(), veg.allows(pair.getDishB() + "\n"
                    + details.get(pair.getDishB())));
        }
    }

    @Test
    public void malaPulaDajeMniejParZamiastPowtorek() {
        Recipe[][] pools = {{new Recipe("A", ""), new Recipe("B", ""), new Recipe("C", ""),
                new Recipe("D", "")}};
        List<TasteSurvey.Pair> pairs = planner.plan(pools, null, 40, new Random(2));
        assertTrue(pairs.size() <= 6); // C(4,2)
        assertFalse(pairs.isEmpty());
        assertTrue(planner.plan(new Recipe[0][], null, 10, new Random(2)).isEmpty());
    }
}
