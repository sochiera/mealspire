package com.mealspire.app.update;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.mealspire.app.BuildConfig;
import com.mealspire.app.domain.VersionInfo;
import com.mealspire.app.domain.VersionInfoParser;
import com.mealspire.app.net.HttpVersionJsonFetcher;
import com.mealspire.app.storage.SharedPreferencesUpdateStateStore;
import com.mealspire.app.ui.Ui;

/** Foreground, user-initiated update. No exported component or public file URI. */
public class UpdateActivity extends Activity {
    static final String RESULT_ACTION = "com.mealspire.app.UPDATE_RESULT";
    private static final int PERMISSION_REQUEST = 70;
    private TextView message;
    private ProgressBar progress;
    private Button retry;
    private Thread worker;
    private PackageUpdateInstaller installer;
    private int sessionId = -1;
    private boolean committed;
    private volatile boolean destroyed;

    public static Intent intent(Context context, VersionInfo info) {
        return new Intent(context, UpdateActivity.class)
                .putExtra("code", info.getVersionCode()).putExtra("name", info.getVersionName())
                .putExtra("url", info.getApkUrl()).putExtra("sha256", info.getSha256());
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        installer = createInstaller();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Ui.BACKGROUND);
        layout.setPadding(Ui.dp(this, 20), Ui.dp(this, 28), Ui.dp(this, 20), Ui.dp(this, 24));

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.mealspire.app.R.drawable.ic_brand_mark);
        logo.setScaleType(ImageView.ScaleType.FIT_XY);
        final int corner = Ui.dp(this, 16);
        logo.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), corner);
            }
        });
        logo.setClipToOutline(true);
        layout.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56)));
        layout.addView(Ui.headline(this, "Aktualizacja Mealspire", 26), Ui.marginTop(this, 18));

        LinearLayout card = Ui.card(this);
        message = Ui.body(this, "");
        message.setId(com.mealspire.app.R.id.update_message);
        message.setTextSize(17);
        card.addView(message, Ui.matchWrap());
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(ColorStateList.valueOf(Ui.ACCENT));
        progress.setIndeterminateTintList(ColorStateList.valueOf(Ui.ACCENT));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.OUTLINE));
        card.addView(progress, Ui.marginTop(this, 14));
        layout.addView(card, Ui.marginTop(this, 18));

        retry = new Button(this);
        retry.setText("Spróbuj ponownie");
        Ui.primary(retry);
        retry.setOnClickListener(v -> requestInstall());
        Button cancel = new Button(this);
        cancel.setText("Wróć do Mealspire");
        Ui.ghost(cancel);
        cancel.setOnClickListener(v -> finish());
        layout.addView(retry, Ui.marginTop(this, 18));
        layout.addView(cancel, Ui.marginTop(this, 8));
        TextView note = Ui.text(this, "Pobrany plik jest sprawdzany (SHA-256), a instalację "
                + "zawsze potwierdzasz w oknie systemowym. Twoje dane zostają.", 13, Ui.INK_SOFT);
        layout.addView(note, Ui.marginTop(this, 20));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        scroll.addView(layout);
        setContentView(scroll);
        if (state != null && state.getBoolean("committed")) {
            committed = true;
            sessionId = state.getInt("session", -1);
            busy("Oczekiwanie na wynik instalacji…");
        } else if (state != null && RESULT_ACTION.equals(getIntent().getAction())) {
            error(state.getString("message", "Możesz spróbować ponownie."));
        } else if (!RESULT_ACTION.equals(getIntent().getAction())) requestInstall();
        if (state == null && RESULT_ACTION.equals(getIntent().getAction())) handleResult(getIntent());
    }

    private void busy(String text) {
        message.setText(text);
        progress.setIndeterminate(true);
        progress.setVisibility(android.view.View.VISIBLE);
        retry.setVisibility(android.view.View.GONE);
    }
    private void error(String text) {
        message.setText(text);
        progress.setVisibility(android.view.View.GONE);
        retry.setVisibility(android.view.View.VISIBLE);
    }

    private void requestInstall() {
        if (worker != null || committed) return;
        VersionInfo info = new VersionInfo(getIntent().getIntExtra("code", 0),
                getIntent().getStringExtra("name"), getIntent().getStringExtra("url"),
                getIntent().getStringExtra("sha256"));
        if (info.getVersionCode() <= BuildConfig.VERSION_CODE || !info.hasSha256()) {
            error("Wydanie nie ma danych do bezpiecznej aktualizacji. Sprawdź ponownie później.");
            retry.setVisibility(android.view.View.GONE);
            return;
        }
        if (!hasInstallPermission()) {
            busy(Build.VERSION.SDK_INT >= 26
                    ? "Zezwól Mealspire na instalowanie aplikacji, potem wróć tutaj."
                    : "Włącz „Nieznane źródła” w ustawieniach zabezpieczeń, potem wróć tutaj.");
            Intent settings = Build.VERSION.SDK_INT >= 26
                    ? new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))
                    : new Intent(Settings.ACTION_SECURITY_SETTINGS);
            try { startActivityForResult(settings, PERMISSION_REQUEST); }
            catch (RuntimeException e) { error("Nie udało się otworzyć ustawień instalacji."); }
            return;
        }
        busy("Pobieranie aktualizacji " + info.getVersionName() + "…");
        worker = new Thread(() -> {
            final long[] lastProgress = {0};
            try {
                // The APK URL points at mutable main: refresh metadata on every attempt.
                VersionInfo fresh = new VersionInfoParser().parse(fetchLatestJson());
                if (fresh == null || !fresh.hasSha256() || fresh.getVersionCode() <= BuildConfig.VERSION_CODE)
                    throw new java.io.IOException("Brak nowszego wydania do bezpiecznej aktualizacji.");
                new SharedPreferencesUpdateStateStore(this).saveLatestKnown(fresh);
                int staged = installer.stage(fresh, (bytes, total) -> {
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now - lastProgress[0] < 100 && bytes != total) return;
                    lastProgress[0] = now;
                    runOnUiThread(() -> {
                        if (destroyed) return;
                        progress.setIndeterminate(total <= 0);
                        if (total > 0) progress.setProgress((int) (bytes * 100 / total));
                        message.setText("Pobieranie aktualizacji: " + (bytes / 1024) + " KB"
                                + (total > 0 ? " / " + (total / 1024) + " KB" : ""));
                    });
                });
                runOnUiThread(() -> {
                    worker = null;
                    if (destroyed || isFinishing()) { installer.abandon(staged); return; }
                    setIntent(intent(this, fresh));
                    sessionId = staged;
                    commit();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    worker = null;
                    if (!destroyed) error("Nie udało się pobrać aktualizacji. "
                            + (e instanceof java.io.IOException ? e.getMessage() : "Spróbuj ponownie."));
                });
            }
        }, "mealspire-apk-download");
        worker.start();
    }

    protected PackageUpdateInstaller createInstaller() { return new PackageUpdateInstaller(this); }

    protected String fetchLatestJson() throws java.io.IOException {
        return new HttpVersionJsonFetcher().fetchJson();
    }

    boolean hasInstallPermission() {
        if (Build.VERSION.SDK_INT >= 26) return getPackageManager().canRequestPackageInstalls();
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1;
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PERMISSION_REQUEST) {
            if (hasInstallPermission()) requestInstall();
            else error("Nie przyznano zgody na instalację. Możesz spróbować ponownie.");
        }
    }

    private void commit() {
        try (PackageInstaller.Session session = getPackageManager().getPackageInstaller().openSession(sessionId)) {
            // API 35 rejects immutable status receivers. Explicit, non-exported target.
            Intent callback = new Intent(this, UpdateActivity.class).setAction(RESULT_ACTION)
                    .putExtra("code", getIntent().getIntExtra("code", 0))
                    .putExtra("name", getIntent().getStringExtra("name"))
                    .putExtra("url", getIntent().getStringExtra("url"))
                    .putExtra("sha256", getIntent().getStringExtra("sha256"))
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            Bundle options = null;
            if (Build.VERSION.SDK_INT >= 35) {
                ActivityOptions activityOptions = ActivityOptions.makeBasic();
                activityOptions.setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                options = activityOptions.toBundle();
            }
            PendingIntent status = PendingIntent.getActivity(this, sessionId, callback, flags, options);
            session.commit(status.getIntentSender());
            committed = true;
            busy("Potwierdź instalację w oknie systemowym…");
        } catch (Exception e) {
            installer.abandon(sessionId);
            sessionId = -1;
            error("Nie udało się rozpocząć instalacji. Spróbuj ponownie.");
        }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (RESULT_ACTION.equals(intent.getAction()) && sessionId >= 0 && acceptsResult(intent)) {
            setIntent(intent);
            handleResult(intent);
        }
    }
    private boolean acceptsResult(Intent intent) {
        int resultSession = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        return resultSession >= 0 && worker == null && (sessionId < 0 || resultSession == sessionId);
    }
    private void handleResult(Intent intent) {
        if (!acceptsResult(intent)) return;
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) {
                committed = true;
                busy("Potwierdź instalację w oknie systemowym…");
                try { startActivity(confirmation); return; }
                catch (RuntimeException ignored) { }
            }
            installer.abandon(sessionId);
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            committed = false;
            sessionId = -1;
            message.setText("Aktualizacja została zainstalowana.");
            progress.setVisibility(android.view.View.GONE);
            retry.setVisibility(android.view.View.GONE);
            return;
        }
        committed = false;
        sessionId = -1;
        if (status == PackageInstaller.STATUS_FAILURE_ABORTED)
            error("Instalacja została anulowana. Możesz spróbować ponownie.");
        else if (status == PackageInstaller.STATUS_FAILURE_STORAGE)
            error("Brak miejsca na aktualizację. Zwolnij miejsce i spróbuj ponownie.");
        else if (status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE)
            error("Aktualizacja jest niezgodna z obecną instalacją lub urządzeniem. Twoje dane pozostają bez zmian.");
        else error("Nie udało się zainstalować aktualizacji. Sprawdź zgodę na instalację i spróbuj ponownie.");
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("message", message.getText().toString());
        state.putBoolean("committed", committed);
        state.putInt("session", sessionId);
        super.onSaveInstanceState(state);
    }
    @Override public void finish() {
        cancelDownload();
        super.finish();
    }
    private void cancelDownload() {
        destroyed = true;
        if (worker != null) worker.interrupt();
        if (installer != null && !committed) installer.cancel();
    }
    @Override protected void onDestroy() {
        cancelDownload();
        if (!committed && sessionId >= 0) installer.abandon(sessionId);
        super.onDestroy();
    }
}
