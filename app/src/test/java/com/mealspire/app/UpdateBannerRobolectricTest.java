package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.UpdateChecker;
import com.mealspire.app.domain.UpdateStateStore;
import com.mealspire.app.domain.VersionInfo;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesUpdateStateStore;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

/**
 * The update banner on the start screen. Like every Robolectric test these
 * stay strictly off the network: the JSON source is replaced through a test
 * seam, and one guard test proves a fresh start never touches the source at
 * all (the first-read seeding of the last-check timestamp keeps it a day away).
 */
@RunWith(RobolectricTestRunner.class)
public class UpdateBannerRobolectricTest {

    private static final String APK_URL =
            "https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk";

    @After
    public void resetSeams() {
        MainActivity.versionJsonSourceOverride = null;
        MainActivity.updateCheckExecutorOverride = null;
    }

    private UpdateStateStore store() {
        return new SharedPreferencesUpdateStateStore(
                ApplicationProvider.getApplicationContext());
    }

    private MainActivity launch() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    /** Runs the "background" check inline so tests are deterministic. */
    private void runChecksInline() {
        MainActivity.updateCheckExecutorOverride = Runnable::run;
    }

    @Test
    public void bannerAppearsWhenAKnownReleaseIsNewer() {
        store().saveLatestKnown(new VersionInfo(999, "9.9", APK_URL));
        MainActivity activity = launch();

        Button banner = activity.findViewById(R.id.update_banner);
        assertNotNull("Znany nowszy versionCode powinien pokazać baner", banner);
        assertTrue("Baner powinien pokazywać nazwę wersji",
                banner.getText().toString().contains("9.9"));
    }

    @Test
    public void tappingTheBannerOpensTheInAppUpdater() {
        store().saveLatestKnown(new VersionInfo(999, "9.9", APK_URL));
        MainActivity activity = launch();

        activity.<Button>findViewById(R.id.update_banner).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Klik w baner powinien odpalić intent", started);
        assertEquals("com.mealspire.app.update.UpdateActivity", started.getComponent().getClassName());
        assertEquals(APK_URL, started.getStringExtra("url"));
    }

    @Test
    public void noBannerWhenTheKnownReleaseIsNotNewer() {
        store().saveLatestKnown(
                new VersionInfo(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, APK_URL));
        MainActivity activity = launch();

        assertNull(activity.findViewById(R.id.update_banner));
    }

    @Test
    public void freshStartNeverTouchesTheJsonSource() {
        runChecksInline();
        MainActivity.versionJsonSourceOverride = () -> {
            throw new AssertionError("Świeży start nie może dotykać sieci");
        };

        MainActivity activity = launch();

        assertNull(activity.findViewById(R.id.update_banner));
    }

    @Test
    public void staleLastCheckFetchesAndShowsTheBanner() {
        long stale = System.currentTimeMillis() - UpdateChecker.CHECK_INTERVAL_MS - 1000;
        store().saveLastCheckMillis(stale);
        runChecksInline();
        MainActivity.versionJsonSourceOverride = () -> "{"
                + "\"versionCode\": 999, \"versionName\": \"9.9\","
                + "\"apkUrl\": \"" + APK_URL + "\"}";

        MainActivity activity = launch();

        assertNotNull("Po udanym sprawdzeniu baner powinien się pojawić",
                activity.findViewById(R.id.update_banner));
        assertTrue("Znacznik czasu sprawdzenia powinien być odświeżony",
                store().loadLastCheckMillis(0) > stale);
    }

    @Test
    public void failedFetchStillRecordsTheAttempt() {
        long stale = System.currentTimeMillis() - UpdateChecker.CHECK_INTERVAL_MS - 1000;
        store().saveLastCheckMillis(stale);
        runChecksInline();
        MainActivity.versionJsonSourceOverride = () -> {
            throw new java.io.IOException("brak sieci");
        };

        MainActivity activity = launch();

        assertNull("Nieudane sprawdzenie nie pokazuje banera",
                activity.findViewById(R.id.update_banner));
        assertTrue("Nieudana próba też odsuwa kolejną o dobę (bez młócenia sieci)",
                store().loadLastCheckMillis(0) > stale);
    }
}
