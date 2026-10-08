package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Parses release metadata from dist/wersja.json. Contract: a valid JSON object
 * yields a VersionInfo, anything else yields null — the update check is not a
 * critical feature, so malformed data must silently disable it rather than
 * crash the app.
 */
public class VersionInfoParserTest {

    private final VersionInfoParser parser = new VersionInfoParser();

    private static final String VALID_JSON = "{"
            + "\"versionCode\": 7,"
            + "\"versionName\": \"1.6\","
            + "\"apkUrl\": \"https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk\""
            + "}";

    @Test
    public void acceptsSha256ButKeepsLegacyMetadataReadable() {
        VersionInfo info = new VersionInfoParser().parse("{\"versionCode\":999,\"versionName\":\"9.9\",\"apkUrl\":\"https://example.invalid/a.apk\",\"sha256\":\"" + "a".repeat(64) + "\"}");
        org.junit.Assert.assertNotNull(info);
        org.junit.Assert.assertTrue(info.hasSha256());
        org.junit.Assert.assertEquals("a".repeat(64), info.getSha256());
    }

    @Test
    public void parsesValidJson() {
        VersionInfo info = parser.parse(VALID_JSON);
        assertEquals(7, info.getVersionCode());
        assertEquals("1.6", info.getVersionName());
        assertEquals(
                "https://raw.githubusercontent.com/sochiera/mealspire/main/dist/mealspire-debug.apk",
                info.getApkUrl());
    }

    @Test
    public void ignoresExtraAndFutureFields() {
        String json = "{\"versionCode\": 3, \"versionName\": \"1.2\", "
                + "\"apkUrl\": \"https://example.com/a.apk\", \"sha256\": \"abc\", \"newField\": 1}";
        VersionInfo info = parser.parse(json);
        assertEquals(3, info.getVersionCode());
    }

    @Test
    public void missingVersionCodeYieldsNull() {
        assertNull(parser.parse("{\"versionName\": \"1.6\", \"apkUrl\": \"https://x/a.apk\"}"));
    }

    @Test
    public void missingVersionNameYieldsNull() {
        assertNull(parser.parse("{\"versionCode\": 7, \"apkUrl\": \"https://x/a.apk\"}"));
    }

    @Test
    public void missingApkUrlYieldsNull() {
        assertNull(parser.parse("{\"versionCode\": 7, \"versionName\": \"1.6\"}"));
    }

    @Test
    public void nonNumericVersionCodeYieldsNull() {
        assertNull(parser.parse(
                "{\"versionCode\": \"siedem\", \"versionName\": \"1.6\", \"apkUrl\": \"https://x/a.apk\"}"));
    }

    @Test
    public void nonPositiveVersionCodeYieldsNull() {
        assertNull(parser.parse(
                "{\"versionCode\": 0, \"versionName\": \"1.6\", \"apkUrl\": \"https://x/a.apk\"}"));
    }

    @Test
    public void nonHttpsApkUrlYieldsNull() {
        // An update must never be fetched over plain HTTP or from local files.
        assertNull(parser.parse(
                "{\"versionCode\": 7, \"versionName\": \"1.6\", \"apkUrl\": \"http://x/a.apk\"}"));
        assertNull(parser.parse(
                "{\"versionCode\": 7, \"versionName\": \"1.6\", \"apkUrl\": \"file:///sdcard/a.apk\"}"));
    }

    @Test
    public void garbageYieldsNull() {
        assertNull(parser.parse("to nie jest json"));
        assertNull(parser.parse("[]"));
        assertNull(parser.parse(""));
        assertNull(parser.parse(null));
    }
}
