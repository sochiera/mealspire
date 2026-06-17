package com.mealspire.app.domain;

/**
 * Builds the system and user prompts for recipe generation. Kept separate from
 * the network layer so prompt wording is unit-testable and easy to tune.
 */
public final class RecipePromptBuilder {

    public String systemPrompt() {
        return "Jesteś pomocnym asystentem kulinarnym dla osoby, która gotuje w domu "
                + "dla siebie i swojej rodziny. Proponujesz konkretne, realne do ugotowania "
                + "dania z łatwo dostępnych składników. Odpowiadasz wyłącznie po polsku.\n\n"
                + "Format odpowiedzi:\n"
                + "- pierwsza linia: sama nazwa dania (bez znaczników, bez słowa \"Przepis\"),\n"
                + "- następnie pusta linia,\n"
                + "- dalej krótka lista składników i prosty sposób przygotowania.";
    }

    public String userPrompt(String mealType) {
        return userPrompt(mealType, UserPreferences.empty());
    }

    public String userPrompt(String mealType, UserPreferences preferences) {
        StringBuilder sb = new StringBuilder();
        sb.append("Zaproponuj jedno danie na: ").append(mealType).append(". ");
        sb.append("Podaj nazwę dania, listę składników i sposób przygotowania.");
        if (preferences != null && !preferences.getLikes().isEmpty()) {
            sb.append(" Użytkownik lubi: ").append(join(preferences.getLikes())).append('.');
        }
        if (preferences != null && !preferences.getDislikes().isEmpty()) {
            sb.append(" Użytkownik unika: ").append(join(preferences.getDislikes())).append('.');
        }
        return sb.toString();
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
