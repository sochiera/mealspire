package com.mealspire.app.domain;

/** Persists the {@link DishReactionLog} between launches. */
public interface DishReactionStore {
    DishReactionLog load();

    void save(DishReactionLog log);

    /**
     * Zapis, który mówi, czy dane naprawdę są trwałe (np. {@code commit()} zamiast
     * {@code apply()}); zapis wyborów z ankiety zatwierdza ją dopiero przy {@code true}.
     */
    default boolean saveDurably(DishReactionLog log) {
        save(log);
        return true;
    }
}
