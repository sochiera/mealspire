package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PreferenceAwarePromptTest {

    private final RecipePromptBuilder builder = new RecipePromptBuilder();

    @Test
    public void promptMentionsLikesAndDislikes() {
        UserPreferences prefs = UserPreferences.empty()
                .withLike("makaron")
                .withDislike("ostre przyprawy");

        String prompt = builder.userPrompt("Obiad", prefs);

        assertTrue(prompt.contains("Obiad"));
        assertTrue(prompt.contains("makaron"));
        assertTrue(prompt.contains("ostre przyprawy"));
    }

    @Test
    public void promptWithEmptyPreferencesHasNoPreferenceClause() {
        String prompt = builder.userPrompt("Kolacja", UserPreferences.empty());
        assertTrue(prompt.contains("Kolacja"));
        assertFalse(prompt.toLowerCase().contains("lubi"));
        assertFalse(prompt.toLowerCase().contains("unika"));
    }
}
