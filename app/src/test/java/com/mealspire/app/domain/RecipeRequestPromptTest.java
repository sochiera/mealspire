package com.mealspire.app.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class RecipeRequestPromptTest {

    @Test
    public void promptFromRequestIncludesEverything() {
        UserPreferences prefs = UserPreferences.empty()
                .withLike("kasza")
                .withDislike("grzyby");
        RecipeRequest request = new RecipeRequest("Obiad", prefs,
                Arrays.asList("Pizza"), Arrays.asList("danie wegetariańskie"));

        String prompt = new RecipePromptBuilder().userPrompt(request);

        assertTrue(prompt.contains("Obiad"));
        assertTrue(prompt.contains("kasza"));
        assertTrue(prompt.contains("grzyby"));
        assertTrue(prompt.contains("Pizza"));
        assertTrue(prompt.contains("danie wegetariańskie"));
    }

    @Test
    public void serviceUsesRequestPrompt() throws Exception {
        final String[] captured = new String[1];
        ClaudeClient client = (system, user) -> {
            captured[0] = user;
            return "Danie\nopis";
        };
        RecipeService service = new RecipeService(client, new RecipePromptBuilder(),
                new RecipeTextParser());

        RecipeRequest request = new RecipeRequest("Śniadanie", UserPreferences.empty(),
                null, Arrays.asList("danie wysokobiałkowe"));
        service.generateRecipe(request);

        assertTrue(captured[0].contains("danie wysokobiałkowe"));
    }
}
