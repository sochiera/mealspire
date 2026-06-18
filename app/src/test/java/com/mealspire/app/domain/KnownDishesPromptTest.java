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
    public void fourArgConstructorStillHasNoKnownDishes() {
        RecipeRequest request = new RecipeRequest("Obiad", UserPreferences.empty(), null, null);
        assertTrue(request.getKnownDishes().isEmpty());
    }
}
