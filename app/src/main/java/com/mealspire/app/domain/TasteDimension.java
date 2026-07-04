package com.mealspire.app.domain;

/**
 * Wymiary, w których system uczy się gustu — zamiast płaskiego worka słów
 * każde danie opisujemy bazą (główny składnik), stylem kuchni i charakterem.
 * Wysiłek/czas nie jest wymiarem uczonym: pochodzi wprost z deklaracji
 * użytkownika (pytanie o czas) i z pola „Czas" propozycji.
 */
public enum TasteDimension {
    /** Główny składnik: kurczak, wieprzowina, ryba, jaja, strączki… */
    BASE("bazą"),
    /** Styl kuchni: polska, włoska, azjatycka… */
    CUISINE("kuchnią"),
    /** Charakter dania: zupa, zapiekanka, sałatka, kanapki… */
    CHARACTER("charakterem");

    private final String instrumental;

    TasteDimension(String instrumental) {
        this.instrumental = instrumental;
    }

    /** Narzędnik do zdań promptu („danie z inną bazą/kuchnią/charakterem"). */
    public String instrumental() {
        return instrumental;
    }
}
