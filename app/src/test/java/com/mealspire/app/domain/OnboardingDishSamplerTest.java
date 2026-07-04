package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dobór trójek dań do rund onboardingu: 3 rundy × 3 dania, bez powtórek
 * między rundami, w każdej rundzie po jednym daniu z każdej pory posiłku
 * (śniadanie/obiad/kolacja), deterministycznie przy wstrzykniętym seedzie.
 */
public class OnboardingDishSamplerTest {

    private final OnboardingDishSampler sampler = new OnboardingDishSampler();

    private static Recipe[] pool(String prefix, int size) {
        Recipe[] recipes = new Recipe[size];
        for (int i = 0; i < size; i++) {
            recipes[i] = new Recipe(prefix + " " + i, "Składniki: coś.\n\nZrób coś.");
        }
        return recipes;
    }

    private static Recipe[][] pools() {
        return new Recipe[][]{pool("Śniadanie", 8), pool("Obiad", 8), pool("Kolacja", 8)};
    }

    @Test
    public void trzyRundyPoTrzyDania() {
        List<List<Recipe>> rounds = sampler.sample(pools(), 3, new Random(1));
        assertEquals(3, rounds.size());
        for (List<Recipe> round : rounds) {
            assertEquals(3, round.size());
        }
    }

    @Test
    public void bezPowtorekMiedzyRundami() {
        List<List<Recipe>> rounds = sampler.sample(pools(), 3, new Random(2));
        Set<String> titles = new HashSet<>();
        for (List<Recipe> round : rounds) {
            for (Recipe recipe : round) {
                assertTrue("duplikat: " + recipe.getTitle(),
                        titles.add(recipe.getTitle().toLowerCase()));
            }
        }
        assertEquals(9, titles.size());
    }

    @Test
    public void kazdaRundaMaDaniaZRoznychPorPosilku() {
        List<List<Recipe>> rounds = sampler.sample(pools(), 3, new Random(3));
        for (List<Recipe> round : rounds) {
            Set<String> mealsInRound = new HashSet<>();
            for (Recipe recipe : round) {
                mealsInRound.add(recipe.getTitle().split(" ")[0]);
            }
            assertEquals(3, mealsInRound.size());
        }
    }

    @Test
    public void tenSamSeedDajeTeSameRundy() {
        List<List<Recipe>> first = sampler.sample(pools(), 3, new Random(42));
        List<List<Recipe>> second = sampler.sample(pools(), 3, new Random(42));
        for (int r = 0; r < 3; r++) {
            for (int i = 0; i < 3; i++) {
                assertEquals(first.get(r).get(i).getTitle(),
                        second.get(r).get(i).getTitle());
            }
        }
    }

    @Test
    public void dzialaNaPrawdziwejPuliWbudowanychDan() {
        Recipe[][] builtIn = new Recipe[BuiltInRecipes.mealCount()][];
        for (int i = 0; i < builtIn.length; i++) {
            builtIn[i] = BuiltInRecipes.forMeal(i);
        }
        List<List<Recipe>> rounds = sampler.sample(builtIn, 3, new Random(7));
        Set<String> titles = new HashSet<>();
        for (List<Recipe> round : rounds) {
            assertEquals(3, round.size());
            for (Recipe recipe : round) {
                assertTrue(titles.add(recipe.getTitle().toLowerCase()));
            }
        }
    }
}
