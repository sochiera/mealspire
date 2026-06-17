package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PreferencesSerializerTest {

    private final PreferencesSerializer serializer = new PreferencesSerializer();

    @Test
    public void roundTripPreservesLikesAndDislikes() {
        UserPreferences prefs = UserPreferences.empty()
                .withLike("Pierogi")
                .withLike("Naleśniki")
                .withDislike("Kalafior");

        UserPreferences restored = serializer.fromJson(serializer.toJson(prefs));

        assertEquals(prefs.getLikes(), restored.getLikes());
        assertEquals(prefs.getDislikes(), restored.getDislikes());
    }

    @Test
    public void nullOrBlankJsonYieldsEmptyPreferences() {
        assertTrue(serializer.fromJson(null).getLikes().isEmpty());
        assertTrue(serializer.fromJson("").getDislikes().isEmpty());
    }

    @Test
    public void malformedJsonYieldsEmptyPreferences() {
        UserPreferences prefs = serializer.fromJson("{not valid json");
        assertTrue(prefs.getLikes().isEmpty());
        assertTrue(prefs.getDislikes().isEmpty());
    }
}
