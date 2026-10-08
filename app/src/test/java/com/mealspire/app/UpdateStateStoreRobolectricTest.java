package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.UpdateStateStore;
import com.mealspire.app.domain.VersionInfo;
import com.mealspire.app.storage.SharedPreferencesUpdateStateStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Persisted state of the update check. The first-ever read of the last-check
 * timestamp seeds it with "now": a fresh install just received the newest APK,
 * so the first network check is deliberately a day away — which also keeps
 * every Robolectric test on clean SharedPreferences off the network.
 */
@RunWith(RobolectricTestRunner.class)
public class UpdateStateStoreRobolectricTest {

    private static final long NOW = 1_750_000_000_000L;

    private UpdateStateStore newStore() {
        return new SharedPreferencesUpdateStateStore(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void sha256SurvivesRestartAndClearing() {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        SharedPreferencesUpdateStateStore store = new SharedPreferencesUpdateStateStore(context);
        store.saveLatestKnown(new VersionInfo(999, "9.9", "https://example.invalid/a.apk", "a".repeat(64)));
        assertEquals("a".repeat(64), new SharedPreferencesUpdateStateStore(context).loadLatestKnown().getSha256());
        store.saveLatestKnown(null);
        assertNull(store.loadLatestKnown());
    }

    @Test
    public void firstReadOfLastCheckSeedsItWithNow() {
        assertEquals(NOW, newStore().loadLastCheckMillis(NOW));
    }

    @Test
    public void seededLastCheckSurvivesAndIgnoresLaterNow() {
        newStore().loadLastCheckMillis(NOW);
        // A later read must return the seeded value, not the new "now".
        assertEquals(NOW, newStore().loadLastCheckMillis(NOW + 123_456));
    }

    @Test
    public void savedLastCheckWinsOverSeeding() {
        UpdateStateStore store = newStore();
        store.saveLastCheckMillis(NOW - 5000);
        assertEquals(NOW - 5000, newStore().loadLastCheckMillis(NOW));
    }

    @Test
    public void latestKnownVersionIsNullOnFreshInstall() {
        assertNull(newStore().loadLatestKnown());
    }

    @Test
    public void latestKnownVersionRoundTripsAcrossInstances() {
        newStore().saveLatestKnown(new VersionInfo(3, "1.2", "https://x/a.apk"));
        VersionInfo loaded = newStore().loadLatestKnown();
        assertEquals(3, loaded.getVersionCode());
        assertEquals("1.2", loaded.getVersionName());
        assertEquals("https://x/a.apk", loaded.getApkUrl());
    }

    @Test
    public void savingNullClearsTheLatestKnownVersion() {
        UpdateStateStore store = newStore();
        store.saveLatestKnown(new VersionInfo(3, "1.2", "https://x/a.apk"));
        store.saveLatestKnown(null);
        assertNull(newStore().loadLatestKnown());
    }
}
