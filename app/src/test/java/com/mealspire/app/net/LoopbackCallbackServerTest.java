package com.mealspire.app.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void expiredWaitRejectsEvenAQueuedCallbackAndClosesThePort() throws Exception {
        try (LoopbackCallbackServer server = new LoopbackCallbackServer();
             Socket browser = connect(server)) {
            send(browser, "/callback?code=late&state=s");

            assertThrows(IOException.class, () -> server.awaitCallbackQuery(0));
            assertThrows(ConnectException.class, () -> connect(server));
        }
    }

    @Test
    public void incompleteRequestCannotExtendTheDeadlineOrKeepThePortOpen() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (LoopbackCallbackServer server = new LoopbackCallbackServer();
             Socket browser = connect(server)) {
            browser.getOutputStream().write("GET /callback?".getBytes(StandardCharsets.US_ASCII));
            browser.getOutputStream().flush();

            Future<String> callback = worker.submit(() -> server.awaitCallbackQuery(250));
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> callback.get(3, TimeUnit.SECONDS));
            assertTrue(failure.getCause().getMessage().contains("trwało zbyt długo"));
            assertThrows(ConnectException.class, () -> connect(server));
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    public void browserThatGaveUpBeforeThePageDoesNotLoseTheSignIn() throws Exception {
        try (LoopbackCallbackServer server = new LoopbackCallbackServer()) {
            Socket browser = connect(server);
            send(browser, "/callback?code=abc&state=s");
            browser.close(); // the browser showed its own timeout error and closed the tab

            assertEquals("code=abc&state=s", server.awaitCallbackQuery(10000));
        }
    }

    @Test
    public void brokenStrayConnectionDoesNotEndTheWait() throws Exception {
        try (LoopbackCallbackServer server = new LoopbackCallbackServer()) {
            connect(server).close(); // connected, sent nothing
            Socket browser = connect(server);
            send(browser, "/callback?code=abc&state=s");

            assertEquals("code=abc&state=s", server.awaitCallbackQuery(10000));
            browser.close();
        }
    }

    @Test
    public void callbackPageDoesNotClaimSuccessBeforeTheAppFinishes() throws Exception {
        try (LoopbackCallbackServer server = new LoopbackCallbackServer();
             Socket browser = connect(server)) {
            send(browser, "/callback?code=abc&state=s");
            server.awaitCallbackQuery(10000);

            String page = new String(readAll(browser.getInputStream()), StandardCharsets.UTF_8);
            assertTrue(page, page.startsWith("HTTP/1.1 200 OK"));
            assertTrue(page, page.contains("Wróć do aplikacji Mealspire"));
            assertFalse(page, page.contains("Gotowe"));
        }
    }

    @Test(expected = IOException.class)
    public void closingTheServerCancelsTheWait() throws Exception {
        LoopbackCallbackServer server = new LoopbackCallbackServer();
        new Thread(() -> {
            try {
                Thread.sleep(100);
                server.close();
            } catch (Exception ignored) {
                // test helper
            }
        }).start();
        server.awaitCallbackQuery(10000);
    }

    private static Socket connect(LoopbackCallbackServer server) throws IOException {
        URL url = new URL(server.redirectUri());
        return new Socket(url.getHost(), url.getPort());
    }

    private static void send(Socket socket, String target) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(("GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
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
