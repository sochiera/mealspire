package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Jedyna lista, z której aplikacja uczy się gustu na ścieżce z LLM: jawne
 * reakcje w kolejności dopisywania (najstarsza pierwsza). Niemutowalna;
 * powyżej {@link #MAX_STORED} najstarsze wypadają.
 */
public final class DishReactionLog {

    public static final int MAX_STORED = 200;

    private final List<DishReaction> reactions;

    public DishReactionLog(List<DishReaction> reactions) {
        List<DishReaction> copy = new ArrayList<>();
        if (reactions != null) {
            for (DishReaction reaction : reactions) {
                if (reaction != null && !reaction.getDish().isEmpty()) {
                    copy.add(reaction);
                }
            }
        }
        if (copy.size() > MAX_STORED) {
            copy = new ArrayList<>(copy.subList(copy.size() - MAX_STORED, copy.size()));
        }
        this.reactions = Collections.unmodifiableList(copy);
    }

    public static DishReactionLog empty() {
        return new DishReactionLog(Collections.<DishReaction>emptyList());
    }

    /**
     * Jednorazowe przeniesienie polubień sprzed listy reakcji, żeby
     * aktualizacja aplikacji nie zerowała tego, czego już się nauczyła.
     */
    public static DishReactionLog fromLikes(Collection<String> likes,
                                            Map<String, String> detailsByTitle, long now) {
        List<DishReaction> seeded = new ArrayList<>();
        for (String like : likes) {
            String details = detailsByTitle == null ? null : detailsByTitle.get(like);
            seeded.add(new DishReaction(like, DishReaction.describe(details), true, now));
        }
        return new DishReactionLog(seeded);
    }

    public DishReactionLog append(DishReaction reaction) {
        List<DishReaction> next = new ArrayList<>(reactions);
        next.add(reaction);
        return new DishReactionLog(next);
    }

    public List<DishReaction> all() {
        return reactions;
    }

    public boolean isEmpty() {
        return reactions.isEmpty();
    }

    public int size() {
        return reactions.size();
    }

    /**
     * Do {@code limit} najnowszych reakcji, najnowsza pierwsza, po jednej na
     * danie — późniejsza zmiana zdania zastępuje wcześniejszą.
     */
    public List<DishReaction> latestPerDish(int limit) {
        List<DishReaction> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = reactions.size() - 1; i >= 0 && result.size() < limit; i--) {
            DishReaction reaction = reactions.get(i);
            if (seen.add(reaction.getDish().toLowerCase())) {
                result.add(reaction);
            }
        }
        return result;
    }
}
