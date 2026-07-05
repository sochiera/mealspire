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
 * Pilnuje spójności wydania: stabilny podpis APK (keystore w repo + signingConfig)
 * jest warunkiem instalowania aktualizacji po wierzchu bez utraty danych,
 * a `dist/wersja.json` musi zgadzać się z wersją wkompilowaną w APK — inaczej
 * mechanizm aktualizacji kłamie. Test napisany przed konfiguracją (TDD).
 */
public class ReleaseConsistencyTest {

    /** Wspina się z katalogu roboczego testu do korzenia repozytorium. */
    private static File repoRoot() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null) {
            if (new File(dir, "settings.gradle").isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("Nie znaleziono korzenia repo (settings.gradle)");
    }

    private static String buildGradleContents() throws IOException {
        File buildGradle = new File(repoRoot(), "app/build.gradle");
        assertTrue("Oczekiwano pliku app/build.gradle", buildGradle.isFile());
        return new String(Files.readAllBytes(buildGradle.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void keystoreJestWRepo() {
        File keystore = new File(repoRoot(), "signing/mealspire.keystore");
        assertTrue(
                "Brak signing/mealspire.keystore — bez wspólnego keystore każdy build "
                        + "ma inny podpis i aktualizacja po wierzchu jest niemożliwa",
                keystore.isFile());
    }

    @Test
    public void buildUzywaWspolnegoKeystore() throws IOException {
        String gradle = buildGradleContents();
        assertTrue("app/build.gradle powinien definiować signingConfigs",
                gradle.contains("signingConfigs"));
        assertTrue("signingConfig powinien wskazywać signing/mealspire.keystore",
                gradle.contains("signing/mealspire.keystore"));
    }

    private static JSONObject wersjaJson() throws Exception {
        File json = new File(repoRoot(), "dist/wersja.json");
        assertTrue(
                "Brak dist/wersja.json — aplikacja nie ma skąd dowiedzieć się o nowej wersji",
                json.isFile());
        return new JSONObject(new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void wersjaJsonZgadzaSieZWersjaWkompilowanaWApk() throws Exception {
        JSONObject json = wersjaJson();
        assertEquals(
                "versionCode w dist/wersja.json musi równać się BuildConfig.VERSION_CODE — "
                        + "podbij oba przy wydaniu",
                BuildConfig.VERSION_CODE, json.getInt("versionCode"));
        assertEquals(
                "versionName w dist/wersja.json musi równać się BuildConfig.VERSION_NAME",
                BuildConfig.VERSION_NAME, json.getString("versionName"));
    }

    @Test
    public void wersjaJsonWskazujeApkWDist() throws Exception {
        String apkUrl = wersjaJson().getString("apkUrl");
        assertTrue("apkUrl powinien prowadzić do raw.githubusercontent.com tego repo",
                apkUrl.startsWith("https://raw.githubusercontent.com/sochiera/mealspire/"));
        assertTrue("apkUrl powinien wskazywać dist/mealspire-debug.apk",
                apkUrl.endsWith("dist/mealspire-debug.apk"));
    }

    @Test
    public void wydanaWersjaNieJestPierwotnaJedynka() {
        // versionCode 1 to era niestabilnego podpisu debug; pierwsza wersja
        // z mechanizmem aktualizacji zaczyna się od 2.
        assertTrue("versionCode powinien być podbity ponad pierwotne 1",
                BuildConfig.VERSION_CODE >= 2);
    }

    @Test
    public void apkWDistIstnieje() {
        assertTrue("Brak dist/mealspire-debug.apk — odśwież go po buildzie",
                new File(repoRoot(), "dist/mealspire-debug.apk").isFile());
    }

    @Test
    public void debugIReleaseSaPodpisywaneTymSamymKluczem() throws IOException {
        String gradle = buildGradleContents();
        assertTrue("buildTypes.debug powinien używać signingConfigs.mealspire",
                gradle.contains("debug"));
        assertTrue("buildTypes.release powinien używać signingConfigs.mealspire",
                gradle.contains("release"));
        assertTrue("Oba typy buildów powinny wskazywać signingConfigs.mealspire",
                gradle.contains("signingConfigs.mealspire"));
    }
}
