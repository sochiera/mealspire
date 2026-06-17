package com.mealspire.app.domain;

import java.io.IOException;

/**
 * Coordinates recipe generation: builds prompts, calls the LLM client, and
 * parses the answer into a {@link Recipe}. Holds no Android dependencies.
 */
public final class RecipeService {

    private final ClaudeClient client;
    private final RecipePromptBuilder promptBuilder;
    private final RecipeTextParser textParser;

    public RecipeService(ClaudeClient client, RecipePromptBuilder promptBuilder,
                         RecipeTextParser textParser) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.textParser = textParser;
    }

    public Recipe generateRecipe(String mealType) throws IOException {
        return generateRecipe(mealType, UserPreferences.empty());
    }

    public Recipe generateRecipe(String mealType, UserPreferences preferences) throws IOException {
        return generateRecipe(mealType, preferences, java.util.Collections.<String>emptyList());
    }

    public Recipe generateRecipe(String mealType, UserPreferences preferences,
                                 Iterable<String> recentDishesToAvoid) throws IOException {
        java.util.List<String> recent = new java.util.ArrayList<>();
        for (String dish : recentDishesToAvoid) {
            recent.add(dish);
        }
        return generateRecipe(new RecipeRequest(mealType, preferences, recent,
                java.util.Collections.<String>emptyList()));
    }

    public Recipe generateRecipe(RecipeRequest request) throws IOException {
        String answer = client.complete(
                promptBuilder.systemPrompt(),
                promptBuilder.userPrompt(request));
        return textParser.parse(answer);
    }
}
