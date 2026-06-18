package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecipePromptBuilderTest {

    private final RecipePromptBuilder builder = new RecipePromptBuilder();

    @Test
    public void systemPromptAsksForPolishFamilyFriendlyRecipes() {
        String system = builder.systemPrompt();
        assertTrue(system.toLowerCase().contains("polsk"));
        assertFalse(system.trim().isEmpty());
    }

    @Test
    public void userPromptIncludesMealType() {
        String user = builder.userPrompt("Obiad");
        assertTrue(user.contains("Obiad"));
    }

    @Test
    public void fullRecipePromptPinsDishNameAndCarriesChoices() {
        com.mealspire.app.domain.RecipeRequest request =
                new com.mealspire.app.domain.RecipeRequest("Obiad", UserPreferences.empty(),
                        java.util.Collections.<String>emptyList(),
                        java.util.Arrays.asList("dla 4 osób"));

        String prompt = builder.fullRecipePrompt("Leczo", request);

        assertTrue(prompt.contains("Leczo"));
        assertTrue(prompt.contains("dla 4 osób"));
        assertTrue(prompt.toLowerCase().contains("sposób przygotowania"));
    }
}
