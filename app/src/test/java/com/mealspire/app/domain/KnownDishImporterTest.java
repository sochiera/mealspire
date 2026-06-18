package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class KnownDishImporterTest {

    private static class StubClient implements ClaudeClient {
        String lastUser;
        String response;

        StubClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            this.lastUser = userPrompt;
            return response;
        }
    }

    private static class StubFetcher implements PageFetcher {
        String text;
        boolean fail;
        String requestedUrl;

        @Override
        public String fetchText(String url) throws IOException {
            this.requestedUrl = url;
            if (fail) {
                throw new IOException("network down");
            }
            return text;
        }
    }

    private KnownDishImporter importer(ClaudeClient client, PageFetcher fetcher) {
        return new KnownDishImporter(client, fetcher,
                new KnownDishPromptBuilder(), new RecipeTextParser());
    }

    @Test
    public void importsFromDescription() throws Exception {
        StubClient client = new StubClient("Spaghetti Carbonara\n\nMakaron z jajkiem i boczkiem.");
        StubFetcher fetcher = new StubFetcher();

        CookbookEntry entry = importer(client, fetcher)
                .importDish("kremowy makaron z boczkiem i jajkiem");

        assertEquals("Spaghetti Carbonara", entry.getTitle());
        assertTrue(entry.getRecipe().contains("Makaron"));
        assertEquals("opis", entry.getSource());
        assertTrue(client.lastUser.contains("kremowy makaron"));
    }

    @Test
    public void importsFromUrlUsingFetchedPageText() throws Exception {
        StubClient client = new StubClient("Tarta z warzywami\n\nSpód i nadzienie warzywne.");
        StubFetcher fetcher = new StubFetcher();
        fetcher.text = "Najlepsza tarta z cukinią i papryką, przepis krok po kroku";

        CookbookEntry entry = importer(client, fetcher)
                .importDish("https://example.com/tarta");

        assertEquals("Tarta z warzywami", entry.getTitle());
        assertEquals("https://example.com/tarta", entry.getSource());
        assertEquals("https://example.com/tarta", fetcher.requestedUrl);
        assertTrue(client.lastUser.contains("cukinią"));
    }

    @Test
    public void fallsBackToUrlAsTextWhenFetchFails() throws Exception {
        StubClient client = new StubClient("Coś z linku\n\nopis");
        StubFetcher fetcher = new StubFetcher();
        fetcher.fail = true;

        CookbookEntry entry = importer(client, fetcher)
                .importDish("https://example.com/x");

        assertEquals("Coś z linku", entry.getTitle());
        assertEquals("https://example.com/x", entry.getSource());
        assertTrue(client.lastUser.contains("https://example.com/x"));
    }

    @Test(expected = IOException.class)
    public void blankInputThrows() throws Exception {
        importer(new StubClient("x"), new StubFetcher()).importDish("   ");
    }
}
