package com.mealspire.app.domain;

import java.io.IOException;

/**
 * Turns a user's "I already know and like this dish" input (a link or a short
 * description) into a {@link CookbookEntry}, using the LLM to normalize the dish
 * name and produce a concise recipe. Network access is behind {@link PageFetcher}
 * and {@link ClaudeClient} so this orchestration is fully unit-testable.
 */
public final class KnownDishImporter {

    private static final String SOURCE_DESCRIPTION = "opis";

    private final ClaudeClient client;
    private final PageFetcher pageFetcher;
    private final KnownDishPromptBuilder promptBuilder;
    private final RecipeTextParser textParser;

    public KnownDishImporter(ClaudeClient client, PageFetcher pageFetcher,
                             KnownDishPromptBuilder promptBuilder, RecipeTextParser textParser) {
        this.client = client;
        this.pageFetcher = pageFetcher;
        this.promptBuilder = promptBuilder;
        this.textParser = textParser;
    }

    public CookbookEntry importDish(String rawInput) throws IOException {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new IOException("Podaj link albo krótki opis dania.");
        }
        String input = rawInput.trim();

        String userPrompt;
        String source;
        if (KnownDishInput.isUrl(input)) {
            source = input;
            String pageText = "";
            try {
                pageText = pageFetcher.fetchText(input);
            } catch (IOException e) {
                pageText = "";
            }
            userPrompt = pageText == null || pageText.trim().isEmpty()
                    ? promptBuilder.userPromptForDescription(input)
                    : promptBuilder.userPromptForPageText(input, pageText);
        } else {
            source = SOURCE_DESCRIPTION;
            userPrompt = promptBuilder.userPromptForDescription(input);
        }

        String answer = client.complete(promptBuilder.systemPrompt(), userPrompt);
        Recipe recipe = textParser.parse(answer);
        return new CookbookEntry(recipe.getTitle(), recipe.getDetails(), source);
    }
}
