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
}
