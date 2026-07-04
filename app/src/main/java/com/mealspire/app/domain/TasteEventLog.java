package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Niemutowalny, chronologiczny dziennik zdarzeń gustu (najstarsze pierwsze).
 * To jest surowa prawda o zachowaniu użytkownika — model gustu można z niego
 * w każdej chwili przeliczyć od zera. Limit rozmiaru egzekwuje kompakcja
 * (najstarsze zdarzenia są zwijane do zamrożonego agregatu, nie gubione).
 */
public final class TasteEventLog {

    /**
     * Ile najnowszych zdarzeń trzymamy w pełnej postaci. Przy typowym użyciu
     * (kilka zdarzeń dziennie) pokrywa to ponad jeden półokres wygaszania,
     * czyli zdecydowaną większość efektywnej masy modelu.
     */
    public static final int MAX_EVENTS = 400;

    private final List<TasteEvent> events;

    public TasteEventLog(List<TasteEvent> events) {
        List<TasteEvent> copy = new ArrayList<>();
        if (events != null) {
            for (TasteEvent event : events) {
                if (event != null && event.isValid()) {
                    copy.add(event);
                }
            }
        }
        this.events = Collections.unmodifiableList(copy);
    }

    public static TasteEventLog empty() {
        return new TasteEventLog(Collections.<TasteEvent>emptyList());
    }

    public TasteEventLog append(TasteEvent event) {
        if (event == null || !event.isValid()) {
            return this;
        }
        List<TasteEvent> updated = new ArrayList<>(events);
        updated.add(event);
        return new TasteEventLog(updated);
    }

    /** Zdarzenia w kolejności chronologicznej (najstarsze pierwsze). */
    public List<TasteEvent> events() {
        return events;
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    /** Zdarzenia ponad limit (najstarsze) — wejście dla kompakcji. */
    public List<TasteEvent> overflow(int max) {
        if (events.size() <= max) {
            return Collections.emptyList();
        }
        return new ArrayList<>(events.subList(0, events.size() - max));
    }

    /** Dziennik przycięty do {@code max} najnowszych zdarzeń. */
    public TasteEventLog trimToNewest(int max) {
        if (events.size() <= max) {
            return this;
        }
        return new TasteEventLog(new ArrayList<>(
                events.subList(events.size() - max, events.size())));
    }

    /**
     * Tytuły dań z jawnych pozytywnych zdarzeń, od najświeższego, bez
     * duplikatów — „przykładowe ulubione dania" do promptu.
     */
    public List<String> recentLikedTitles(int limit) {
        List<String> titles = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && titles.size() < limit; i--) {
            TasteEvent event = events.get(i);
            if (!event.getType().isExplicit() || event.getType().weight() <= 0) {
                continue;
            }
            boolean seen = false;
            for (String title : titles) {
                if (title.equalsIgnoreCase(event.getDishTitle())) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                titles.add(event.getDishTitle());
            }
        }
        return titles;
    }
}
