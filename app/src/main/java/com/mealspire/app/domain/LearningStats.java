package com.mealspire.app.domain;

import java.util.Locale;

/**
 * Lokalne liczniki jakości uczenia — appka jest offline-first i prywatna,
 * więc żadna telemetria nie wychodzi z telefonu; te same liczby służą do
 * ręcznej oceny zmian algorytmu między wydaniami (widok diagnostyczny).
 * Sukces = rosnący odsetek zestawów z wyborem, malejący odsetek rerolli.
 */
public final class LearningStats {

    private final int triosShown;
    private final int triosEngaged;
    private final int rerolls;
    private final int aiProposals;
    private final int aiProposalsUntagged;

    public LearningStats(int triosShown, int triosEngaged, int rerolls,
                         int aiProposals, int aiProposalsUntagged) {
        this.triosShown = Math.max(0, triosShown);
        this.triosEngaged = Math.max(0, triosEngaged);
        this.rerolls = Math.max(0, rerolls);
        this.aiProposals = Math.max(0, aiProposals);
        this.aiProposalsUntagged = Math.max(0, aiProposalsUntagged);
    }

    public static LearningStats empty() {
        return new LearningStats(0, 0, 0, 0, 0);
    }

    public LearningStats withTrioShown() {
        return new LearningStats(triosShown + 1, triosEngaged, rerolls,
                aiProposals, aiProposalsUntagged);
    }

    public LearningStats withTrioEngaged() {
        return new LearningStats(triosShown, triosEngaged + 1, rerolls,
                aiProposals, aiProposalsUntagged);
    }

    public LearningStats withReroll() {
        return new LearningStats(triosShown, triosEngaged, rerolls + 1,
                aiProposals, aiProposalsUntagged);
    }

    /** Dolicza propozycje AI z zestawu; nieotagowana baza = starzejące się słowniki. */
    public LearningStats withAiProposals(int total, int untagged) {
        return new LearningStats(triosShown, triosEngaged, rerolls,
                aiProposals + Math.max(0, total),
                aiProposalsUntagged + Math.max(0, untagged));
    }

    public int getTriosShown() {
        return triosShown;
    }

    public int getTriosEngaged() {
        return triosEngaged;
    }

    public int getRerolls() {
        return rerolls;
    }

    public int getAiProposals() {
        return aiProposals;
    }

    public int getAiProposalsUntagged() {
        return aiProposalsUntagged;
    }

    /** Odsetek pokazanych zestawów, z których coś wybrano; 0 gdy brak danych. */
    public double acceptanceRate() {
        return triosShown == 0 ? 0.0 : (double) triosEngaged / triosShown;
    }

    /** Odsetek zestawów odrzuconych przez „Inne propozycje". */
    public double rerollRate() {
        return triosShown == 0 ? 0.0 : (double) rerolls / triosShown;
    }

    /** Tekst do widoku diagnostycznego (po polsku, gotowy do dialogu). */
    public String summaryText(int tasteEventCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pokazane zestawy propozycji: ").append(triosShown).append('\n');
        sb.append("Zestawy z wyborem: ").append(triosEngaged)
                .append(" (").append(percent(acceptanceRate())).append(")\n");
        sb.append("„Inne propozycje”: ").append(rerolls)
                .append(" (").append(percent(rerollRate())).append(")\n");
        sb.append("Propozycje od AI: ").append(aiProposals)
                .append(", bez rozpoznanej bazy: ").append(aiProposalsUntagged);
        if (aiProposals > 0) {
            sb.append(" (").append(percent(
                    (double) aiProposalsUntagged / aiProposals)).append(")");
        }
        sb.append('\n');
        sb.append("Zdarzeń gustu w dzienniku: ").append(tasteEventCount);
        return sb.toString();
    }

    private static String percent(double rate) {
        return String.format(Locale.ROOT, "%.0f%%", rate * 100);
    }
}
