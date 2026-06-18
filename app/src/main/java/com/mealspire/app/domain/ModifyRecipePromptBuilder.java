package com.mealspire.app.domain;

/**
 * Builds the prompts for tweaking an already-shown recipe with a free-text
 * instruction from the user (for example "nie mam jogurtu, czym zastąpić?").
 * The model returns a full, revised recipe in the same title + body format as a
 * freshly generated one, so {@link RecipeTextParser} can parse it unchanged.
 */
public final class ModifyRecipePromptBuilder {

    public String systemPrompt() {
        return "Jesteś pomocnym asystentem kulinarnym. Otrzymujesz istniejący przepis i "
                + "prośbę użytkownika o zmianę. Zwracasz poprawioną wersję przepisu. "
                + "Odpowiadasz wyłącznie po polsku.\n\n"
                + "Jeśli użytkownik pisze, że czegoś nie ma, zaproponuj sensowny zamiennik "
                + "i uwzględnij go w przepisie.\n\n"
                + "Format odpowiedzi:\n"
                + "- pierwsza linia: sama nazwa dania (bez znaczników, bez słowa \"Przepis\"),\n"
                + "- następnie pusta linia,\n"
                + "- dalej krótka lista składników i prosty sposób przygotowania.";
    }

    public String userPrompt(Recipe current, String instruction) {
        String title = current == null ? "" : current.getTitle();
        String details = current == null ? "" : current.getDetails();
        String change = instruction == null ? "" : instruction.trim();
        StringBuilder sb = new StringBuilder();
        sb.append("Oto aktualny przepis.\n");
        sb.append("Nazwa: ").append(title).append('\n');
        sb.append(details).append("\n\n");
        sb.append("Wprowadź następującą zmianę zgodnie z prośbą użytkownika: \"")
                .append(change).append("\".\n");
        sb.append("Podaj poprawiony przepis w tym samym formacie (nazwa, składniki, "
                + "sposób przygotowania).");
        return sb.toString();
    }
}
