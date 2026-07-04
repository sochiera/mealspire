package com.mealspire.app.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anty-monotonia wymiarowa: gdy w ostatnio pokazywanych propozycjach dominuje
 * jedna baza (np. czwarty raz kurczak), do promptu trafia zdanie „unikaj tej
 * bazy w głównych propozycjach". Uogólnia listę ostatnich tytułów na wymiar —
 * tytuły łapią tylko dosłowne powtórki, baza łapie „ciągle to samo w innym
 * przebraniu".
 */
public final class MonotonyDetector {

    /** Ile ostatnich propozycji oglądamy (3 trójki). */
    public static final int WINDOW = 9;

    /** Od ilu wystąpień jednej bazy w oknie robi się monotonnie. */
    public static final int THRESHOLD = 4;

    private final DishTagger tagger = new DishTagger();

    /**
     * @param recentTitles   ostatnio pokazywane dania, najnowsze pierwsze
     * @param detailsByTitle znane przepisy (lepsze tagowanie), może być null
     * @return zdanie do promptu albo pusty string, gdy nie ma monotonii
     */
    public String detect(List<String> recentTitles, Map<String, String> detailsByTitle) {
        if (recentTitles == null || recentTitles.isEmpty()) {
            return "";
        }
        Map<String, Integer> counts = new HashMap<>();
        int seen = 0;
        for (String title : recentTitles) {
            if (seen >= WINDOW) {
                break;
            }
            seen++;
            String details = detailsByTitle == null ? null : detailsByTitle.get(title);
            String base = tagger.tag(title, details).get(TasteDimension.BASE);
            if (base != null) {
                Integer current = counts.get(base);
                counts.put(base, (current == null ? 0 : current) + 1);
            }
        }
        String dominant = null;
        int max = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                dominant = entry.getKey();
            }
        }
        if (dominant == null || max < THRESHOLD) {
            return "";
        }
        return "Ostatnio proponowane dania często miały bazę „" + dominant
                + "” — w pierwszych dwóch propozycjach unikaj tej bazy.";
    }
}
