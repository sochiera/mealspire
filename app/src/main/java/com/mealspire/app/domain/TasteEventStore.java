package com.mealspire.app.domain;

/** Trwały magazyn dziennika zdarzeń gustu i zamrożonego agregatu po kompakcji. */
public interface TasteEventStore {

    TasteEventLog load();

    void save(TasteEventLog log);

    /**
     * Zapis, który mówi, czy dane naprawdę są trwałe (np. {@code commit()} zamiast
     * {@code apply()}); zapis wyborów z ankiety zatwierdza ją dopiero przy {@code true}.
     */
    default boolean saveDurably(TasteEventLog log) {
        save(log);
        return true;
    }

    FrozenTasteAggregate loadAggregate();

    void saveAggregate(FrozenTasteAggregate aggregate);
}
