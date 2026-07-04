package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Kontrastowe rundy quizu: każda rozstrzyga jeden wymiar gustu (baza /
 * charakter / kuchnia), bez powtórek między rundami, z bezwzględnym
 * poszanowaniem wykluczeń diety i deterministycznie dla ziarna.
 */
public class ContrastiveDishSamplerTest {

    private final ContrastiveDishSampler sampler = new ContrastiveDishSampler();
    private final DishTagger tagger = new DishTagger();

    private static Recipe[][] builtInPools() {
        Recipe[][] pools = new Recipe[BuiltInRecipes.mealCount()][];
        for (int i = 0; i < pools.length; i++) {
            pools[i] = BuiltInRecipes.forMeal(i);
        }
        return pools;
    }

    private List<List<Recipe>> sample(DietConstraints diet, long seed) {
        return sampler.sample(builtInPools(), diet, new Random(seed));
    }

    @Test
    public void trzyPelneRundyBezPowtorekTytulow() {
        List<List<Recipe>> rounds = sample(DietConstraints.empty(), 1);
        assertEquals(ContrastiveDishSampler.rounds(), rounds.size());
        Set<String> titles = new HashSet<>();
        for (List<Recipe> round : rounds) {
            assertEquals(ContrastiveDishSampler.ROUND_SIZE, round.size());
            for (Recipe recipe : round) {
                assertTrue("powtórka: " + recipe.getTitle(),
                        titles.add(recipe.getTitle().toLowerCase()));
            }
        }
    }

    @Test
    public void pierwszaRundaKontrastujeBaze() {
        List<String> meat = Arrays.asList("kurczak", "wieprzowina", "wołowina",
                "mięso mielone");
        List<String> meatless = Arrays.asList("jajka", "strączki", "warzywa", "nabiał");
        for (long seed = 0; seed < 5; seed++) {
            List<Recipe> round = sample(DietConstraints.empty(), seed).get(0);
            List<String> bases = new ArrayList<>();
            for (Recipe recipe : round) {
                bases.add(tagger.tag(recipe.getTitle(), recipe.getDetails())
                        .get(TasteDimension.BASE));
            }
            assertTrue("mięso w rundzie (seed " + seed + "): " + bases,
                    containsAny(bases, meat));
            assertTrue("ryba w rundzie (seed " + seed + "): " + bases,
                    bases.contains("ryba"));
            assertTrue("bezmięsne w rundzie (seed " + seed + "): " + bases,
                    containsAny(bases, meatless));
        }
    }

    @Test
    public void wykluczeniaDietyObowiazujaWRundach() {
        DietConstraints veg = DietConstraints.of(Collections.singletonList(
                DietConstraints.Exclusion.VEGETARIAN));
        for (long seed = 0; seed < 5; seed++) {
            for (List<Recipe> round : sample(veg, seed)) {
                assertEquals("runda ma być pełna mimo wykluczeń",
                        ContrastiveDishSampler.ROUND_SIZE, round.size());
                for (Recipe recipe : round) {
                    assertTrue("dieta złamana: " + recipe.getTitle(),
                            veg.allows(recipe.getTitle() + "\n" + recipe.getDetails()));
                }
            }
        }
    }

    @Test
    public void toSamoZiarnoDajeTeSameRundy() {
        List<List<Recipe>> first = sample(DietConstraints.empty(), 42);
        List<List<Recipe>> second = sample(DietConstraints.empty(), 42);
        for (int i = 0; i < first.size(); i++) {
            for (int j = 0; j < first.get(i).size(); j++) {
                assertEquals(first.get(i).get(j).getTitle(),
                        second.get(i).get(j).getTitle());
            }
        }
    }

    private static boolean containsAny(List<String> values, List<String> wanted) {
        for (String value : values) {
            if (wanted.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
