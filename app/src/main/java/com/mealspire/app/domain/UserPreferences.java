package com.mealspire.app.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Immutable record of what the user likes and dislikes (dishes or ingredients).
 * This is where the app's taste "learning" lives: each {@link #withLike} /
 * {@link #withDislike} returns a new, normalized snapshot.
 *
 * Normalization: entries are trimmed, blanks dropped, and de-duplicated
 * case-insensitively (first spelling wins). Liking something removes it from the
 * dislikes and vice-versa, so the two sets never contradict each other.
 */
public final class UserPreferences {

    private final Set<String> likes;
    private final Set<String> dislikes;

    public UserPreferences(Collection<String> likes, Collection<String> dislikes) {
        Set<String> normalizedLikes = normalize(likes);
        Set<String> normalizedDislikes = normalize(dislikes);
        // A like wins over a dislike if both somehow contain the same value.
        removeIgnoreCase(normalizedDislikes, normalizedLikes);
        this.likes = Collections.unmodifiableSet(normalizedLikes);
        this.dislikes = Collections.unmodifiableSet(normalizedDislikes);
    }

    public static UserPreferences empty() {
        return new UserPreferences(Collections.<String>emptyList(),
                Collections.<String>emptyList());
    }

    public Set<String> getLikes() {
        return likes;
    }

    public Set<String> getDislikes() {
        return dislikes;
    }

    public UserPreferences withLike(String item) {
        if (isBlank(item)) {
            return this;
        }
        Set<String> newLikes = new LinkedHashSet<>(likes);
        addIfAbsentIgnoreCase(newLikes, item.trim());
        Set<String> newDislikes = new LinkedHashSet<>(dislikes);
        removeIgnoreCase(newDislikes, item.trim());
        return new UserPreferences(newLikes, newDislikes);
    }

    public UserPreferences withDislike(String item) {
        if (isBlank(item)) {
            return this;
        }
        Set<String> newDislikes = new LinkedHashSet<>(dislikes);
        addIfAbsentIgnoreCase(newDislikes, item.trim());
        Set<String> newLikes = new LinkedHashSet<>(likes);
        removeIgnoreCase(newLikes, item.trim());
        return new UserPreferences(newLikes, newDislikes);
    }

    private static Set<String> normalize(Collection<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (!isBlank(value)) {
                    addIfAbsentIgnoreCase(result, value.trim());
                }
            }
        }
        return result;
    }

    private static void addIfAbsentIgnoreCase(Set<String> set, String value) {
        for (String existing : set) {
            if (existing.equalsIgnoreCase(value)) {
                return;
            }
        }
        set.add(value);
    }

    private static void removeIgnoreCase(Set<String> set, Iterable<String> values) {
        for (String value : values) {
            removeIgnoreCase(set, value);
        }
    }

    private static void removeIgnoreCase(Set<String> set, String value) {
        // Explicit iterator instead of Collection.removeIf (which is API 24+).
        java.util.Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            if (it.next().equalsIgnoreCase(value)) {
                it.remove();
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
