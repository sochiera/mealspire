package com.mealspire.app.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure decisions of the update mechanism: when to hit the network (at most
 * once a day) and whether a fetched VersionInfo means an update is available.
 */
public class UpdateCheckerTest {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final long NOW = 1_750_000_000_000L;

    private final UpdateChecker checker = new UpdateChecker();

    @Test
    public void checkIntervalIsTwentyFourHours() {
        assertTrue(UpdateChecker.CHECK_INTERVAL_MS == DAY_MS);
    }

    @Test
    public void versionUrlPointsAtVersionJsonInThisRepo() {
        assertTrue(UpdateChecker.VERSION_URL.startsWith(
                "https://raw.githubusercontent.com/sochiera/mealspire/"));
        assertTrue(UpdateChecker.VERSION_URL.endsWith("dist/wersja.json"));
    }

    @Test
    public void doesNotCheckBeforeTheIntervalPasses() {
        assertFalse(checker.shouldCheck(NOW - DAY_MS + 1, NOW));
        assertFalse(checker.shouldCheck(NOW, NOW));
    }

    @Test
    public void checksOnceTheIntervalHasPassed() {
        assertTrue(checker.shouldCheck(NOW - DAY_MS, NOW));
        assertTrue(checker.shouldCheck(NOW - 3 * DAY_MS, NOW));
    }

    @Test
    public void clockMovedBackwardsDoesNotBlockTheCheckForever() {
        // A last-check timestamp from the "future" (clock reset, restored
        // backup) must not freeze the mechanism until that future arrives.
        assertTrue(checker.shouldCheck(NOW + 30 * DAY_MS, NOW));
    }

    @Test
    public void higherVersionCodeMeansUpdateAvailable() {
        assertTrue(checker.isUpdateAvailable(2,
                new VersionInfo(3, "1.2", "https://x/a.apk")));
    }

    @Test
    public void equalOrLowerVersionCodeMeansNoUpdate() {
        assertFalse(checker.isUpdateAvailable(2,
                new VersionInfo(2, "1.1", "https://x/a.apk")));
        assertFalse(checker.isUpdateAvailable(2,
                new VersionInfo(1, "1.0", "https://x/a.apk")));
    }

    @Test
    public void nullVersionInfoMeansNoUpdate() {
        assertFalse(checker.isUpdateAvailable(2, null));
    }
}
