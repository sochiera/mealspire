package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Niemutowalny profil domowników z onboardingu: dla kogo gotujemy, jak nam
 * idzie gotowanie, jakie kuchnie lubimy, jak ostro, lekko czy sycąco, za ile
 * i czy wolimy klasyki, czy nowości. Każda odpowiedź jest opcjonalna —
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

    /** Ile czasu jest zwykle na gotowanie w dzień powszedni. */
    public enum CookingTime {
        UNKNOWN,
        QUICK,
        MEDIUM,
        LONG
    }

    /** Jak ostro jadamy. */
    public enum SpiceLevel {
        UNKNOWN,
        MILD,
        MEDIUM,
        HOT
    }

    /** Na jakie jedzenie mamy zwykle ochotę: lekkie czy sycące. */
    public enum MealStyle {
        UNKNOWN,
        LIGHT,
        BALANCED,
        HEARTY
    }

    /** Ile chcemy wydawać na składniki. */
    public enum Budget {
        UNKNOWN,
        THRIFTY,
        MODERATE,
        GENEROUS
    }

    /** Sprawdzone klasyki czy ciągle coś nowego. */
    public enum Novelty {
        UNKNOWN,
        CLASSICS,
        MIXED,
        ADVENTUROUS
    }

    private final Audience audience;
    private final CookingSkill skill;
    private final CookingTime time;
    private final List<String> cuisines;
    private final DietConstraints diet;
    private final SpiceLevel spice;
    private final MealStyle style;
    private final Budget budget;
    private final Novelty novelty;

    private HouseholdProfile(Audience audience, CookingSkill skill, CookingTime time,
                             List<String> cuisines, DietConstraints diet, SpiceLevel spice,
                             MealStyle style, Budget budget, Novelty novelty) {
        this.audience = audience;
        this.skill = skill;
        this.time = time;
        this.cuisines = cuisines;
        this.diet = diet;
        this.spice = spice;
        this.style = style;
        this.budget = budget;
        this.novelty = novelty;
    }

    public static HouseholdProfile empty() {
        return new HouseholdProfile(Audience.UNKNOWN, CookingSkill.UNKNOWN,
                CookingTime.UNKNOWN, Collections.<String>emptyList(),
                DietConstraints.empty(), SpiceLevel.UNKNOWN, MealStyle.UNKNOWN,
                Budget.UNKNOWN, Novelty.UNKNOWN);
    }

    public HouseholdProfile withAudience(Audience newAudience) {
        return new HouseholdProfile(
                newAudience == null ? Audience.UNKNOWN : newAudience, skill, time,
                cuisines, diet, spice, style, budget, novelty);
    }

    public HouseholdProfile withSkill(CookingSkill newSkill) {
        return new HouseholdProfile(
                audience, newSkill == null ? CookingSkill.UNKNOWN : newSkill, time,
                cuisines, diet, spice, style, budget, novelty);
    }

    public HouseholdProfile withTime(CookingTime newTime) {
        return new HouseholdProfile(audience, skill,
                newTime == null ? CookingTime.UNKNOWN : newTime, cuisines, diet,
                spice, style, budget, novelty);
    }

    public HouseholdProfile withCuisines(List<String> newCuisines) {
        return new HouseholdProfile(audience, skill, time, normalize(newCuisines), diet,
                spice, style, budget, novelty);
    }

    public HouseholdProfile withDiet(DietConstraints newDiet) {
        return new HouseholdProfile(audience, skill, time, cuisines,
                newDiet == null ? DietConstraints.empty() : newDiet,
                spice, style, budget, novelty);
    }

    public HouseholdProfile withSpice(SpiceLevel newSpice) {
        return new HouseholdProfile(audience, skill, time, cuisines, diet,
                newSpice == null ? SpiceLevel.UNKNOWN : newSpice, style, budget, novelty);
    }

    public HouseholdProfile withStyle(MealStyle newStyle) {
        return new HouseholdProfile(audience, skill, time, cuisines, diet, spice,
                newStyle == null ? MealStyle.UNKNOWN : newStyle, budget, novelty);
    }

    public HouseholdProfile withBudget(Budget newBudget) {
        return new HouseholdProfile(audience, skill, time, cuisines, diet, spice, style,
                newBudget == null ? Budget.UNKNOWN : newBudget, novelty);
    }

    public HouseholdProfile withNovelty(Novelty newNovelty) {
        return new HouseholdProfile(audience, skill, time, cuisines, diet, spice, style,
                budget, newNovelty == null ? Novelty.UNKNOWN : newNovelty);
    }

    public Audience getAudience() {
        return audience;
    }

    public CookingSkill getSkill() {
        return skill;
    }

    public CookingTime getTime() {
        return time;
    }

    public List<String> getCuisines() {
        return cuisines;
    }

    public SpiceLevel getSpice() {
        return spice;
    }

    public MealStyle getStyle() {
        return style;
    }

    public Budget getBudget() {
        return budget;
    }

    public Novelty getNovelty() {
        return novelty;
    }

    /** Twarde wykluczenia diety; nigdy null (puste = brak wykluczeń). */
    public DietConstraints getDiet() {
        return diet;
    }

    public boolean isEmpty() {
        return audience == Audience.UNKNOWN
                && skill == CookingSkill.UNKNOWN
                && time == CookingTime.UNKNOWN
                && cuisines.isEmpty()
                && diet.isEmpty()
                && spice == SpiceLevel.UNKNOWN
                && style == MealStyle.UNKNOWN
                && budget == Budget.UNKNOWN
                && novelty == Novelty.UNKNOWN;
    }

    /**
     * Zdania (po polsku) dokładane do promptów AI, żeby profil realnie wpływał
     * na propozycje i przepisy. Odpowiedzi neutralne (tylko dorośli, „radzę
     * sobie", środkowe opcje ostrości/stylu/budżetu/nowości) oraz brak
     * odpowiedzi nie dodają nic.
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
        // MEDIUM (ok. pół godziny) to typowe założenie — nie dodaje nic.
        if (time == CookingTime.QUICK) {
            sentences.add("W dzień powszedni mam najwyżej ok. 20 minut na "
                    + "gotowanie — proponuj szybkie dania.");
        } else if (time == CookingTime.LONG) {
            sentences.add("Zwykle mam godzinę lub więcej na gotowanie — "
                    + "dłuższe przepisy też pasują.");
        }
        if (!cuisines.isEmpty()) {
            sentences.add("Preferowane kuchnie: " + PromptText.join(cuisines) + ".");
        }
        if (spice == SpiceLevel.MILD) {
            sentences.add("Jemy łagodnie — bez ostrych przypraw i chili.");
        } else if (spice == SpiceLevel.HOT) {
            sentences.add("Lubimy ostre jedzenie — śmiało dodawaj chili i pikantne przyprawy.");
        }
        if (style == MealStyle.LIGHT) {
            sentences.add("Wolimy lekkie, zdrowe dania — dużo warzyw, mało smażenia.");
        } else if (style == MealStyle.HEARTY) {
            sentences.add("Lubimy sycące, domowe jedzenie.");
        }
        if (budget == Budget.THRIFTY) {
            sentences.add("Gotujemy oszczędnie — tanie, łatwo dostępne składniki.");
        } else if (budget == Budget.GENEROUS) {
            sentences.add("Budżet nie gra roli — droższe składniki też są w porządku.");
        }
        if (novelty == Novelty.CLASSICS) {
            sentences.add("Najchętniej jemy sprawdzone klasyki — bez kulinarnych eksperymentów.");
        } else if (novelty == Novelty.ADVENTUROUS) {
            sentences.add("Lubimy próbować nowych smaków — proponuj też mniej oczywiste dania.");
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
