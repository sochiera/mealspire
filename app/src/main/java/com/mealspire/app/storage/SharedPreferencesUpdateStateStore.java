package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.UpdateStateStore;
import com.mealspire.app.domain.VersionInfo;

/**
 * {@link UpdateStateStore} backed by {@link SharedPreferences}: the check
 * timestamp and the newest advertised release survive app restarts.
 */
public final class SharedPreferencesUpdateStateStore implements UpdateStateStore {

    private static final String PREFS_NAME = "mealspire_update_state";
    private static final String KEY_LAST_CHECK = "last_check_millis";
    private static final String KEY_LATEST_CODE = "latest_version_code";
    private static final String KEY_LATEST_NAME = "latest_version_name";
    private static final String KEY_LATEST_APK_URL = "latest_apk_url";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesUpdateStateStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public long loadLastCheckMillis(long nowMillis) {
        long stored = sharedPreferences.getLong(KEY_LAST_CHECK, -1);
        if (stored < 0) {
            saveLastCheckMillis(nowMillis);
            return nowMillis;
        }
        return stored;
    }

    @Override
    public void saveLastCheckMillis(long millis) {
        sharedPreferences.edit().putLong(KEY_LAST_CHECK, millis).apply();
    }

    @Override
    public VersionInfo loadLatestKnown() {
        int code = sharedPreferences.getInt(KEY_LATEST_CODE, -1);
        String name = sharedPreferences.getString(KEY_LATEST_NAME, null);
        String apkUrl = sharedPreferences.getString(KEY_LATEST_APK_URL, null);
        if (code <= 0 || name == null || apkUrl == null) {
            return null;
        }
        return new VersionInfo(code, name, apkUrl);
    }

    @Override
    public void saveLatestKnown(VersionInfo info) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (info == null) {
            editor.remove(KEY_LATEST_CODE)
                    .remove(KEY_LATEST_NAME)
                    .remove(KEY_LATEST_APK_URL);
        } else {
            editor.putInt(KEY_LATEST_CODE, info.getVersionCode())
                    .putString(KEY_LATEST_NAME, info.getVersionName())
                    .putString(KEY_LATEST_APK_URL, info.getApkUrl());
        }
        editor.apply();
    }
}
