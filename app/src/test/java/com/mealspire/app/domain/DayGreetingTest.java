package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Nagłówek ekranu startowego wita zgodnie z porą dnia. */
public class DayGreetingTest {

    @Test
    public void odRanaDoPopołudniaMówiDzieńDobry() {
        assertEquals("Dzień dobry", DayGreeting.forHour(5));
        assertEquals("Dzień dobry", DayGreeting.forHour(12));
        assertEquals("Dzień dobry", DayGreeting.forHour(17));
    }

    @Test
    public void wieczoremINocąMówiDobryWieczór() {
        assertEquals("Dobry wieczór", DayGreeting.forHour(18));
        assertEquals("Dobry wieczór", DayGreeting.forHour(23));
        assertEquals("Dobry wieczór", DayGreeting.forHour(0));
        assertEquals("Dobry wieczór", DayGreeting.forHour(4));
    }
}
