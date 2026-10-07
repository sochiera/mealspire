package com.mealspire.app.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoopbackCallbackServerTest {

    @Test
    public void redirectUriIsLoopbackIpWithCallbackPath() throws IOException {
        try (LoopbackCallbackServer server = new LoopbackCallbackServer()) {
            assertTrue(server.redirectUri().matches("http://127\\.0\\.0\\.1:\\d+/callback"));
        }
    }

    @Test
    public void returnsQueryOfCallbackAndIgnoresOtherPaths() throws Exception {
        ExecutorService browser = Executors.newSingleThreadExecutor();
        try (LoopbackCallbackServer server = new LoopbackCallbackServer()) {
            String base = server.redirectUri().replace("/callback", "");
            Future<int[]> statuses = browser.submit(() -> new int[]{
                    status(base + "/favicon.ico"),
                    status(server.redirectUri() + "?code=abc&state=xyz")});

            String query = server.awaitCallbackQuery(10000);

            assertEquals("code=abc&state=xyz", query);
            int[] s = statuses.get();
            assertEquals(404, s[0]);
            assertEquals(200, s[1]);
        } finally {
            browser.shutdownNow();
        }
    }

    @Test(expected = IOException.class)
    public void timesOutWhenBrowserNeverReturns() throws IOException {
        try (LoopbackCallbackServer server = new LoopbackCallbackServer()) {
            server.awaitCallbackQuery(200);
        }
    }

    private static int status(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            return c.getResponseCode();
        } finally {
            c.disconnect();
        }
    }
}
