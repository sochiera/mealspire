package com.mealspire.app.domain;

/**
 * Builds the prompts for the lightweight first step: one or more <em>proposals</em>
 * (name, one-line description, time, key ingredients) rather than a full recipe.
 * The dishes should be simple and made of commonly-available ingredients, and the
 * suggestions should lean towards what the user has shown they like.
 */
public final class ProposalPromptBuilder {

    public String systemPrompt() {
        return "Jesteś pomocnym asystentem kulinarnym dla osoby, która gotuje w domu "
                + "dla siebie i swojej rodziny. Proponujesz proste dania z łatwo dostępnych, "
                + "powszechnych składników — takie, które można zrobić z tego, co zwykle jest "
                + "w kuchni. Uczysz się kuchni użytkownika: jeśli wiesz, jakie dania lubi, "
                + "proponuj podobne. Odpowiadasz wyłącznie po polsku.\n\n"
                + "Na tym etapie NIE podajesz pełnego przepisu — tylko krótką propozycję dania.\n"
                + "Każdą propozycję podaj dokładnie w tym formacie, każde pole w osobnej linii:\n"
                + "Nazwa: <nazwa dania>\n"
                + "Opis: <jedno krótkie, zachęcające zdanie>\n"
                + "Czas: <przybliżony czas przygotowania, np. ok. 30 min>\n"
                + "Składniki: <kilka kluczowych składników po przecinku>\n"
                + "Jeśli proponujesz kilka dań, oddziel każdą propozycję osobną linią: ---";
    }

    /** A single proposal. */
    public String userPrompt(RecipeRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Zaproponuj jedno proste danie na: ").append(request.getMealType()).append(". ");
        sb.append("Podaj tylko propozycję: nazwę, krótki opis, czas i kluczowe składniki.");
        appendContext(sb, request);
        return sb.toString();
    }

    /** Several proposals at once, separated by lines with "---". */
    public String userPrompt(RecipeRequest request, int count) {
        if (count <= 1) {
            return userPrompt(request);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Zaproponuj ").append(count).append(" różne, proste dania na: ")
                .append(request.getMealType()).append(". ");
        sb.append("Dla każdego podaj tylko: nazwę, krótki opis, czas i kluczowe składniki. ");
        sb.append("Oddziel każdą propozycję osobną linią z trzema myślnikami: ---.");
        appendContext(sb, request);
        return sb.toString();
    }

    private void appendContext(StringBuilder sb, RecipeRequest request) {
        UserPreferences preferences = request.getPreferences();
        if (!preferences.getLikes().isEmpty()) {
            sb.append(" Dania, które użytkownik lubi (proponuj w podobnym duchu): ")
                    .append(join(preferences.getLikes())).append('.');
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
