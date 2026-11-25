package com.mealspire.app.net;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * One-shot HTTP listener on 127.0.0.1 that receives the Sign in with ChatGPT
 * redirect ({@code http://127.0.0.1:PORT/callback}). Sign in with ChatGPT for
 * open-source apps allows loopback redirects only, so the app hosts this tiny
 * server while the user signs in in the browser.
 */
public final class LoopbackCallbackServer implements Closeable {

    private static final String PATH = "/callback";
    private static final String DONE_PAGE = "<!doctype html><html lang=\"pl\"><head>"
            + "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\">"
            + "<title>Mealspire</title></head><body style=\"font-family:sans-serif;"
            + "text-align:center;padding:2em\"><h2>Wróć do aplikacji Mealspire</h2>"
            + "<p>Mealspire dokończy logowanie i pokaże jego wynik.</p></body></html>";
    private static final String TIMEOUT_MESSAGE =
            "Logowanie do ChatGPT trwało zbyt długo — spróbuj ponownie.";

    private final ServerSocket serverSocket;

    public LoopbackCallbackServer() throws IOException {
        serverSocket = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"));
    }

    public String redirectUri() {
        return "http://127.0.0.1:" + serverSocket.getLocalPort() + PATH;
    }

    /**
     * Blocks until the browser hits {@code /callback} and returns its query
     * string. Other paths (e.g. favicon) get a 404 and are ignored.
     *
     * <p>The browser may come back late: Android freezes a backgrounded app,
     * so the redirect can sit in the socket backlog until the user returns.
     * A callback already waiting there is still taken after the deadline,
     * and a browser that gave up before getting the page (broken pipe) does
     * not lose the sign-in — the query already arrived.
     *
     * @throws IOException on timeout or when {@link #close()} was called
     */
    public String awaitCallbackQuery(int timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            long left = deadline - System.currentTimeMillis();
            // Past the deadline only connections already queued are taken.
            serverSocket.setSoTimeout((int) Math.max(1, Math.min(left, Integer.MAX_VALUE)));
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketTimeoutException e) {
                throw new IOException(TIMEOUT_MESSAGE, e);
            }
            try {
                String query = handle(socket);
                if (query != null) {
                    return query;
                }
            } finally {
                closeQuietly(socket);
            }
        }
    }

    /** Returns the callback's query, or null for any other (or unreadable) request. */
    private static String handle(Socket socket) {
        String target;
        try {
            socket.setSoTimeout(10000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            target = requestTarget(reader.readLine());
        } catch (IOException e) {
            return null; // A stray or broken connection must not end the wait.
        }
        boolean callback = target != null
                && (target.equals(PATH) || target.startsWith(PATH + "?"));
        try {
            respond(socket.getOutputStream(), callback ? "200 OK" : "404 Not Found",
                    callback ? DONE_PAGE : "");
        } catch (IOException ignored) {
            // The browser stopped waiting; the redirect itself still counts.
        }
        if (!callback) {
            return null;
        }
        int q = target.indexOf('?');
        return q < 0 ? "" : target.substring(q + 1);
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing left to do with this connection.
        }
    }

    private static String requestTarget(String requestLine) {
        if (requestLine == null) {
            return null;
        }
        String[] parts = requestLine.split(" ");
        return parts.length >= 2 && "GET".equals(parts[0]) ? parts[1] : null;
    }

    private static void respond(OutputStream out, String status, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}
