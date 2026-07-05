package com.mealspire.app.domain;

/**
 * Pure decisions of the update mechanism: whether enough time has passed to
 * ask the network again (at most once a day) and whether fetched release
 * metadata is actually newer than the installed build.
 */
public final class UpdateChecker {

    /** Where the newest release advertises itself; served without any token. */
    public static final String VERSION_URL =
            "https://raw.githubusercontent.com/sochiera/mealspire/main/dist/wersja.json";

    /** At most one network check per day — updates are rare and not urgent. */
    public static final long CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000;

    public boolean shouldCheck(long lastCheckMillis, long nowMillis) {
        long elapsed = nowMillis - lastCheckMillis;
        // A negative elapsed means the stored timestamp is in the future
        // (clock reset, restored backup); waiting for it would freeze the
        // mechanism, so check now and let the fresh timestamp heal the state.
        return elapsed < 0 || elapsed >= CHECK_INTERVAL_MS;
    }

    public boolean isUpdateAvailable(int installedVersionCode, VersionInfo latest) {
        return latest != null && latest.getVersionCode() > installedVersionCode;
    }
}
