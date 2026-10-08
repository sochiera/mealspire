package com.mealspire.app.domain;

/** Ocena kandydata przez LLM: nazwa dania, 0–10 i jednozdaniowy powód. */
public final class DishRating {

    private final String dish;
    private final int score;
    private final String reason;

    public DishRating(String dish, int score, String reason) {
        this.dish = dish == null ? "" : dish.trim();
        this.score = Math.max(0, Math.min(10, score));
        this.reason = reason == null ? "" : reason.trim();
    }

    public String getDish() {
        return dish;
    }

    public int getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}
