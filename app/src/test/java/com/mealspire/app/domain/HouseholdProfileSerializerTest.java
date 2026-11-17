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
                .withTime(HouseholdProfile.CookingTime.QUICK)
                .withCuisines(Arrays.asList("polska", "azjatycka"))
                .withDiet(DietConstraints.of(Arrays.asList(
                        DietConstraints.Exclusion.NO_PORK,
                        DietConstraints.Exclusion.NO_NUTS)))
                .withSpice(HouseholdProfile.SpiceLevel.HOT)
                .withStyle(HouseholdProfile.MealStyle.LIGHT)
                .withBudget(HouseholdProfile.Budget.THRIFTY)
                .withNovelty(HouseholdProfile.Novelty.CLASSICS);

        HouseholdProfile restored = serializer.fromJson(serializer.toJson(original));

        assertEquals(HouseholdProfile.SpiceLevel.HOT, restored.getSpice());
        assertEquals(HouseholdProfile.MealStyle.LIGHT, restored.getStyle());
        assertEquals(HouseholdProfile.Budget.THRIFTY, restored.getBudget());
        assertEquals(HouseholdProfile.Novelty.CLASSICS, restored.getNovelty());

        assertEquals(HouseholdProfile.Audience.WITH_CHILDREN, restored.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.BEGINNER, restored.getSkill());
        assertEquals(HouseholdProfile.CookingTime.QUICK, restored.getTime());
        assertEquals(Arrays.asList("polska", "azjatycka"), restored.getCuisines());
        assertEquals(2, restored.getDiet().getExclusions().size());
        assertTrue(restored.getDiet().getExclusions()
                .contains(DietConstraints.Exclusion.NO_PORK));
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
                "{\"audience\":\"KOSMICI\",\"skill\":\"MISTRZ\",\"cuisines\":[\"polska\"],"
                        + "\"exclusions\":[\"NO_PORK\",\"BEZ_KOSMITOW\"]}");
        assertEquals(HouseholdProfile.Audience.UNKNOWN, restored.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.UNKNOWN, restored.getSkill());
        assertEquals(Arrays.asList("polska"), restored.getCuisines());
        // Znane wykluczenie zostaje, nieznane (z nowszej wersji) jest pomijane.
        assertEquals(1, restored.getDiet().getExclusions().size());
        assertTrue(restored.getDiet().getExclusions()
                .contains(DietConstraints.Exclusion.NO_PORK));
    }

    @Test
    public void staryJsonBezWykluczenDajePusteWykluczenia() {
        HouseholdProfile restored = serializer.fromJson(
                "{\"audience\":\"ADULTS_ONLY\",\"skill\":\"COMFORTABLE\","
                        + "\"cuisines\":[]}");
        assertTrue(restored.getDiet().isEmpty());
    }

    @Test
    public void starszyZapisBezNowychPolDajeNieznaneOdpowiedzi() {
        HouseholdProfile restored = serializer.fromJson(
                "{\"audience\":\"ADULTS_ONLY\",\"spice\":\"Z_PRZYSZLOSCI\"}");
        assertEquals(HouseholdProfile.Audience.ADULTS_ONLY, restored.getAudience());
        assertEquals(HouseholdProfile.SpiceLevel.UNKNOWN, restored.getSpice());
        assertEquals(HouseholdProfile.MealStyle.UNKNOWN, restored.getStyle());
        assertEquals(HouseholdProfile.Budget.UNKNOWN, restored.getBudget());
        assertEquals(HouseholdProfile.Novelty.UNKNOWN, restored.getNovelty());
    }
}
