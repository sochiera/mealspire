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
                + "w kuchni. Uczysz się kuchni użytkownika: proponuj zarówno dania podobne do "
                + "tych, które lubi, jak i NOWE dania, które mają z nimi coś wspólnego "
                + "(podobne składniki, kuchnia lub styl). Gust to tylko wskazówka, nie sztywna "
                + "reguła: dbaj o różnorodność i NIE proponuj naraz kilku dań opartych na tym "
                + "samym głównym składniku (np. samego kurczaka). Bazą propozycji mają być proste, "
                + "codzienne dania — dodatki i warianty (np. owsianka z owocami i orzechami) traktuj "
                + "jako opcjonalne urozmaicenie do wspomnienia przy składnikach, a nie jako powód do "
                + "komplikowania dania. Odpowiadasz wyłącznie po polsku.\n\n"
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
        sb.append("Zadbaj o różnorodność — najlepiej każda propozycja z innym głównym "
                + "składnikiem, a przynajmniej jedna zupełnie nowa, inna niż zwykle. ");
        sb.append("Oddziel każdą propozycję osobną linią z trzema myślnikami: ---.");
        appendContext(sb, request);
        // Reguła 2+1: cel eksploracji wybrała appka — model tylko go realizuje.
        String exploration = request.getTasteContext().getExplorationSentence();
        if (!exploration.isEmpty()) {
            sb.append(' ').append(exploration);
        }
        return sb.toString();
    }

    private void appendContext(StringBuilder sb, RecipeRequest request) {
        request.getHouseholdProfile().appendPromptSentences(sb);
        TasteContext taste = request.getTasteContext();
        if (!taste.isEmpty()) {
            // Skompresowany profil zamiast surowej listy wszystkich polubień —
            // przy 50+ polubieniach pełna lista szumi i rozmywa sygnał.
            for (String sentence : taste.getProfileSentences()) {
                sb.append(' ').append(sentence);
            }
            if (!taste.getExampleDishes().isEmpty()) {
                sb.append(" Przykłady dań, które ostatnio polubił: ")
                        .append(join(taste.getExampleDishes())).append('.');
            }
        } else if (!request.getPreferences().getLikes().isEmpty()) {
            sb.append(" Dania, które użytkownik lubi: ")
                    .append(join(request.getPreferences().getLikes())).append('.');
        }
        String affinities = join(request.getTasteAffinities());
        if (!affinities.isEmpty()) {
            sb.append(" Cechy wspólne dań, które lubi (luźna wskazówka, nie wymóg): ")
                    .append(affinities)
                    .append(". Część propozycji może mieć z nimi coś wspólnego, ale zadbaj o "
                            + "różnorodność: NIE proponuj samych dań z tym samym składnikiem — "
                            + "dorzuć też nowe, inne dania.");
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
        String antiMonotony = request.getTasteContext().getAntiMonotonySentence();
        if (!antiMonotony.isEmpty()) {
            sb.append(' ').append(antiMonotony);
        }
    }

    private static String join(Iterable<String> items) {
        return PromptText.join(items);
    }
}
