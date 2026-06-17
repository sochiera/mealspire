package com.mealspire.app.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ChoiceLearningTest {

    @Test
    public void everySelectedChoiceIsLearnedAsLike() {
        MealChoiceOption veg = new MealChoiceOption("veg", "Wegetariańskie",
                "danie wegetariańskie", "kuchnia wegetariańska");
        MealChoiceOption quick = new MealChoiceOption("quick", "Szybkie",
                "danie do 20 minut", "szybkie dania");

        UserPreferences result = ChoiceLearning.learnFrom(
                UserPreferences.empty(), Arrays.asList(veg, quick));

        assertTrue(result.getLikes().contains("kuchnia wegetariańska"));
        assertTrue(result.getLikes().contains("szybkie dania"));
    }

    @Test
    public void noSelectionLeavesPreferencesUnchanged() {
        UserPreferences original = UserPreferences.empty().withLike("ryż");
        UserPreferences result = ChoiceLearning.learnFrom(
                original, Collections.<MealChoiceOption>emptyList());
        assertTrue(result.getLikes().contains("ryż"));
    }
}
