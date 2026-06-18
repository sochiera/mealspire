package com.mealspire.app.domain;

import java.io.IOException;

/**
 * Fetches a web page and returns its readable plain text. Abstracted so the
 * dish importer can be unit-tested without real network access.
 */
public interface PageFetcher {
    String fetchText(String url) throws IOException;
}
