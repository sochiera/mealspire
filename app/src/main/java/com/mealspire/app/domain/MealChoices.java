package com.mealspire.app.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Catalog of meal choices offered to the user. Selecting any of these shapes the
 * AI prompt and is learned into the user's preferences.
 */
public final class MealChoices {

    private MealChoices() {
    }

    public static List<MealChoiceOption> defaults() {
        return Collections.unmodifiableList(Arrays.asList(
                new MealChoiceOption("quick", "Szybkie",
                        "danie gotowe w około 20 minut", "szybkie dania"),
                new MealChoiceOption("vegetarian", "Wegetariańskie",
                        "danie wegetariańskie", "kuchnia wegetariańska"),
                new MealChoiceOption("hearty", "Sycące",
                        "danie sycące i pożywne", "sycące dania"),
                new MealChoiceOption("light", "Lekkie",
                        "danie lekkie i niskokaloryczne", "lekkie dania"),
                new MealChoiceOption("budget", "Tanie",
                        "danie z tanich i łatwo dostępnych składników", "tanie dania"),
                new MealChoiceOption("kids", "Dla dzieci",
                        "danie, które posmakuje dzieciom", "dania dla dzieci")
        ));
    }
}
