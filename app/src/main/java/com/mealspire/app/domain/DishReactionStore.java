package com.mealspire.app.domain;

/** Persists the {@link DishReactionLog} between launches. */
public interface DishReactionStore {
    DishReactionLog load();

    void save(DishReactionLog log);
}
