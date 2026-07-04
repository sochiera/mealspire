package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Profil domowników z onboardingu: dla kogo gotujemy, jak nam idzie gotowanie
 * i jakie kuchnie lubimy. Niemutowalny, a pusty profil nie wnosi żadnych zdań
 * do promptów.
 */
public class HouseholdProfileTest {

    @Test
    public void pustyProfilJestPustyIBezZdan() {
        HouseholdProfile profile = HouseholdProfile.empty();
        assertTrue(profile.isEmpty());
        assertTrue(profile.promptSentences().isEmpty());
        assertEquals(HouseholdProfile.Audience.UNKNOWN, profile.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.UNKNOWN, profile.getSkill());
        assertTrue(profile.getCuisines().isEmpty());
    }

    @Test
    public void witherzyTworzaNoweInstancjeIZachowujaResztePol() {
        HouseholdProfile base = HouseholdProfile.empty()
                .withAudience(HouseholdProfile.Audience.WITH_CHILDREN);
        HouseholdProfile withSkill = base.withSkill(HouseholdProfile.CookingSkill.BEGINNER);

        assertNotSame(base, withSkill);
        assertEquals(HouseholdProfile.Audience.WITH_CHILDREN, withSkill.getAudience());
        assertEquals(HouseholdProfile.CookingSkill.UNKNOWN, base.getSkill());
        assertFalse(withSkill.isEmpty());
    }

    @Test
    public void gotowanieDlaDzieciDodajeZdanieODzieciach() {
        List<String> sentences = HouseholdProfile.empty()
                .withAudience(HouseholdProfile.Audience.WITH_CHILDREN)
                .promptSentences();
        assertTrue(joined(sentences).contains("dzieci"));
    }

    @Test
    public void tylkoDorosliNieDodajeZdania() {
        assertTrue(HouseholdProfile.empty()
                .withAudience(HouseholdProfile.Audience.ADULTS_ONLY)
                .promptSentences().isEmpty());
    }

    @Test
    public void poczatkujacyProsiOProsteBezWyzwan() {
        String joined = joined(HouseholdProfile.empty()
                .withSkill(HouseholdProfile.CookingSkill.BEGINNER)
                .promptSentences());
        assertTrue(joined.contains("uczę się gotować"));
        assertTrue(joined.contains("proste"));
    }

    @Test
    public void wprawionyKucharzDostajeZdanieOWyzwaniach() {
        String joined = joined(HouseholdProfile.empty()
                .withSkill(HouseholdProfile.CookingSkill.CONFIDENT)
                .promptSentences());
        assertTrue(joined.contains("lubię wyzwania"));
    }

    @Test
    public void preferowaneKuchnieSaWymienione() {
        String joined = joined(HouseholdProfile.empty()
                .withCuisines(Arrays.asList("polska", "włoska"))
                .promptSentences());
        assertTrue(joined.contains("Preferowane kuchnie"));
        assertTrue(joined.contains("polska"));
        assertTrue(joined.contains("włoska"));
    }

    @Test
    public void kuchnieSaNormalizowaneBezPustychIDuplikatow() {
        HouseholdProfile profile = HouseholdProfile.empty()
                .withCuisines(Arrays.asList(" polska ", "", null, "polska", "włoska"));
        assertEquals(Arrays.asList("polska", "włoska"), profile.getCuisines());
    }

    private static String joined(List<String> sentences) {
        StringBuilder sb = new StringBuilder();
        for (String sentence : sentences) {
            sb.append(sentence).append(' ');
        }
        return sb.toString();
    }
}
