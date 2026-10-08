package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Guards release consistency. A stable APK signature (shared keystore +
 * signingConfig) is what makes in-place updates possible without losing user
 * data, and dist/wersja.json must match the version compiled into the APK —
 * otherwise the update mechanism lies to the user. Written test-first (TDD).
 */
public class ReleaseConsistencyTest {

    /** Walks up from the test working directory to the repository root. */
    private static File repoRoot() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null) {
            if (new File(dir, "settings.gradle").isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("Could not locate repository root (settings.gradle)");
    }

    private static String buildGradleContents() throws IOException {
        File buildGradle = new File(repoRoot(), "app/build.gradle");
        assertTrue("Expected app/build.gradle to exist", buildGradle.isFile());
        return new String(Files.readAllBytes(buildGradle.toPath()), StandardCharsets.UTF_8);
    }

    private static JSONObject versionJson() throws Exception {
        File json = new File(repoRoot(), "dist/wersja.json");
        assertTrue(
                "dist/wersja.json is missing — the app has no way to learn about a new version",
                json.isFile());
        return new JSONObject(new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void keystoreIsCommittedToTheRepo() {
        File keystore = new File(repoRoot(), "signing/mealspire.keystore");
        assertTrue(
                "signing/mealspire.keystore is missing — without a shared keystore every "
                        + "build has a different signature and in-place updates are impossible",
                keystore.isFile());
    }

    @Test
    public void buildUsesTheSharedKeystore() throws IOException {
        String gradle = buildGradleContents();
        assertTrue("app/build.gradle should define signingConfigs",
                gradle.contains("signingConfigs"));
        assertTrue("signingConfig should point at signing/mealspire.keystore",
                gradle.contains("signing/mealspire.keystore"));
    }

    @Test
    public void debugAndReleaseAreSignedWithTheSameKey() throws IOException {
        String gradle = buildGradleContents();
        assertTrue("buildTypes.debug should use signingConfigs.mealspire",
                gradle.contains("debug"));
        assertTrue("buildTypes.release should use signingConfigs.mealspire",
                gradle.contains("release"));
        assertTrue("Both build types should point at signingConfigs.mealspire",
                gradle.contains("signingConfigs.mealspire"));
    }

    @Test
    public void versionJsonMatchesTheVersionCompiledIntoTheApk() throws Exception {
        JSONObject json = versionJson();
        assertEquals(
                "versionCode in dist/wersja.json must equal BuildConfig.VERSION_CODE — "
                        + "bump both when releasing",
                BuildConfig.VERSION_CODE, json.getInt("versionCode"));
        assertEquals(
                "versionName in dist/wersja.json must equal BuildConfig.VERSION_NAME",
                BuildConfig.VERSION_NAME, json.getString("versionName"));
    }

    @Test
    public void versionJsonPointsAtTheApkInDist() throws Exception {
        String apkUrl = versionJson().getString("apkUrl");
        assertTrue("apkUrl should point at raw.githubusercontent.com of this repo",
                apkUrl.startsWith("https://raw.githubusercontent.com/sochiera/mealspire/"));
        assertTrue("apkUrl should point at dist/mealspire-debug.apk",
                apkUrl.endsWith("dist/mealspire-debug.apk"));
    }

    @Test
    public void releasedVersionIsPastTheOriginalOne() {
        // versionCode 1 is the era of the unstable debug signature; the first
        // release with the update mechanism starts at 2.
        assertTrue("versionCode should be bumped past the original 1",
                BuildConfig.VERSION_CODE >= 2);
    }

    @Test
    public void sha256MatchesPublishedApk() throws Exception {
        byte[] bytes = Files.readAllBytes(new File(repoRoot(), "dist/mealspire-debug.apk").toPath());
        StringBuilder hash = new StringBuilder();
        for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
            hash.append(String.format("%02x", b & 255));
        assertEquals(hash.toString(), versionJson().getString("sha256"));
    }

    @Test
    public void apkInDistExists() {
        assertTrue("dist/mealspire-debug.apk is missing — refresh it after building",
                new File(repoRoot(), "dist/mealspire-debug.apk").isFile());
    }
}
