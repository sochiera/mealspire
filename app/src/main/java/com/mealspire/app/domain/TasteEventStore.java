package com.mealspire.app.domain;

/** Trwały magazyn dziennika zdarzeń gustu i zamrożonego agregatu po kompakcji. */
public interface TasteEventStore {

    TasteEventLog load();

    void save(TasteEventLog log);

    FrozenTasteAggregate loadAggregate();

    void saveAggregate(FrozenTasteAggregate aggregate);
}
