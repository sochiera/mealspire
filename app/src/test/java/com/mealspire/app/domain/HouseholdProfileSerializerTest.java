package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * Serializacja profilu domowników do JSON-a (SharedPreferences). Odporna na
 * null/śmieci — wtedy zwraca pusty profil zamiast rzucać wyjątkiem.
 */
public class HouseholdProfileSerializerTest {

    private final HouseholdProfileSerializer serializer = new HouseholdProfileSerializer();

    @Test
    public void pelnyProfilPrzezywaRoundtrip() {
        HouseholdProfile original = HouseholdProfile.empty()
                .withAudience(HouseholdProfile.Audience.WITH_CHILDREN)
                .withSkill(HouseholdProfile.CookingSkill.BEGINNER)
                .withCuisines(Arrays.asList("polska", "azjatycka"));

        HouseholdProfile restored = serializer.fromJson(serializer.toJson(original));

        assertEquals(HouseholdProfile.Audience.WITH_CHILDREN, restored.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.BEGINNER, restored.getSkill());
        assertEquals(Arrays.asList("polska", "azjatycka"), restored.getCuisines());
    }

    @Test
    public void pustyProfilPrzezywaRoundtrip() {
        HouseholdProfile restored = serializer.fromJson(
                serializer.toJson(HouseholdProfile.empty()));
        assertTrue(restored.isEmpty());
    }

    @Test
    public void nullPusteISmieciDajaPustyProfil() {
        assertTrue(serializer.fromJson(null).isEmpty());
        assertTrue(serializer.fromJson("").isEmpty());
        assertTrue(serializer.fromJson("to nie jest json").isEmpty());
    }

    @Test
    public void nieznaneWartosciEnumowSaTolerowane() {
        HouseholdProfile restored = serializer.fromJson(
                "{\"audience\":\"KOSMICI\",\"skill\":\"MISTRZ\",\"cuisines\":[\"polska\"]}");
        assertEquals(HouseholdProfile.Audience.UNKNOWN, restored.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.UNKNOWN, restored.getSkill());
        assertEquals(Arrays.asList("polska"), restored.getCuisines());
    }
}
