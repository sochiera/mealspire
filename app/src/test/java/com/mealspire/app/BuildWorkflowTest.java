package com.mealspire.app;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/**
 * Verifies that a GitHub Actions workflow exists which builds the Android
 * package (APK) on GitHub. Driven by TDD: the test was written before the
 * workflow file and describes the contract we expect from CI.
 */
public class BuildWorkflowTest {

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

    private static String workflowContents() throws IOException {
        File workflow = new File(repoRoot(), ".github/workflows/build.yml");
        assertTrue(
                "Expected GitHub Actions workflow at .github/workflows/build.yml",
                workflow.isFile());
        return new String(Files.readAllBytes(workflow.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void workflowFileExists() throws IOException {
        // workflowContents() asserts existence.
        assertTrue(!workflowContents().isEmpty());
    }

    @Test
    public void workflowTriggersOnPushAndPullRequest() throws IOException {
        String yaml = workflowContents();
        assertTrue("Workflow should trigger on push", yaml.contains("push"));
        assertTrue("Workflow should trigger on pull_request", yaml.contains("pull_request"));
    }

    @Test
    public void workflowSetsUpJava17() throws IOException {
        String yaml = workflowContents();
        assertTrue("Workflow should use the setup-java action", yaml.contains("actions/setup-java"));
        assertTrue("Workflow should target Java 17", yaml.contains("17"));
    }

    @Test
    public void workflowBuildsTheApkPackage() throws IOException {
        String yaml = workflowContents();
        assertTrue(
                "Workflow should assemble the debug APK package",
                yaml.contains("assembleDebug"));
    }

    @Test
    public void workflowRunsUnitTests() throws IOException {
        String yaml = workflowContents();
        assertTrue("Workflow should run the JVM unit tests", yaml.contains("gradlew test"));
    }

    @Test
    public void workflowUploadsApkArtifact() throws IOException {
        String yaml = workflowContents();
        assertTrue(
                "Workflow should upload the built APK as an artifact",
                yaml.contains("actions/upload-artifact"));
    }
}
