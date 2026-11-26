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

    /** Dziennik po kompakcji razem z agregatem — oba albo żaden (jeden zapis). */
    default void save(TasteEventLog log, FrozenTasteAggregate aggregate) {
        saveAggregate(aggregate);
        save(log);
    }

    /** Jak {@link #save(TasteEventLog, FrozenTasteAggregate)}, ale czeka na dysk. */
    default boolean saveDurably(TasteEventLog log, FrozenTasteAggregate aggregate) {
        saveAggregate(aggregate);
        return saveDurably(log);
    }
}
