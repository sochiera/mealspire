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
        List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), null, 20, new Random(5));
        Map<Integer, Integer> perMeal = new HashMap<>();
        int contrasting = 0;
        for (int i = 0; i < pairs.size(); i++) {
            int indexInMeal = perMeal.merge(pairs.get(i).getMeal(), 1, Integer::sum) - 1;
            TasteDimension dim = TasteSurveyPlanner.dimensionFor(indexInMeal);
            String a = tagger.tag(pairs.get(i).getDishA(), details.get(pairs.get(i).getDishA()))
                    .get(dim);
            String b = tagger.tag(pairs.get(i).getDishB(), details.get(pairs.get(i).getDishB()))
                    .get(dim);
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

    @Test
    public void obaDaniaWParzeSaZTejSamejPoryPosilkuAPoryKraza() {
        Recipe[][] pools = builtInPools();
        List<TasteSurvey.Pair> pairs = planner.plan(pools, null, 30, new Random(9));
        Map<Integer, Integer> perMeal = new HashMap<>();
        for (TasteSurvey.Pair pair : pairs) {
            int meal = pair.getMeal();
            assertTrue("para bez pory posiłku", meal >= 0 && meal < pools.length);
            assertTrue(pair.getDishA(), inPool(pools[meal], pair.getDishA()));
            assertTrue(pair.getDishB(), inPool(pools[meal], pair.getDishB()));
            perMeal.merge(meal, 1, Integer::sum);
        }
        assertEquals(10, (int) perMeal.get(0));
        assertEquals(10, (int) perMeal.get(1));
        assertEquals(10, (int) perMeal.get(2));
    }

    @Test
    public void wyczerpanaPoraOddajeMiejsceInnym() {
        Recipe[][] pools = {{new Recipe("A", ""), new Recipe("B", "")},
                {new Recipe("C", ""), new Recipe("D", ""), new Recipe("E", ""),
                        new Recipe("F", "")}};
        List<TasteSurvey.Pair> pairs = planner.plan(pools, null, 6, new Random(4));
        assertTrue(pairs.size() > 2);
        int fromFirst = 0;
        for (TasteSurvey.Pair pair : pairs) {
            fromFirst += pair.getMeal() == 0 ? 1 : 0;
        }
        assertEquals(1, fromFirst); // jedyna możliwa para A/B
    }

    @Test
    public void domyslnaAnkietaJestWyraznieDluzszaNiz20Par() {
        assertTrue(TasteSurvey.DEFAULT_PAIRS > 20);
        List<TasteSurvey.Pair> pairs = planner.plan(builtInPools(), null,
                TasteSurvey.DEFAULT_PAIRS, new Random(6));
        assertEquals(TasteSurvey.DEFAULT_PAIRS, pairs.size());
    }

    @Test
    public void poraPosilkuParyPrzezywaZapisIStaryZapisJestCzytelny() {
        TasteSurveySerializer serializer = new TasteSurveySerializer();
        TasteSurvey survey = TasteSurvey.start(Collections.singletonList(
                new TasteSurvey.Pair("Owsianka", "Jajecznica", 0)), "[]");
        assertEquals(0, serializer.fromJson(serializer.toJson(survey)).current().getMeal());
        TasteSurvey legacy = serializer.fromJson(
                "{\"pairs\":[[\"Owsianka\",\"Gulasz\"]],\"answers\":[-1]}");
        assertEquals(TasteSurvey.ANY_MEAL, legacy.current().getMeal());
    }

    private static boolean inPool(Recipe[] pool, String title) {
        for (Recipe recipe : pool) {
            if (recipe.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }
}
