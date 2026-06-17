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
        String answer = client.complete(
                promptBuilder.systemPrompt(),
                promptBuilder.userPrompt(mealType));
        return textParser.parse(answer);
    }
}
