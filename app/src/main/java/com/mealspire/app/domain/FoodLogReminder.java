package com.mealspire.app.domain;

/**
 * Wieczorne pytanie „Co dzisiaj jedliście?" — osobne od przypomnień 8/12/18
 * z pomysłami na posiłek. Jedno dziennie, po kolacji; godzinę użytkownik
 * zmienia w „Więcej…" (albo wyłącza pytanie).
 */
public final class FoodLogReminder {

    /** Domyślnie po kolacji (przypomnienie o kolacji jest o 18:00). */
    public static final int DEFAULT_HOUR = 20;
    /** Godziny do wyboru w „Więcej…". */
    public static final int[] HOUR_CHOICES = {19, 20, 21, 22};
    /** Wartość godziny oznaczająca wyłączone pytanie. */
    public static final int DISABLED = -1;

    public static final String TITLE = "Co dzisiaj jedliście?";
    public static final String TEXT = "Wpisz krótko, co było dziś na talerzu — "
            + "kolejne propozycje nie będą tego powtarzać.";
    public static final String REPLY_LABEL = "Odpowiedz";
    public static final String REPLY_HINT = "np. owsianka, pomidorowa, pierogi";
    public static final String SAVED_TEXT = "Zapisane — dzięki! Jutro zaproponuję coś innego.";

    private FoodLogReminder() {
    }

    /** Czy {@code hour} to poprawna godzina pytania (0–23) albo {@link #DISABLED}. */
    public static boolean isValidHour(int hour) {
        return hour == DISABLED || (hour >= 0 && hour <= 23);
    }

    /** Etykieta godziny w menu, np. „20:00". */
    public static String hourLabel(int hour) {
        return hour == DISABLED ? "Wyłączone" : hour + ":00";
    }
}
