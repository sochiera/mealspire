package com.mealspire.app.domain;

/**
 * Jednorazowa migracja starych danych gustu: polubienia sprzed ery dziennika
 * stają się zdarzeniami {@code LIKED} z bieżącym timestampem. Uruchamiana przy
 * starcie; nic nie robi, gdy dziennik już coś zawiera (idempotentna).
 */
public final class TasteEventMigration {

    private TasteEventMigration() {
    }

    public static TasteEventLog migrate(TasteEventLog log, UserPreferences preferences,
                                        long now) {
        if (log == null) {
            log = TasteEventLog.empty();
        }
        if (!log.isEmpty() || preferences == null) {
            return log;
        }
        TasteEventLog migrated = log;
        for (String like : preferences.getLikes()) {
            migrated = migrated.append(new TasteEvent(
                    TasteEvent.Type.LIKED, like, TasteEvent.NO_MEAL, now));
        }
        return migrated;
    }
}
