package com.mealspire.app;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/**
 * Pilnuje spójności wydania: stabilny podpis APK (keystore w repo + signingConfig)
 * jest warunkiem instalowania aktualizacji po wierzchu bez utraty danych.
 * Test napisany przed konfiguracją (TDD) — opisuje kontrakt wydania.
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
