package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Długa ankieta gustu „A czy B?": seria par dań i odpowiedzi na nie. To tylko
 * stan kwestionariusza (kolejność, postęp, wybory czekające na zapis) —
 * dania identyfikowane są nazwą jak w całej aplikacji, a wynik trafia do
 * wspólnych zdarzeń: wybrane danie = zwykła reakcja „lubię"
 * ({@link DishReactionLog}) i zdarzenie {@link TasteEvent.Type#ONBOARDING_PICK}.
 * Odpowiedzi przed {@code committed} są już zapisane jako reakcje i nie da się
 * ich cofnąć; te dalej podmienia „Cofnij". Niemutowalna.
 */
public final class TasteSurvey {

    /** Domyślna długość ankiety; użytkownik zmienia ją w „Więcej…". */
    public static final int DEFAULT_PAIRS = 40;
    /** Długości do wyboru w „Więcej…" → „Ankieta gustu". */
    public static final int[] LENGTH_CHOICES = {20, 40, 60, 80};
    /** Para bez przypisanej pory posiłku (np. zapis ze starszej wersji). */
    public static final int ANY_MEAL = -1;

    public static final int NOT_ANSWERED = -1;
    public static final int CHOICE_A = 0;
    public static final int CHOICE_B = 1;
    /** „Żadne z tych" — wymuszony wybór to fałszywy sygnał, nic nie zapisujemy. */
    public static final int CHOICE_NEITHER = 2;

    /**
     * Jedna para dań do porównania. Oba dania są z tej samej pory posiłku
     * ({@code meal}: 0 śniadanie, 1 obiad, 2 kolacja), żeby pytanie miało sens
     * („Co wolisz na obiad?"), a nie porównywało owsianki z gulaszem.
     */
    public static final class Pair {
        private final String dishA;
        private final String dishB;
        private final int meal;

        public Pair(String dishA, String dishB) {
            this(dishA, dishB, ANY_MEAL);
        }

        public Pair(String dishA, String dishB, int meal) {
            this.dishA = dishA == null ? "" : dishA;
            this.dishB = dishB == null ? "" : dishB;
            this.meal = meal < 0 ? ANY_MEAL : meal;
        }

        /** Pora posiłku obu dań albo {@link #ANY_MEAL}. */
        public int getMeal() {
            return meal;
        }

        public String getDishA() {
            return dishA;
        }

        public String getDishB() {
            return dishB;
        }
    }

    private final List<Pair> pairs;
    private final List<Integer> answers;
    private final int position;
    private final int committed;
    private final String dietKey;

    public TasteSurvey(List<Pair> pairs, List<Integer> answers, int position, int committed,
                       String dietKey) {
        this.pairs = Collections.unmodifiableList(new ArrayList<>(pairs));
        List<Integer> fixed = new ArrayList<>();
        for (int i = 0; i < this.pairs.size(); i++) {
            Integer answer = answers != null && i < answers.size() ? answers.get(i) : null;
            fixed.add(answer == null || answer < CHOICE_A || answer > CHOICE_NEITHER
                    ? NOT_ANSWERED : answer);
        }
        this.answers = Collections.unmodifiableList(fixed);
        this.position = clamp(position, 0, this.pairs.size());
        this.committed = clamp(committed, 0, this.position);
        this.dietKey = dietKey == null ? "" : dietKey;
    }

    /** Nowa ankieta: same pary, bez odpowiedzi. */
    public static TasteSurvey start(List<Pair> pairs, String dietKey) {
        return new TasteSurvey(pairs, null, 0, 0, dietKey);
    }

    public List<Pair> pairs() {
        return pairs;
    }

    public List<Integer> answers() {
        return answers;
    }

    /** Indeks bieżącej pary = liczba już przejrzanych par. */
    public int position() {
        return position;
    }

    public int committed() {
        return committed;
    }

    public String dietKey() {
        return dietKey;
    }

    public int size() {
        return pairs.size();
    }

    public boolean isFinished() {
        return position >= pairs.size();
    }

    /** Bieżąca para albo null po ostatniej. */
    public Pair current() {
        return isFinished() ? null : pairs.get(position);
    }

    /** Odpowiedź na bieżącą parę i przejście do następnej. */
    public TasteSurvey answer(int choice) {
        if (isFinished()) {
            return this;
        }
        List<Integer> next = new ArrayList<>(answers);
        next.set(position, choice);
        return new TasteSurvey(pairs, next, position + 1, committed, dietKey);
    }

    /** „Cofnij" działa tylko w obrębie odpowiedzi, które nie są jeszcze zapisane. */
    public boolean canGoBack() {
        return position > committed;
    }

    /**
     * „Cofnij" z kontrolą poprzedniej pary: oba dania muszą być w aktualnym
     * katalogu ({@code catalogDetails}) i spełniać dietę. Para sprzed zmiany nazw
     * lub sprzed zmiany wykluczeń zachowuje odpowiedź, ale nie wraca na ekran.
     */
    public boolean canGoBack(DietConstraints diet, Map<String, String> catalogDetails) {
        if (!canGoBack()) {
            return false;
        }
        Pair previous = pairs.get(position - 1);
        return catalogDetails.containsKey(previous.dishA) && catalogDetails.containsKey(previous.dishB)
                && allows(diet, previous.dishA, catalogDetails)
                && allows(diet, previous.dishB, catalogDetails);
    }

    public TasteSurvey back() {
        return canGoBack()
                ? new TasteSurvey(pairs, answers, position - 1, committed, dietKey) : this;
    }

    /** Czy jest choć jedna odpowiedź (także przed bieżącą pozycją po „Cofnij"). */
    public boolean hasAnswers() {
        for (int answer : answers) {
            if (answer != NOT_ANSWERED) {
                return true;
            }
        }
        return false;
    }

    /** Wybrane dania z odpowiedzi czekających na zapis (kolejność odpowiedzi). */
    public List<String> pendingPicks() {
        return picks(committed, position);
    }

    /** Zapisane — od teraz odpowiedzi do bieżącej pary są ostateczne. */
    public TasteSurvey commit() {
        return new TasteSurvey(pairs, answers, position, position, dietKey);
    }

    /** Grupa lubianych dań z tej ankiety: każde wybrane danie raz. */
    public List<String> likedDishes() {
        return new ArrayList<>(new LinkedHashSet<>(picks(0, position)));
    }

    /**
     * Usuwa nieprzejrzane pary z daniem, którego dieta nie dopuszcza (np. po
     * zmianie wykluczeń w „Profil domowników" między przerwą a wznowieniem).
     */
    public TasteSurvey withoutDisallowed(DietConstraints diet, Map<String, String> detailsByTitle,
                                         String newDietKey) {
        List<Pair> keptPairs = new ArrayList<>();
        List<Integer> keptAnswers = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            Pair pair = pairs.get(i);
            if (i < position || (allows(diet, pair.dishA, detailsByTitle)
                    && allows(diet, pair.dishB, detailsByTitle))) {
                keptPairs.add(pair);
                keptAnswers.add(answers.get(i));
            }
        }
        return new TasteSurvey(keptPairs, keptAnswers, position, committed, newDietKey);
    }

    /**
     * Usuwa nieprzejrzane pary z daniem spoza aktualnego katalogu, np. zapisane
     * pod nazwą sprzed aktualizacji (przemianowane dania). Takiego dania nie da
     * się sprawdzić z dietą ani pokazać pod aktualną nazwą — para odpada, a
     * przejrzane lub już odpowiedziane pary zostają bez zmian. Bez zmian zwraca {@code this}.
     */
    public TasteSurvey withoutUnknownDishes(Collection<String> knownTitles) {
        List<Pair> keptPairs = new ArrayList<>();
        List<Integer> keptAnswers = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            Pair pair = pairs.get(i);
            // Odpowiedziane pary zostają także przed pozycją po „Cofnij" (odpowiedź sprzed przerwy).
            if (i < position || answers.get(i) != NOT_ANSWERED || (knownTitles.contains(pair.dishA)
                    && knownTitles.contains(pair.dishB))) {
                keptPairs.add(pair);
                keptAnswers.add(answers.get(i));
            }
        }
        return keptPairs.size() == pairs.size() ? this
                : new TasteSurvey(keptPairs, keptAnswers, position, committed, dietKey);
    }

    private List<String> picks(int from, int to) {
        List<String> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            int answer = answers.get(i);
            if (answer == CHOICE_A) {
                result.add(pairs.get(i).dishA);
            } else if (answer == CHOICE_B) {
                result.add(pairs.get(i).dishB);
            }
        }
        return result;
    }

    private static boolean allows(DietConstraints diet, String dish,
                                  Map<String, String> detailsByTitle) {
        if (diet == null) {
            return true;
        }
        String details = detailsByTitle == null ? null : detailsByTitle.get(dish);
        if (details == null) {
            // Bez składników nie da się potwierdzić diety — przy wykluczeniach odpada.
            return diet.isEmpty();
        }
        return diet.allows(dish + "\n" + details);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
