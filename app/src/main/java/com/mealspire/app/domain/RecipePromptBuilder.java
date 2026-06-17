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
        return "Zaproponuj jedno danie na: " + mealType + ". "
                + "Podaj nazwę dania, listę składników i sposób przygotowania.";
    }
}
