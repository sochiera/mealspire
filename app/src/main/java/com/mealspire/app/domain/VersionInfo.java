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
    private final String sha256;

    public VersionInfo(int versionCode, String versionName, String apkUrl) {
        this(versionCode, versionName, apkUrl, "");
    }

    public VersionInfo(int versionCode, String versionName, String apkUrl, String sha256) {
        this.sha256 = sha256 == null ? "" : sha256.trim();
        this.versionCode = versionCode;
        this.versionName = versionName == null ? "" : versionName.trim();
        this.apkUrl = apkUrl == null ? "" : apkUrl.trim();
    }

    public String getSha256() { return sha256; }
    public boolean hasSha256() { return sha256.matches("(?i)[0-9a-f]{64}"); }

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
