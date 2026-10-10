package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gotowa pula dań jednego posiłku, ocenionych przez LLM wcześniej, w tle.
 * Trzyma wyłącznie nazwy z oceną i powodem — samo danie (skład, przepis)
 * zostaje w katalogu/książce kucharskiej, więc pula nie dubluje magazynu dań.
 * {@code rated} to wszystkie nazwy ocenione przy danym podpisie (także
 * odrzucone za niską ocenę), żeby uzupełnianie nie oceniało ich drugi raz.
 * Podpis ({@link #signature}) wiąże pulę z reakcjami, dietą i modelem; inny
 * podpis znaczy „pula nieaktualna": wolno z niej jeszcze brać, ale
 * uzupełnienie zastępuje ją od zera.
 */
public final class ReadyDishPool {

    private final String signature;
    private final List<DishRating> entries;
    private final Set<String> rated;

    public ReadyDishPool(String signature, List<DishRating> entries, Collection<String> rated) {
        this.signature = signature == null ? "" : signature;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        Set<String> keys = new LinkedHashSet<>();
        for (String name : rated) {
            keys.add(key(name));
        }
        for (DishRating entry : entries) {
            keys.add(key(entry.getDish()));
        }
        this.rated = Collections.unmodifiableSet(keys);
    }

    public static ReadyDishPool empty() {
        return new ReadyDishPool("", Collections.<DishRating>emptyList(),
                Collections.<String>emptyList());
    }

    /**
     * Podpis gustu, przy którym oceny są ważne: najnowsze reakcje (po jednej
     * na danie), wykluczenia diety i model. Zmiana któregokolwiek = nowa pula.
     */
    public static String signature(DishReactionLog reactions, DietConstraints diet,
                                   String model) {
        StringBuilder text = new StringBuilder(model == null ? "" : model).append('|');
        for (DietConstraints.Exclusion exclusion : diet.getExclusions()) {
            text.append(exclusion.name()).append(',');
        }
        text.append('|');
        for (DishReaction reaction : reactions.latestPerDish(DishRecommender.MAX_REACTIONS)) {
            text.append(key(reaction.getDish())).append(reaction.isLiked() ? '+' : '-')
                    .append(';');
        }
        return Integer.toHexString(text.toString().hashCode()) + ":" + text.length();
    }

    public String getSignature() {
        return signature;
    }

    /** Dania gotowe do pokazania, w kolejności od najlepiej ocenionego partiami. */
    public List<DishRating> getEntries() {
        return entries;
    }

    /** Nazwy (małymi literami) już ocenione przy tym podpisie. */
    public Set<String> getRated() {
        return rated;
    }

    public int size() {
        return entries.size();
    }

    public boolean isCurrent(String currentSignature) {
        return signature.equals(currentSignature);
    }

    /** Ile dań z puli da się jeszcze pokazać, pomijając już widziane w serii. */
    public int available(Set<String> excluded) {
        int count = 0;
        for (DishRating entry : entries) {
            if (!excluded.contains(key(entry.getDish()))) {
                count++;
            }
        }
        return count;
    }

    /** Pula bez podanych dań (pokazanych albo już nieaktualnych). */
    public ReadyDishPool without(Collection<String> names) {
        Set<String> removed = new HashSet<>();
        for (String name : names) {
            removed.add(key(name));
        }
        List<DishRating> kept = new ArrayList<>();
        for (DishRating entry : entries) {
            if (!removed.contains(key(entry.getDish()))) {
                kept.add(entry);
            }
        }
        return new ReadyDishPool(signature, kept, rated);
    }

    /**
     * Dołącza wynik uzupełnienia. Przy tym samym podpisie dopisuje nowe dania
     * na koniec; przy innym zastępuje całą pulę (gust się zmienił).
     *
     * @param ratedNames wszystkie nazwy ocenione w tym uzupełnieniu
     * @param shown      dania już pokazane w serii — nie wracają do puli
     */
    public ReadyDishPool merge(String newSignature, List<DishRating> fresh,
                               Collection<String> ratedNames, Collection<String> shown) {
        boolean same = signature.equals(newSignature);
        Set<String> skip = new HashSet<>();
        for (String name : shown) {
            skip.add(key(name));
        }
        List<DishRating> merged = new ArrayList<>();
        Set<String> used = new HashSet<>(skip);
        if (same) {
            for (DishRating entry : entries) {
                if (used.add(key(entry.getDish()))) {
                    merged.add(entry);
                }
            }
        }
        for (DishRating entry : fresh) {
            if (used.add(key(entry.getDish()))) {
                merged.add(entry);
            }
        }
        Set<String> allRated = new LinkedHashSet<>(same ? rated : Collections.<String>emptySet());
        allRated.addAll(ratedNames);
        return new ReadyDishPool(newSignature, merged, allRated);
    }

    static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
