package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Niemutowalny profil domowników z onboardingu: dla kogo gotujemy, jak nam
 * idzie gotowanie i jakie kuchnie lubimy. Każda odpowiedź jest opcjonalna —
 * nieudzielona ({@code UNKNOWN} / pusta lista) nie wnosi nic do promptów,
 * więc pominięty quiz niczego nie zmienia.
 */
public final class HouseholdProfile {

    /** Dla kogo gotujemy. */
    public enum Audience {
        UNKNOWN,
        ADULTS_ONLY,
        WITH_CHILDREN
    }

    /** Jak użytkownikowi idzie gotowanie. */
    public enum CookingSkill {
        UNKNOWN,
        BEGINNER,
        COMFORTABLE,
        CONFIDENT
    }

    private final Audience audience;
    private final CookingSkill skill;
    private final List<String> cuisines;

    private HouseholdProfile(Audience audience, CookingSkill skill, List<String> cuisines) {
        this.audience = audience;
        this.skill = skill;
        this.cuisines = cuisines;
    }

    public static HouseholdProfile empty() {
        return new HouseholdProfile(Audience.UNKNOWN, CookingSkill.UNKNOWN,
                Collections.<String>emptyList());
    }

    public HouseholdProfile withAudience(Audience newAudience) {
        return new HouseholdProfile(
                newAudience == null ? Audience.UNKNOWN : newAudience, skill, cuisines);
    }

    public HouseholdProfile withSkill(CookingSkill newSkill) {
        return new HouseholdProfile(
                audience, newSkill == null ? CookingSkill.UNKNOWN : newSkill, cuisines);
    }

    public HouseholdProfile withCuisines(List<String> newCuisines) {
        return new HouseholdProfile(audience, skill, normalize(newCuisines));
    }

    public Audience getAudience() {
        return audience;
    }

    public CookingSkill getSkill() {
        return skill;
    }

    public List<String> getCuisines() {
        return cuisines;
    }

    public boolean isEmpty() {
        return audience == Audience.UNKNOWN
                && skill == CookingSkill.UNKNOWN
                && cuisines.isEmpty();
    }

    /**
     * Zdania (po polsku) dokładane do promptów AI, żeby profil realnie wpływał
     * na propozycje i przepisy. Odpowiedzi neutralne (tylko dorośli, „radzę
     * sobie") oraz brak odpowiedzi nie dodają nic.
     */
    public List<String> promptSentences() {
        List<String> sentences = new ArrayList<>();
        if (audience == Audience.WITH_CHILDREN) {
            sentences.add("Gotuję też dla dzieci — proponuj dania, "
                    + "które dzieci chętnie jedzą.");
        }
        if (skill == CookingSkill.BEGINNER) {
            sentences.add("Dopiero uczę się gotować — tylko proste przepisy "
                    + "z niewielu kroków.");
        } else if (skill == CookingSkill.CONFIDENT) {
            sentences.add("Gotuję dobrze i lubię wyzwania — możesz czasem "
                    + "zaproponować coś ambitniejszego.");
        }
        if (!cuisines.isEmpty()) {
            StringBuilder sb = new StringBuilder("Preferowane kuchnie: ");
            for (int i = 0; i < cuisines.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(cuisines.get(i));
            }
            sentences.add(sb.append('.').toString());
        }
        return Collections.unmodifiableList(sentences);
    }

    private static List<String> normalize(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }
                String trimmed = value.trim();
                if (!containsIgnoreCase(result, trimmed)) {
                    result.add(trimmed);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        for (String existing : values) {
            if (existing.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
