package com.mealspire.app.update;

import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.provider.Settings;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.domain.VersionInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class UpdateActivityTest {
    private Intent update(String hash) {
        return UpdateActivity.intent(ApplicationProvider.getApplicationContext(),
                new VersionInfo(999, "9.9", "https://example.invalid/update.apk", hash));
    }
    private String message(UpdateActivity activity) {
        return activity.<TextView>findViewById(com.mealspire.app.R.id.update_message)
                .getText().toString();
    }
    @org.junit.Before public void resetWorkerGate() {
        RefreshActivity.gate = new java.util.concurrent.CountDownLatch(0);
        RefreshActivity.started = new java.util.concurrent.CountDownLatch(1);
    }
    public static class RefreshActivity extends UpdateActivity {
        static java.util.concurrent.CountDownLatch gate;
        static java.util.concurrent.CountDownLatch started;
        VersionInfo received;
        int createdSession;
        @Override boolean hasInstallPermission() { return true; }
        @Override protected String fetchLatestJson() {
            return "{\"versionCode\":1000,\"versionName\":\"10.0\",\"apkUrl\":\"https://example.invalid/new.apk\",\"sha256\":\""
                    + "b".repeat(64) + "\"}";
        }
        @Override protected PackageUpdateInstaller createInstaller() {
            return new PackageUpdateInstaller(this) {
                @Override public int stage(VersionInfo info, VerifiedApkTransfer.Progress progress) throws java.io.IOException {
                    received = info;
                    createdSession = getPackageManager().getPackageInstaller().createSession(
                            new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
                    started.countDown();
                    try { gate.await(); }
                    catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); }
                    return createdSession;
                }
            };
        }
    }
    private void awaitWorker(UpdateActivity activity) throws Exception {
        java.lang.reflect.Field field = UpdateActivity.class.getDeclaredField("worker");
        field.setAccessible(true);
        Thread thread = (Thread) field.get(activity);
        if (thread != null) {
            thread.join(5000);
            assertFalse("Fake downloader should finish without network", thread.isAlive());
        }
    }
    @Test public void refreshesCachedMetadataBeforeStaging() throws Exception {
        RefreshActivity activity = Robolectric.buildActivity(RefreshActivity.class, update("a".repeat(64))).setup().get();
        awaitWorker(activity);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(1000, activity.received.getVersionCode());
        assertEquals("b".repeat(64), activity.received.getSha256());
        assertEquals("https://example.invalid/new.apk", activity.getIntent().getStringExtra("url"));
        assertTrue(message(activity).contains("Potwierdź"));
    }
    @Test public void exitBeforeCommitAbandonsStagedSession() throws Exception {
        RefreshActivity.gate = new java.util.concurrent.CountDownLatch(1);
        RefreshActivity activity = Robolectric.buildActivity(RefreshActivity.class, update("a".repeat(64))).setup().get();
        assertTrue(RefreshActivity.started.await(5, java.util.concurrent.TimeUnit.SECONDS));
        activity.finish();
        RefreshActivity.gate.countDown();
        awaitWorker(activity);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertNull(activity.getPackageManager().getPackageInstaller().getSessionInfo(activity.createdSession));
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }
    @Test public void missingHashDoesNotDownloadOrOpenBrowser() {
        UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, update("")).setup().get();
        assertTrue(message(activity).contains("bezpiecznej aktualizacji"));
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }
    @Test @Config(sdk = 28) public void android8RequestsPermissionForMealspire() {
        UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, update("a".repeat(64))).setup().get();
        Intent settings = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, settings.getAction());
        assertEquals("package:com.mealspire.app", settings.getDataString());
        activity.onActivityResult(70, 0, null);
        assertTrue(message(activity).contains("Nie przyznano zgody"));
    }
    @Test @Config(sdk = 23) public void android6RequestsGlobalUnknownSources() {
        UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, update("a".repeat(64))).setup().get();
        assertEquals(Settings.ACTION_SECURITY_SETTINGS,
                Shadows.shadowOf(activity).getNextStartedActivity().getAction());
    }
    @Test public void pendingUserActionOpensSystemConfirmation() {
        Intent confirmation = new Intent("test.system.CONFIRM");
        Intent callback = update("a".repeat(64)).setAction(UpdateActivity.RESULT_ACTION)
                .putExtra(PackageInstaller.EXTRA_SESSION_ID, 42)
                .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
                .putExtra(Intent.EXTRA_INTENT, confirmation);
        UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, callback).setup().get();
        assertEquals(confirmation.getAction(), Shadows.shadowOf(activity).getNextStartedActivity().getAction());
    }
    @Test public void ignoresStatusFromOlderSession() throws Exception {
        UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, update("")).setup().get();
        java.lang.reflect.Field field = UpdateActivity.class.getDeclaredField("sessionId");
        field.setAccessible(true);
        field.setInt(activity, 222);
        activity.onNewIntent(update("a".repeat(64)).setAction(UpdateActivity.RESULT_ACTION)
                .putExtra(PackageInstaller.EXTRA_SESSION_ID, 111)
                .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE_ABORTED));
        assertEquals(222, field.getInt(activity));
        assertTrue(message(activity).contains("bezpiecznej aktualizacji"));
    }
    @Test public void ignoresLateConfirmationAfterTerminalResult() {
        UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, update("")).setup().get();
        activity.onNewIntent(update("a".repeat(64)).setAction(UpdateActivity.RESULT_ACTION)
                .putExtra(PackageInstaller.EXTRA_SESSION_ID, 111)
                .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
                .putExtra(Intent.EXTRA_INTENT, new Intent("test.system.CONFIRM")));
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        assertTrue(message(activity).contains("bezpiecznej aktualizacji"));
    }
    @Test public void recreationDoesNotReplayConfirmation() {
        Intent callback = update("a".repeat(64)).setAction(UpdateActivity.RESULT_ACTION)
                .putExtra(PackageInstaller.EXTRA_SESSION_ID, 42)
                .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
                .putExtra(Intent.EXTRA_INTENT, new Intent("test.system.CONFIRM"));
        org.robolectric.android.controller.ActivityController<UpdateActivity> controller =
                Robolectric.buildActivity(UpdateActivity.class, callback).setup();
        assertNotNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
        android.os.Bundle state = new android.os.Bundle();
        controller.saveInstanceState(state).pause().stop().destroy();
        UpdateActivity restored = Robolectric.buildActivity(UpdateActivity.class, callback)
                .create(state).start().resume().visible().get();
        assertNull(Shadows.shadowOf(restored).getNextStartedActivity());
        assertTrue(message(restored).contains("Oczekiwanie"));
    }
    @Test public void cancellationAndFailureAreVisibleWithoutDownload() {
        for (int status : new int[]{PackageInstaller.STATUS_FAILURE_ABORTED, PackageInstaller.STATUS_FAILURE_STORAGE}) {
            Intent callback = update("a".repeat(64)).setAction(UpdateActivity.RESULT_ACTION)
                    .putExtra(PackageInstaller.EXTRA_SESSION_ID, 42)
                    .putExtra(PackageInstaller.EXTRA_STATUS, status);
            UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class, callback).setup().get();
            assertTrue(message(activity).contains(status == PackageInstaller.STATUS_FAILURE_ABORTED
                    ? "anulowana" : "Brak miejsca"));
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
}
