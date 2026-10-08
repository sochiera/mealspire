package com.mealspire.app.domain;

/** Powitanie w nagłówku ekranu startowego, zależne od pory dnia. */
public final class DayGreeting {
    private static final int MORNING_FROM = 5;
    private static final int EVENING_FROM = 18;

    private DayGreeting() {
    }

    /** {@code hour} w zakresie 0–23 (zegar telefonu). */
    public static String forHour(int hour) {
        return hour >= MORNING_FROM && hour < EVENING_FROM ? "Dzień dobry" : "Dobry wieczór";
    }
}
