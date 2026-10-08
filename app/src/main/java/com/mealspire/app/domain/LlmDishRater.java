package com.mealspire.app.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ocena kandydatów jednym wywołaniem LLM (po stronie backendu). Zwraca tylko
 * oceny kandydatów z listy, bez duplikatów i bez naruszeń diety.
 */
public final class LlmDishRater implements DishRater {

    private final LlmClient llmClient;
    private final DishRatingPromptBuilder promptBuilder;
    private final DishRatingParser parser;

    public LlmDishRater(LlmClient llmClient, DishRatingPromptBuilder promptBuilder,
                        DishRatingParser parser) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
    }

    @Override
    public List<DishRating> rate(String mealType, List<DishReaction> reactionsNewestFirst,
                                 List<DishProposal> candidates, DietConstraints diet, long now)
            throws IOException {
        String answer = llmClient.complete(promptBuilder.systemPrompt(),
                promptBuilder.userPrompt(mealType, reactionsNewestFirst, candidates, now));
        Map<String, DishProposal> known = new HashMap<>();
        for (DishProposal candidate : candidates) {
            known.put(candidate.getName().trim().toLowerCase(), candidate);
        }
        List<DishRating> result = new ArrayList<>();
        for (DishRating rating : parser.parse(answer)) {
            DishProposal candidate = known.remove(rating.getDish().toLowerCase());
            if (candidate == null || (diet != null
                    && !diet.allows(candidate.getName() + "\n" + candidate.getDescription()))) {
                continue;
            }
            result.add(new DishRating(candidate.getName(), rating.getScore(),
                    rating.getReason()));
        }
        return result;
    }
}
