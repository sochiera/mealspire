package com.mealspire.app.domain;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Jeden prompt: „osoba, która lubi/nie lubi tych dań — czy polubi te?".
 * Całe rozumowanie o guście robi model; aplikacja podaje tylko ograniczoną
 * listę jawnych reakcji (najnowsze pierwsze) i kandydatów z nazwą i składem.
 * Dieta nie trafia tu jako preferencja — kandydaci są już przez nią
 * przefiltrowani, a wynik filtruje się ponownie przed pokazaniem.
 */
public final class DishRatingPromptBuilder {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    public String systemPrompt() {
        return "Jesteś doradcą kulinarnym. Oceniasz, czy osoba o podanych reakcjach "
                + "na dania polubi każde z dań-kandydatów. Porównuj smak, składniki i "
                + "sposób przygotowania. Nowsze reakcje są ważniejsze od starszych. "
                + "Odpowiadasz wyłącznie poprawnym JSON-em, bez żadnego innego tekstu.";
    }

    public String userPrompt(String mealType, List<DishReaction> reactionsNewestFirst,
                             List<DishProposal> candidates, long now) {
        StringBuilder sb = new StringBuilder();
        sb.append("Posiłek: ").append(mealType).append(".\n\n");
        if (reactionsNewestFirst.isEmpty()) {
            sb.append("Brak reakcji użytkownika. Oceniaj według tego, jak powszechnie "
                    + "lubiane i popularne są te dania.\n\n");
        } else {
            sb.append("Reakcje użytkownika (od najnowszych):\n");
            for (DishReaction reaction : reactionsNewestFirst) {
                sb.append("- ").append(reaction.isLiked() ? "lubi" : "nie lubi")
                        .append(": ").append(reaction.getDish());
                if (!reaction.getDescription().isEmpty()) {
                    sb.append(" (").append(reaction.getDescription()).append(')');
                }
                sb.append(" — ").append(age(now - reaction.getTimeMillis())).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Kandydaci:\n");
        for (DishProposal candidate : candidates) {
            sb.append("- ").append(candidate.getName());
            String description = candidate.getDescription();
            if (!description.isEmpty()) {
                sb.append(" (").append(description).append(')');
            }
            sb.append('\n');
        }
        sb.append("\nOceń każdego kandydata od 0 (na pewno mu nie posmakuje) do 10 "
                + "(na pewno mu posmakuje). Zwróć dokładnie taki JSON:\n"
                + "{\"oceny\":[{\"danie\":\"<nazwa kandydata dokładnie jak wyżej>\","
                + "\"ocena\":<liczba 0-10>,\"powod\":\"<jedno krótkie zdanie po polsku>\"}]}");
        return sb.toString();
    }

    private static String age(long millis) {
        long days = Math.max(0, millis) / DAY;
        if (days == 0) {
            return "dziś";
        }
        return days == 1 ? "wczoraj" : days + " dni temu";
    }
}
