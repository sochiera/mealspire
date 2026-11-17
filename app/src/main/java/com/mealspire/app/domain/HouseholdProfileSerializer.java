package com.mealspire.app.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serializuje {@link HouseholdProfile} do i z JSON-a na potrzeby
 * SharedPreferences. Odporny na null/puste/zepsute dane — zwraca wtedy pusty
 * profil zamiast rzucać wyjątkiem (wzorem {@link PreferencesSerializer}).
 */
public final class HouseholdProfileSerializer {

    private static final String KEY_AUDIENCE = "audience";
    private static final String KEY_SKILL = "skill";
    private static final String KEY_TIME = "time";
    private static final String KEY_CUISINES = "cuisines";
    private static final String KEY_EXCLUSIONS = "exclusions";
    private static final String KEY_SPICE = "spice";
    private static final String KEY_STYLE = "style";
    private static final String KEY_BUDGET = "budget";
    private static final String KEY_NOVELTY = "novelty";

    public String toJson(HouseholdProfile profile) {
        try {
            JSONObject root = new JSONObject();
            root.put(KEY_AUDIENCE, profile.getAudience().name());
            root.put(KEY_SKILL, profile.getSkill().name());
            root.put(KEY_TIME, profile.getTime().name());
            root.put(KEY_CUISINES, new JSONArray(profile.getCuisines()));
            JSONArray exclusions = new JSONArray();
            for (DietConstraints.Exclusion exclusion : profile.getDiet().getExclusions()) {
                exclusions.put(exclusion.name());
            }
            root.put(KEY_EXCLUSIONS, exclusions);
            root.put(KEY_SPICE, profile.getSpice().name());
            root.put(KEY_STYLE, profile.getStyle().name());
            root.put(KEY_BUDGET, profile.getBudget().name());
            root.put(KEY_NOVELTY, profile.getNovelty().name());
            return root.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    public HouseholdProfile fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return HouseholdProfile.empty();
        }
        try {
            JSONObject root = new JSONObject(json);
            return HouseholdProfile.empty()
                    .withAudience(audienceOf(root.optString(KEY_AUDIENCE, "")))
                    .withSkill(skillOf(root.optString(KEY_SKILL, "")))
                    .withTime(timeOf(root.optString(KEY_TIME, "")))
                    .withCuisines(PreferencesSerializer.readArray(
                            root.optJSONArray(KEY_CUISINES)))
                    .withDiet(dietOf(PreferencesSerializer.readArray(
                            root.optJSONArray(KEY_EXCLUSIONS))))
                    .withSpice(enumOf(HouseholdProfile.SpiceLevel.class,
                            root.optString(KEY_SPICE, ""), HouseholdProfile.SpiceLevel.UNKNOWN))
                    .withStyle(enumOf(HouseholdProfile.MealStyle.class,
                            root.optString(KEY_STYLE, ""), HouseholdProfile.MealStyle.UNKNOWN))
                    .withBudget(enumOf(HouseholdProfile.Budget.class,
                            root.optString(KEY_BUDGET, ""), HouseholdProfile.Budget.UNKNOWN))
                    .withNovelty(enumOf(HouseholdProfile.Novelty.class,
                            root.optString(KEY_NOVELTY, ""), HouseholdProfile.Novelty.UNKNOWN));
        } catch (JSONException e) {
            return HouseholdProfile.empty();
        }
    }

    private static DietConstraints dietOf(java.util.List<String> names) {
        java.util.List<DietConstraints.Exclusion> exclusions = new java.util.ArrayList<>();
        for (String name : names) {
            try {
                exclusions.add(DietConstraints.Exclusion.valueOf(name));
            } catch (IllegalArgumentException e) {
                // Nieznana wartość (np. z nowszej wersji appki) — pomiń.
            }
        }
        return DietConstraints.of(exclusions);
    }

    /** Brak/nieznana wartość (np. starszy zapis lub nowsza appka) = {@code fallback}. */
    private static <E extends Enum<E>> E enumOf(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static HouseholdProfile.Audience audienceOf(String name) {
        try {
            return HouseholdProfile.Audience.valueOf(name);
        } catch (IllegalArgumentException e) {
            return HouseholdProfile.Audience.UNKNOWN;
        }
    }

    private static HouseholdProfile.CookingSkill skillOf(String name) {
        try {
            return HouseholdProfile.CookingSkill.valueOf(name);
        } catch (IllegalArgumentException e) {
            return HouseholdProfile.CookingSkill.UNKNOWN;
        }
    }

    private static HouseholdProfile.CookingTime timeOf(String name) {
        try {
            return HouseholdProfile.CookingTime.valueOf(name);
        } catch (IllegalArgumentException e) {
            return HouseholdProfile.CookingTime.UNKNOWN;
        }
    }
}
