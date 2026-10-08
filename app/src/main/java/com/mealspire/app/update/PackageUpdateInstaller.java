package com.mealspire.app.update;

import android.content.Context;
import android.content.pm.PackageInstaller;
import com.mealspire.app.domain.VersionInfo;
import java.io.*;
import java.net.*;

/** Streams into private installer storage. A failed/unverified session is never committed. */
public class PackageUpdateInstaller {
    private final PackageInstaller installer;
    private final String packageName;
    private volatile HttpURLConnection activeConnection;
    private volatile boolean cancelled;
    public PackageUpdateInstaller(Context context) {
        installer = context.getPackageManager().getPackageInstaller();
        packageName = context.getPackageName();
        // Recover unfinished staging storage after process death. Sealed sessions
        // belong to the system confirmation flow and must be left alone.
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            for (PackageInstaller.SessionInfo old : installer.getMySessions()) {
                if (!old.isActive() && !old.isSealed()) abandon(old.getSessionId());
            }
        }
    }

    public int stage(VersionInfo info, VerifiedApkTransfer.Progress progress) throws IOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Pobieranie anulowane.");
        if (!info.hasSha256()) throw new IOException("Wydanie nie ma sumy SHA-256. Spróbuj później.");
        HttpURLConnection connection = null;
        int id = -1;
        boolean staged = false;
        try {
            URL url = new URL(info.getApkUrl());
            for (int redirects = 0; ; redirects++) {
                if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IOException("Aktualizacja wymaga HTTPS.");
                connection = openConnection(url);
                activeConnection = connection;
                if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Pobieranie anulowane.");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept-Encoding", "identity");
                int status = connection.getResponseCode();
                if (status == 200) break;
                if ((status == 301 || status == 302 || status == 303 || status == 307 || status == 308)
                        && redirects < 5) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Niepoprawne przekierowanie aktualizacji.");
                    URL next = new URL(url, location);
                    connection.disconnect();
                    url = next;
                } else throw new IOException("Nie udało się pobrać aktualizacji (HTTP " + status + ").");
            }
            long length = connection.getContentLength();
            if (length > VerifiedApkTransfer.MAX_BYTES) throw new IOException("Plik aktualizacji jest zbyt duży.");
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(packageName);
            if (android.os.Build.VERSION.SDK_INT >= 31)
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
            id = installer.createSession(params);
            try (PackageInstaller.Session session = installer.openSession(id);
                 InputStream input = connection.getInputStream();
                 OutputStream output = session.openWrite("base.apk", 0, length >= 0 ? length : -1)) {
                VerifiedApkTransfer.copy(input, output, info.getSha256(), length, progress);
                session.fsync(output);
            }
            if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Pobieranie anulowane.");
            staged = true;
            return id;
        } finally {
            activeConnection = null;
            if (connection != null) connection.disconnect();
            if (!staged && id >= 0) abandon(id);
        }
    }
    public void cancel() {
        cancelled = true;
        HttpURLConnection connection = activeConnection;
        if (connection != null) connection.disconnect();
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    public void abandon(int id) {
        try { installer.abandonSession(id); } catch (RuntimeException ignored) { }
    }
}
