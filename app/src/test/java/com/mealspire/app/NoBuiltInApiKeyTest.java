package com.mealspire.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

/**
 * The app must not ship a shared AI secret: no key injected at build time, no
 * (encrypted) key in resources, no Anthropic/Claude API path. AI runs only on
 * the user's own ChatGPT account.
 */
public class NoBuiltInApiKeyTest {

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
    public void buildConfigHasNoApiKeyField() {
        for (Field field : BuildConfig.class.getFields()) {
            String name = field.getName().toUpperCase(Locale.ROOT);
            assertFalse("BuildConfig must not carry a secret: " + name,
                    name.contains("KEY") || name.contains("SECRET") || name.contains("TOKEN"));
        }
    }

    @Test
    public void gradleBuildDoesNotInjectAnyKey() throws IOException {
        String gradle = read(new File(repoRoot(), "app/build.gradle")).toLowerCase(Locale.ROOT);
        assertFalse(gradle.contains("anthropic"));
        assertFalse(gradle.contains("api_key"));
        assertFalse(gradle.contains("api.key"));
        assertFalse(gradle.contains("openai"));
    }

    @Test
    public void productionCodeAndResourcesHaveNoSharedSecretOrClaudePath() throws IOException {
        List<File> files = new ArrayList<>();
        collect(new File(repoRoot(), "app/src/main"), files);
        assertTrue("expected production sources", files.size() > 10);
        for (File file : files) {
            String text = read(file);
            String lower = text.toLowerCase(Locale.ROOT);
            assertFalse(file + " talks to the Anthropic API", lower.contains("api.anthropic.com"));
            assertFalse(file + " sends an API-key header", lower.contains("x-api-key"));
            assertFalse(file + " ships an encrypted key", lower.contains("encrypted_api_key"));
            assertFalse(file + " embeds an Anthropic key", text.contains("sk-ant-"));
            assertFalse(file + " embeds an OpenAI key", text.matches("(?s).*\\bsk-(proj-)?[A-Za-z0-9]{20,}.*"));
        }
    }

    private static void collect(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, out);
            } else if (child.getName().endsWith(".java") || child.getName().endsWith(".xml")) {
                out.add(child);
            }
        }
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
