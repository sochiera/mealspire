package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All inputs to a single recipe generation: the meal type, learned preferences,
 * dishes to avoid for variety, and the choice fragments the user selected this
 * time. Bundling them keeps the service and prompt-builder signatures stable.
 */
public final class RecipeRequest {

    private final String mealType;
    private final UserPreferences preferences;
    private final List<String> recentToAvoid;
    private final List<String> choiceFragments;

    public RecipeRequest(String mealType, UserPreferences preferences,
                         List<String> recentToAvoid, List<String> choiceFragments) {
        this.mealType = mealType;
        this.preferences = preferences != null ? preferences : UserPreferences.empty();
        this.recentToAvoid = copy(recentToAvoid);
        this.choiceFragments = copy(choiceFragments);
    }

    public String getMealType() {
        return mealType;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public List<String> getRecentToAvoid() {
        return recentToAvoid;
    }

    public List<String> getChoiceFragments() {
        return choiceFragments;
    }

    private static List<String> copy(List<String> values) {
        return values == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
