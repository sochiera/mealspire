package com.mealspire.app.domain;

import java.io.IOException;

/**
 * Fetches the raw body of dist/wersja.json. Abstracted so the update check and
 * its tests never depend on real HTTP — the JVM/Robolectric tests inject a fake.
 */
public interface VersionJsonSource {
    String fetchJson() throws IOException;
}
