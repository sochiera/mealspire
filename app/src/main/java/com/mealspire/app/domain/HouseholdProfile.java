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
    private final DietConstraints diet;

    private HouseholdProfile(Audience audience, CookingSkill skill, List<String> cuisines,
                             DietConstraints diet) {
        this.audience = audience;
        this.skill = skill;
        this.cuisines = cuisines;
        this.diet = diet;
    }

    public static HouseholdProfile empty() {
        return new HouseholdProfile(Audience.UNKNOWN, CookingSkill.UNKNOWN,
                Collections.<String>emptyList(), DietConstraints.empty());
    }

    public HouseholdProfile withAudience(Audience newAudience) {
        return new HouseholdProfile(
                newAudience == null ? Audience.UNKNOWN : newAudience, skill, cuisines, diet);
    }

    public HouseholdProfile withSkill(CookingSkill newSkill) {
        return new HouseholdProfile(
                audience, newSkill == null ? CookingSkill.UNKNOWN : newSkill, cuisines, diet);
    }

    public HouseholdProfile withCuisines(List<String> newCuisines) {
        return new HouseholdProfile(audience, skill, normalize(newCuisines), diet);
    }

    public HouseholdProfile withDiet(DietConstraints newDiet) {
        return new HouseholdProfile(audience, skill, cuisines,
                newDiet == null ? DietConstraints.empty() : newDiet);
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

    /** Twarde wykluczenia diety; nigdy null (puste = brak wykluczeń). */
    public DietConstraints getDiet() {
        return diet;
    }

    public boolean isEmpty() {
        return audience == Audience.UNKNOWN
                && skill == CookingSkill.UNKNOWN
                && cuisines.isEmpty()
                && diet.isEmpty();
    }

    /**
     * Zdania (po polsku) dokładane do promptów AI, żeby profil realnie wpływał
     * na propozycje i przepisy. Odpowiedzi neutralne (tylko dorośli, „radzę
     * sobie") oraz brak odpowiedzi nie dodają nic.
     */
    public List<String> promptSentences() {
        List<String> sentences = new ArrayList<>();
        // Wymóg diety zawsze pierwszy — to najważniejsza informacja w prompcie.
        if (!diet.isEmpty()) {
            sentences.add(diet.promptSentence());
        }
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
            sentences.add("Preferowane kuchnie: " + PromptText.join(cuisines) + ".");
        }
        return Collections.unmodifiableList(sentences);
    }

    /**
     * Dokleja zdania profilu do budowanego prompta (każde z wiodącą spacją).
     * Jedno miejsce definiuje sposób sklejania dla wszystkich builderów;
     * pusty profil nie dodaje nic.
     */
    public void appendPromptSentences(StringBuilder sb) {
        for (String sentence : promptSentences()) {
            sb.append(' ').append(sentence);
        }
    }

    private static List<String> normalize(List<String> values) {
        // Jedna definicja "tego samego" tekstu gustu: trim, bez pustych,
        // deduplikacja bez rozróżniania wielkości liter (jak w UserPreferences).
        return Collections.unmodifiableList(
                new ArrayList<>(UserPreferences.normalize(values)));
    }
}
