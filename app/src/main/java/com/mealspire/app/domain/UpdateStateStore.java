package com.mealspire.app.domain;

/**
 * Persisted state of the update check: when the network was last asked and
 * what the newest advertised release was. Keeps MainActivity free of storage
 * details and lets the decision logic stay pure.
 */
public interface UpdateStateStore {

    /**
     * Returns the timestamp of the last check. The first-ever call seeds the
     * value with {@code nowMillis} and returns it: a fresh install just
     * received the newest APK, so the first real check is deliberately a full
     * interval away — which also keeps tests on clean storage off the network.
     */
    long loadLastCheckMillis(long nowMillis);

    /** Records when the network was last asked (also after a failed attempt). */
    void saveLastCheckMillis(long millis);

    /** The newest release seen so far, or null if none (or cleared). */
    VersionInfo loadLatestKnown();

    /** Remembers the newest advertised release; null clears it. */
    void saveLatestKnown(VersionInfo info);
}
