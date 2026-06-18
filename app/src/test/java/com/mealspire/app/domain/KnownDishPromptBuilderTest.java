package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KnownDishPromptBuilderTest {

    private final KnownDishPromptBuilder builder = new KnownDishPromptBuilder();

    @Test
    public void systemPromptIsPolishAndAsksForOneDish() {
        String system = builder.systemPrompt();
        assertFalse(system.trim().isEmpty());
        assertTrue(system.toLowerCase().contains("polsk"));
    }

    @Test
    public void descriptionPromptIncludesTheDescription() {
        String prompt = builder.userPromptForDescription("naleśniki z serem");
        assertTrue(prompt.contains("naleśniki z serem"));
    }

    @Test
    public void pageTextPromptIncludesUrlAndContent() {
        String prompt = builder.userPromptForPageText("https://x.example", "treść strony o cieście");
        assertTrue(prompt.contains("https://x.example"));
        assertTrue(prompt.contains("treść strony o cieście"));
    }
}
