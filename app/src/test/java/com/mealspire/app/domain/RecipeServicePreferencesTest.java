package com.mealspire.app.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecipeServicePreferencesTest {

    private static class CapturingClient implements ClaudeClient {
        String lastUser;

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            this.lastUser = userPrompt;
            return "Danie\nopis";
        }
    }

    @Test
    public void preferencesReachTheClientPrompt() throws Exception {
        CapturingClient client = new CapturingClient();
        RecipeService service = new RecipeService(client, new RecipePromptBuilder(),
                new RecipeTextParser());

        UserPreferences prefs = UserPreferences.empty()
                .withLike("kasza")
                .withDislike("grzyby");
        service.generateRecipe("Obiad", prefs);

        assertTrue(client.lastUser.contains("kasza"));
        assertTrue(client.lastUser.contains("grzyby"));
    }
}
