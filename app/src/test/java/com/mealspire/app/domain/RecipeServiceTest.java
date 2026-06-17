package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecipeServiceTest {

    /** Fake client that records the prompts and returns a canned response. */
    private static class FakeClaudeClient implements ClaudeClient {
        String lastSystem;
        String lastUser;
        String response;

        FakeClaudeClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            this.lastSystem = systemPrompt;
            this.lastUser = userPrompt;
            return response;
        }
    }

    @Test
    public void generatesRecipeFromClientResponse() throws Exception {
        FakeClaudeClient client = new FakeClaudeClient(
                "Placki ziemniaczane\n\nZetrzyj ziemniaki, dodaj jajko i mąkę, smaż na oleju.");
        RecipeService service = new RecipeService(client, new RecipePromptBuilder(),
                new RecipeTextParser());

        Recipe recipe = service.generateRecipe("Obiad");

        assertEquals("Placki ziemniaczane", recipe.getTitle());
        assertEquals("Obiad i ziemniaki w zapytaniu", true,
                client.lastUser.contains("Obiad"));
    }

    @Test
    public void passesSystemPromptToClient() throws Exception {
        FakeClaudeClient client = new FakeClaudeClient("Tytuł\nszczegóły");
        RecipeService service = new RecipeService(client, new RecipePromptBuilder(),
                new RecipeTextParser());

        service.generateRecipe("Śniadanie");

        assertEquals(new RecipePromptBuilder().systemPrompt(), client.lastSystem);
    }
}
