package com.mealspire.app.domain;

/** Trwały magazyn lokalnych liczników uczenia (nic nie opuszcza telefonu). */
public interface LearningStatsStore {

    LearningStats load();

    void save(LearningStats stats);
}
