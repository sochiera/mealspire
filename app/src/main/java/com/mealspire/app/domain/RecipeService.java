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
    private final ProposalPromptBuilder proposalPromptBuilder;
    private final ProposalParser proposalParser;
    private final ModifyRecipePromptBuilder modifyPromptBuilder;

    public RecipeService(ClaudeClient client, RecipePromptBuilder promptBuilder,
                         RecipeTextParser textParser) {
        this(client, promptBuilder, textParser, new ProposalPromptBuilder(),
                new ProposalParser(), new ModifyRecipePromptBuilder());
    }

    public RecipeService(ClaudeClient client, RecipePromptBuilder promptBuilder,
                         RecipeTextParser textParser,
                         ProposalPromptBuilder proposalPromptBuilder,
                         ProposalParser proposalParser,
                         ModifyRecipePromptBuilder modifyPromptBuilder) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.textParser = textParser;
        this.proposalPromptBuilder = proposalPromptBuilder;
        this.proposalParser = proposalParser;
        this.modifyPromptBuilder = modifyPromptBuilder;
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

    /** First step: a lightweight proposal (name, description, time, ingredients). */
    public DishProposal proposeDish(RecipeRequest request) throws IOException {
        String answer = client.complete(
                proposalPromptBuilder.systemPrompt(),
                proposalPromptBuilder.userPrompt(request));
        return proposalParser.parse(answer);
    }

    /** Second step: the full recipe for a dish the user accepted from a proposal. */
    public Recipe generateRecipeFor(String dishName, RecipeRequest request) throws IOException {
        String answer = client.complete(
                promptBuilder.systemPrompt(),
                promptBuilder.fullRecipePrompt(dishName, request));
        return textParser.parse(answer);
    }

    /** Revise an already-shown recipe with a free-text instruction from the user. */
    public Recipe modifyRecipe(Recipe current, String instruction) throws IOException {
        String answer = client.complete(
                modifyPromptBuilder.systemPrompt(),
                modifyPromptBuilder.userPrompt(current, instruction));
        return textParser.parse(answer);
    }
}
