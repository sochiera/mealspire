package com.mealspire.app.domain;

import java.io.IOException;

/**
 * Minimal abstraction over a single-turn LLM completion, so the recipe logic
 * can be unit-tested without any network calls.
 */
public interface ClaudeClient {
    /**
     * Sends the prompts to the model and returns the assistant's plain-text answer.
     *
     * @throws IOException on transport or API errors
     */
    String complete(String systemPrompt, String userPrompt) throws IOException;
}
