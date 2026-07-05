package com.mealspire.app.domain;

/**
 * Metadata of the newest published release, as advertised by dist/wersja.json
 * in the repository. Compared against the installed BuildConfig.VERSION_CODE
 * to decide whether the update banner should be shown.
 */
public final class VersionInfo {

    private final int versionCode;
    private final String versionName;
    private final String apkUrl;

    public VersionInfo(int versionCode, String versionName, String apkUrl) {
        this.versionCode = versionCode;
        this.versionName = versionName == null ? "" : versionName.trim();
        this.apkUrl = apkUrl == null ? "" : apkUrl.trim();
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getApkUrl() {
        return apkUrl;
    }
}
