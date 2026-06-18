package com.mealspire.app.domain;

/**
 * Prompts for importing a dish the user already knows and likes — either from a
 * short description or from the text of a recipe page.
 */
public final class KnownDishPromptBuilder {

    public String systemPrompt() {
        return "Jesteś asystentem kulinarnym. Na podstawie materiału od użytkownika "
                + "rozpoznaj JEDNO danie, które użytkownik zna i lubi, i przygotuj zwięzły "
                + "przepis. Odpowiadasz wyłącznie po polsku.\n\n"
                + "Format odpowiedzi:\n"
                + "- pierwsza linia: sama nazwa dania (bez znaczników),\n"
                + "- następnie pusta linia,\n"
                + "- dalej krótka lista składników i prosty sposób przygotowania.";
    }

    public String userPromptForDescription(String description) {
        return "Użytkownik opisał danie, które zna i lubi: \"" + description + "\". "
                + "Rozpoznaj danie i podaj jego nazwę oraz zwięzły przepis.";
    }

    public String userPromptForPageText(String url, String pageText) {
        return "Użytkownik podał link do dania, które zna i lubi: " + url + "\n\n"
                + "Treść strony:\n" + pageText + "\n\n"
                + "Na tej podstawie podaj nazwę dania oraz zwięzły przepis.";
    }
}
