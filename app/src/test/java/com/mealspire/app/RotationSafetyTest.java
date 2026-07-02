package com.mealspire.app;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/**
 * The UI is built programmatically and holds its state (proposals, an open
 * recipe) only in the Activity. Recreating the Activity on rotation would wipe
 * that state, so the manifest must declare configChanges for orientation — this
 * test pins that contract.
 */
public class RotationSafetyTest {

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

    @Test
    public void manifestKeepsActivityAliveAcrossRotation() throws IOException {
        File manifest = new File(repoRoot(), "app/src/main/AndroidManifest.xml");
        assertTrue("Expected app/src/main/AndroidManifest.xml", manifest.isFile());
        String xml = new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8);
        assertTrue("MainActivity should declare android:configChanges with orientation",
                xml.contains("android:configChanges") && xml.contains("orientation")
                        && xml.contains("screenSize"));
    }
}
