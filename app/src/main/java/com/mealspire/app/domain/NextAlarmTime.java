package com.mealspire.app.domain;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Computes the next epoch-millis at which a given hour-of-day occurs: today if
 * that hour is still ahead, otherwise tomorrow. Minutes, seconds and millis are
 * zeroed. Pure and timezone-explicit so it is easy to unit-test.
 */
public final class NextAlarmTime {

    /** Next occurrence of {@code hour}:00 at or after {@code nowMillis}, in {@code tz}. */
    public long nextOccurrence(long nowMillis, int hour, TimeZone tz) {
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }
}
