package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class NextAlarmTimeTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private final NextAlarmTime nextAlarmTime = new NextAlarmTime();

    private long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(year, month, day, hour, minute, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void laterTodayWhenHourStillAhead() {
        long now = at(2026, Calendar.JUNE, 28, 7, 0);
        long expected = at(2026, Calendar.JUNE, 28, 8, 0);
        assertEquals(expected, nextAlarmTime.nextOccurrence(now, 8, UTC));
    }

    @Test
    public void tomorrowWhenHourAlreadyPassed() {
        long now = at(2026, Calendar.JUNE, 28, 9, 30);
        long expected = at(2026, Calendar.JUNE, 29, 8, 0);
        assertEquals(expected, nextAlarmTime.nextOccurrence(now, 8, UTC));
    }

    @Test
    public void tomorrowWhenExactlyOnTheHour() {
        long now = at(2026, Calendar.JUNE, 28, 8, 0);
        long expected = at(2026, Calendar.JUNE, 29, 8, 0);
        assertEquals(expected, nextAlarmTime.nextOccurrence(now, 8, UTC));
    }

    @Test
    public void resultIsAlwaysInTheFuture() {
        long now = at(2026, Calendar.JUNE, 28, 12, 0);
        assertTrue(nextAlarmTime.nextOccurrence(now, 18, UTC) > now);
        assertTrue(nextAlarmTime.nextOccurrence(now, 8, UTC) > now);
    }
}
