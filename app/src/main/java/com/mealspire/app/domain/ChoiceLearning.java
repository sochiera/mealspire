package com.mealspire.app.domain;

/**
 * Turns the choices a user makes into learned preferences: every selected option
 * is recorded as a liked tag, so future suggestions lean toward those choices.
 */
public final class ChoiceLearning {

    private ChoiceLearning() {
    }

    public static UserPreferences learnFrom(UserPreferences preferences,
                                            Iterable<MealChoiceOption> selectedOptions) {
        UserPreferences result = preferences;
        for (MealChoiceOption option : selectedOptions) {
            result = result.withLike(option.getLearnTag());
        }
        return result;
    }
}
