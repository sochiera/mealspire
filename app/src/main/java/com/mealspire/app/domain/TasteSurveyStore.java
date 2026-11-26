package com.mealspire.app.domain;

/** Trzyma bieżącą ankietę gustu między uruchomieniami (wznowienie po przerwie). */
public interface TasteSurveyStore {

    /** Zapisana ankieta albo null, gdy jeszcze żadnej nie było. */
    TasteSurvey load();

    void save(TasteSurvey survey);

    /**
     * Zapis, który mówi, czy dane naprawdę są trwałe (np. {@code commit()} zamiast
     * {@code apply()}); zapis wyborów z ankiety zatwierdza ją dopiero przy {@code true}.
     */
    default boolean saveDurably(TasteSurvey survey) {
        save(survey);
        return true;
    }
}
