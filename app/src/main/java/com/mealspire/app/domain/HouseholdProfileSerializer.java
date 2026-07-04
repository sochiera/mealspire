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
    private static final String KEY_CUISINES = "cuisines";

    public String toJson(HouseholdProfile profile) {
        try {
            JSONObject root = new JSONObject();
            root.put(KEY_AUDIENCE, profile.getAudience().name());
            root.put(KEY_SKILL, profile.getSkill().name());
            root.put(KEY_CUISINES, new JSONArray(profile.getCuisines()));
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
                    .withCuisines(PreferencesSerializer.readArray(
                            root.optJSONArray(KEY_CUISINES)));
        } catch (JSONException e) {
            return HouseholdProfile.empty();
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
}
