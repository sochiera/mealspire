package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class PromptAvoidRecentTest {

    private final RecipePromptBuilder builder = new RecipePromptBuilder();

    @Test
    public void promptListsRecentDishesToAvoid() {
        String prompt = builder.userPrompt("Obiad", UserPreferences.empty(),
                Arrays.asList("Spaghetti", "Pierogi"));
        assertTrue(prompt.contains("Spaghetti"));
        assertTrue(prompt.contains("Pierogi"));
    }

    @Test
    public void emptyRecentListAddsNoAvoidClause() {
        String prompt = builder.userPrompt("Obiad", UserPreferences.empty(),
                Collections.<String>emptyList());
        assertFalse(prompt.toLowerCase().contains("ostatnio"));
    }
}
