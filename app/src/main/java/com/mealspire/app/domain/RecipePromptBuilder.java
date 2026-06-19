package com.mealspire.app.domain;

import java.util.List;

/**
 * Builds the system and user prompts for recipe generation. Kept separate from
 * the network layer so prompt wording is unit-testable and easy to tune.
 */
public final class RecipePromptBuilder {

    public String systemPrompt() {
        return "Jesteś pomocnym asystentem kulinarnym dla osoby, która gotuje w domu "
                + "dla siebie i swojej rodziny. Proponujesz proste, realne do ugotowania "
                + "dania z łatwo dostępnych, powszechnych składników — takich, które zwykle "
                + "są w kuchni. Odpowiadasz wyłącznie po polsku.\n\n"
                + "Format odpowiedzi:\n"
                + "- pierwsza linia: sama nazwa dania (bez znaczników, bez słowa \"Przepis\"),\n"
                + "- następnie pusta linia,\n"
                + "- dalej krótka lista składników i prosty sposób przygotowania.";
    }

    public String userPrompt(String mealType) {
        return userPrompt(mealType, UserPreferences.empty());
    }

    public String userPrompt(String mealType, UserPreferences preferences) {
        return userPrompt(mealType, preferences, java.util.Collections.<String>emptyList());
    }

    public String userPrompt(String mealType, UserPreferences preferences,
                             Iterable<String> recentDishesToAvoid) {
        List<String> recent = toList(recentDishesToAvoid);
        return userPrompt(new RecipeRequest(mealType, preferences, recent,
                java.util.Collections.<String>emptyList()));
    }

    /**
     * Prompt for the full recipe of a dish the user already accepted from a
     * proposal. Pins the dish name and carries over the user's choices (such as
     * the number of people) so the recipe matches what was proposed.
     */
    public String fullRecipePrompt(String dishName, RecipeRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Podaj pełny przepis na danie: ").append(dishName == null ? "" : dishName.trim())
                .append(". ");
        sb.append("Wypisz listę składników i krok po kroku sposób przygotowania.");
        if (request != null) {
            String choices = join(request.getChoiceFragments());
            if (!choices.isEmpty()) {
                sb.append(" Uwzględnij wybory użytkownika: ").append(choices).append('.');
            }
        }
        return sb.toString();
    }

    public String userPrompt(RecipeRequest request) {
        UserPreferences preferences = request.getPreferences();
        StringBuilder sb = new StringBuilder();
        sb.append("Zaproponuj jedno danie na: ").append(request.getMealType()).append(". ");
        sb.append("Podaj nazwę dania, listę składników i sposób przygotowania.");
        if (!preferences.getLikes().isEmpty()) {
            sb.append(" Użytkownik lubi: ").append(join(preferences.getLikes())).append('.');
        }
        if (!preferences.getDislikes().isEmpty()) {
            sb.append(" Użytkownik unika: ").append(join(preferences.getDislikes())).append('.');
        }
        String choices = join(request.getChoiceFragments());
        if (!choices.isEmpty()) {
            sb.append(" Uwzględnij wybory użytkownika: ").append(choices).append('.');
        }
        String known = join(request.getKnownDishes());
        if (!known.isEmpty()) {
            sb.append(" Dania, które użytkownik zna i lubi z własnej bazy: ").append(known)
                    .append(". Możesz wybrać jedno z nich i podać jego przepis, albo "
                            + "zaproponować coś nowego — wybierz to, co najlepiej pasuje.");
        }
        String recent = join(request.getRecentToAvoid());
        if (!recent.isEmpty()) {
            sb.append(" Ostatnio proponowane dania (zaproponuj coś innego dla urozmaicenia): ")
                    .append(recent).append('.');
        }
        return sb.toString();
    }

    private static List<String> toList(Iterable<String> values) {
        List<String> list = new java.util.ArrayList<>();
        if (values != null) {
            for (String value : values) {
                list.add(value);
            }
        }
        return list;
    }

    private static String join(Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }
        return sb.toString();
    }
}
