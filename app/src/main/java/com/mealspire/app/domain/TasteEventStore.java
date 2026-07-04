package com.mealspire.app.domain;

/** Trwały magazyn dziennika zdarzeń gustu. */
public interface TasteEventStore {

    TasteEventLog load();

    void save(TasteEventLog log);
}
