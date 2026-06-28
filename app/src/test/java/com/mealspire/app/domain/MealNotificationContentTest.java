package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class MealNotificationContentTest {

    @Test
    public void titleNamesTheMeal() {
        MealNotificationContent content =
                MealNotificationContent.forMeal("Śniadanie", Arrays.asList("Owsianka"));
        assertEquals("Śniadanie — pomysły na dziś", content.getTitle());
    }

    @Test
    public void bodyJoinsProposalNames() {
        MealNotificationContent content = MealNotificationContent.forMeal(
                "Obiad", Arrays.asList("Makaron z pomidorami", "Ryż z warzywami", "Kurczak z kaszą"));
        assertEquals("Makaron z pomidorami · Ryż z warzywami · Kurczak z kaszą",
                content.getText());
    }

    @Test
    public void skipsBlankNames() {
        MealNotificationContent content = MealNotificationContent.forMeal(
                "Kolacja", Arrays.asList("Omlet warzywny", "  ", null));
        assertEquals("Omlet warzywny", content.getText());
    }

    @Test
    public void fallsBackWhenNoProposals() {
        MealNotificationContent content =
                MealNotificationContent.forMeal("Obiad", Collections.emptyList());
        assertTrue(content.getText().toLowerCase().contains("otwórz"));
    }
}
