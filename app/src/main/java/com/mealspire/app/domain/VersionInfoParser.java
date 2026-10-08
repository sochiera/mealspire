package com.mealspire.app.domain;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Turns the raw body of dist/wersja.json into a {@link VersionInfo}. Returns
 * null for anything malformed or suspicious (missing fields, non-positive
 * version, non-HTTPS URL) — bad metadata silently disables the update check
 * instead of crashing or misleading the user.
 */
public final class VersionInfoParser {

    public VersionInfo parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(json);
            int versionCode = object.getInt("versionCode");
            String versionName = object.getString("versionName");
            String apkUrl = object.getString("apkUrl");
            if (versionCode <= 0 || versionName.trim().isEmpty()
                    || !apkUrl.trim().startsWith("https://")) {
                return null;
            }
            return new VersionInfo(versionCode, versionName, apkUrl, object.optString("sha256", ""));
        } catch (JSONException e) {
            return null;
        }
    }
}
