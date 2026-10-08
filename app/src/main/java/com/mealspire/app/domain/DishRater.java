package com.mealspire.app.domain;

import java.io.IOException;
import java.util.List;

/**
 * Jedno pytanie do LLM: „osoba z takimi reakcjami — czy polubi te dania?".
 * Na telefonie realizuje je backend VPS, na serwerze {@link LlmDishRater}.
 */
public interface DishRater {
    /**
     * @param reactionsNewestFirst ograniczona lista jawnych reakcji, najnowsza pierwsza
     * @param candidates           kandydaci: nazwa i krótki opis/skład
     * @param diet                 twardy filtr; oceny naruszeń nie wracają
     * @throws IOException brak sieci, błąd serwera albo nieczytelny JSON modelu
     */
    List<DishRating> rate(String mealType, List<DishReaction> reactionsNewestFirst,
                          List<DishProposal> candidates, DietConstraints diet, long now)
            throws IOException;
}
