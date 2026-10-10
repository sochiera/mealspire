package com.mealspire.app.domain;

import java.util.ArrayList;
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
    public static final int DEFAULT_PAIRS = 20;
    /** Długości do wyboru w „Więcej…" → „Ankieta gustu". */
    public static final int[] LENGTH_CHOICES = {10, 20, 30, 40};

    public static final int NOT_ANSWERED = -1;
    public static final int CHOICE_A = 0;
    public static final int CHOICE_B = 1;
    /** „Żadne z tych" — wymuszony wybór to fałszywy sygnał, nic nie zapisujemy. */
    public static final int CHOICE_NEITHER = 2;

    /** Jedna para dań do porównania. */
    public static final class Pair {
        private final String dishA;
        private final String dishB;

        public Pair(String dishA, String dishB) {
            this.dishA = dishA == null ? "" : dishA;
            this.dishB = dishB == null ? "" : dishB;
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

    public TasteSurvey back() {
        return canGoBack()
                ? new TasteSurvey(pairs, answers, position - 1, committed, dietKey) : this;
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
        return diet.allows(dish + "\n" + (details == null ? "" : details));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
