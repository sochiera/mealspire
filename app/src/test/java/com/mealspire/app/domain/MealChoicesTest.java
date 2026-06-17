package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MealChoicesTest {

    @Test
    public void defaultsAreNonEmpty() {
        assertFalse(MealChoices.defaults().isEmpty());
    }

    @Test
    public void everyOptionHasLabelFragmentAndLearnTag() {
        for (MealChoiceOption option : MealChoices.defaults()) {
            assertFalse(option.getId().trim().isEmpty());
            assertFalse(option.getLabel().trim().isEmpty());
            assertFalse(option.getPromptFragment().trim().isEmpty());
            assertFalse(option.getLearnTag().trim().isEmpty());
        }
    }

    @Test
    public void optionIdsAreUnique() {
        List<MealChoiceOption> options = MealChoices.defaults();
        Set<String> ids = new HashSet<>();
        for (MealChoiceOption option : options) {
            ids.add(option.getId());
        }
        assertTrue(ids.size() == options.size());
    }
}
