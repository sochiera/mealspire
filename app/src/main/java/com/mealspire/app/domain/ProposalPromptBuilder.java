package com.mealspire.app.domain;

/**
 * Builds the prompts for the lightweight first step: a <em>proposal</em> (name,
 * one-line description, time, key ingredients) rather than a full recipe. Kept
 * separate from the network layer so the wording stays unit-testable.
 */
public final class ProposalPromptBuilder {

    public String systemPrompt() {
        return "Jesteś pomocnym asystentem kulinarnym dla osoby, która gotuje w domu "
                + "dla siebie i swojej rodziny. Proponujesz konkretne, realne do ugotowania "
                + "dania z łatwo dostępnych składników. Odpowiadasz wyłącznie po polsku.\n\n"
                + "Na tym etapie NIE podajesz pełnego przepisu — tylko krótką propozycję dania.\n"
                + "Odpowiedz dokładnie w tym formacie, każde pole w osobnej linii:\n"
                + "Nazwa: <nazwa dania>\n"
                + "Opis: <jedno krótkie, zachęcające zdanie>\n"
                + "Czas: <przybliżony czas przygotowania, np. ok. 30 min>\n"
                + "Składniki: <kilka kluczowych składników po przecinku>\n"
                + "Nie dodawaj nic poza tymi czterema liniami.";
    }

    public String userPrompt(RecipeRequest request) {
        UserPreferences preferences = request.getPreferences();
        StringBuilder sb = new StringBuilder();
        sb.append("Zaproponuj jedno danie na: ").append(request.getMealType()).append(". ");
        sb.append("Podaj tylko propozycję: nazwę, krótki opis, czas i kluczowe składniki.");
        if (!preferences.getLikes().isEmpty()) {
            sb.append(" Użytkownik lubi: ").append(join(preferences.getLikes())).append('.');
        }
        if (!preferences.getDislikes().isEmpty()) {
            sb.append(" Użytkownik unika: ").append(join(preferences.getDislikes())).append('.');
        }
        String choices = join(request.getChoiceFragments());
        if (!choices.isEmpty()) {
            sb.append(" Uwzględnij: ").append(choices).append('.');
        }
        String known = join(request.getKnownDishes());
        if (!known.isEmpty()) {
            sb.append(" Dania, które użytkownik zna i lubi z własnej bazy: ").append(known)
                    .append(". Możesz zaproponować jedno z nich albo coś nowego — "
                            + "wybierz to, co najlepiej pasuje.");
        }
        String recent = join(request.getRecentToAvoid());
        if (!recent.isEmpty()) {
            sb.append(" Ostatnio proponowane dania (zaproponuj coś innego dla urozmaicenia): ")
                    .append(recent).append('.');
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
