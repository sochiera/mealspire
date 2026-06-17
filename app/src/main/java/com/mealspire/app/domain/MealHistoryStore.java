package com.mealspire.app.domain;

/**
 * Persists {@link MealHistory} between launches and across screen rotation.
 */
public interface MealHistoryStore {
    MealHistory load();

    void save(MealHistory history);
}
