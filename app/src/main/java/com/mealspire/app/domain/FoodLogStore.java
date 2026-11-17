package com.mealspire.app.domain;

/** Trwały zapis dziennika „co dzisiaj jedliście?". */
public interface FoodLogStore {

    FoodLog load();

    void save(FoodLog log);
}
