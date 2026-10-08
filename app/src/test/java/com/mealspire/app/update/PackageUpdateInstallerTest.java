package com.mealspire.app.update;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.mealspire.app.domain.VersionInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class PackageUpdateInstallerTest {
    private static final byte[] APK = {1,2,3};
    private final Context context = ApplicationProvider.getApplicationContext();
    private class Installer extends PackageUpdateInstaller {
        int response = 200;
        boolean disconnected;
        String location;
        int connections;
        Installer() { super(context); }
        @Override protected HttpURLConnection openConnection(URL url) {
            connections++;
            return new HttpURLConnection(url) {
                public void connect() { }
                public void disconnect() { disconnected = true; }
                public boolean usingProxy() { return false; }
                public int getResponseCode() { return response; }
                public String getHeaderField(String name) { return location; }
                public int getContentLength() { return APK.length; }
                public InputStream getInputStream() { return new ByteArrayInputStream(APK); }
            };
        }
    }
    private VersionInfo info(String digest) {
        return new VersionInfo(999, "9.9", "https://example.invalid/a.apk", digest);
    }
    @Test public void stagesVerifiedBytesAndCanBeAbandoned() throws Exception {
        StringBuilder hash = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(APK)) hash.append(String.format("%02x", b & 255));
        Installer installer = new Installer();
        int id = installer.stage(info(hash.toString()), (done, total) -> {});
        assertNotNull(context.getPackageManager().getPackageInstaller().getSessionInfo(id));
        assertTrue(installer.disconnected);
        installer.abandon(id);
        assertNull(context.getPackageManager().getPackageInstaller().getSessionInfo(id));
    }
    @Test public void cleansUnfinishedStorageFromPreviousProcess() throws Exception {
        android.content.pm.PackageInstaller platform = context.getPackageManager().getPackageInstaller();
        int id = platform.createSession(new android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL));
        org.robolectric.util.ReflectionHelpers.setField(platform.getSessionInfo(id), "active", false);
        new Installer();
        assertNull(platform.getSessionInfo(id));
    }
    @Test public void refusesHttpsDowngradeAndRedirectLoops() throws Exception {
        for (String location : new String[]{"http://example.invalid/a.apk", "/loop"}) {
            Installer installer = new Installer();
            installer.response = 302;
            installer.location = location;
            try { installer.stage(info("a".repeat(64)), (done, total) -> {}); fail(); }
            catch (IOException expected) { }
            assertEquals(location.startsWith("http:") ? 1 : 6, installer.connections);
            assertTrue(installer.disconnected);
            assertTrue(context.getPackageManager().getPackageInstaller().getMySessions().isEmpty());
        }
    }
    @Test public void corruptDownloadAndHttpFailureLeaveNoSession() throws Exception {
        for (int response : new int[]{200, 503}) {
            Installer installer = new Installer();
            installer.response = response;
            try { installer.stage(info("0".repeat(64)), (done, total) -> {}); fail(); }
            catch (IOException expected) { }
            assertTrue(installer.disconnected);
            assertTrue(context.getPackageManager().getPackageInstaller().getMySessions().isEmpty());
        }
    }
}
