package com.mealspire.app.domain;

/** Trzyma bieżącą ankietę gustu między uruchomieniami (wznowienie po przerwie). */
public interface TasteSurveyStore {

    /** Zapisana ankieta albo null, gdy jeszcze żadnej nie było. */
    TasteSurvey load();

    void save(TasteSurvey survey);
}
