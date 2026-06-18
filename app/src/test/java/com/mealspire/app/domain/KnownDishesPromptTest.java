package com.mealspire.app.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class KnownDishesPromptTest {

    @Test
    public void promptIncludesKnownDishesFromBase() {
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(),
                null, null, Arrays.asList("Bigos", "Pierogi ruskie"));
        String prompt = new RecipePromptBuilder().userPrompt(request);
        assertTrue(prompt.contains("Bigos"));
        assertTrue(prompt.contains("Pierogi ruskie"));
    }

    @Test
    public void promptLetsModelReuseAKnownDishOrInventANewOne() {
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(),
                null, null, Arrays.asList("Bigos"));
        String prompt = new RecipePromptBuilder().userPrompt(request).toLowerCase();
        // The model may simply hand back one of the saved dishes...
        assertTrue(prompt.contains("wybrać jedno z nich"));
        // ...or propose a brand new dish.
        assertTrue(prompt.contains("coś nowego"));
    }

    @Test
    public void fourArgConstructorStillHasNoKnownDishes() {
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(), null, null);
        assertTrue(request.getKnownDishes().isEmpty());
    }
}
