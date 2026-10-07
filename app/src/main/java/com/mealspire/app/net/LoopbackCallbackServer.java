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
            + "text-align:center;padding:2em\"><h2>Gotowe</h2>"
            + "<p>Możesz wrócić do aplikacji Mealspire.</p></body></html>";

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
     * @throws IOException on timeout or when {@link #close()} was called
     */
    public String awaitCallbackQuery(int timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) {
                throw new IOException("Logowanie do ChatGPT trwało zbyt długo — spróbuj ponownie.");
            }
            serverSocket.setSoTimeout((int) left);
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(10000);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String requestLine = reader.readLine();
                String target = requestTarget(requestLine);
                OutputStream out = socket.getOutputStream();
                if (target == null || !(target.equals(PATH) || target.startsWith(PATH + "?"))) {
                    respond(out, "404 Not Found", "");
                    continue;
                }
                respond(out, "200 OK", DONE_PAGE);
                int q = target.indexOf('?');
                return q < 0 ? "" : target.substring(q + 1);
            } catch (SocketTimeoutException e) {
                throw new IOException("Logowanie do ChatGPT trwało zbyt długo — spróbuj ponownie.", e);
            }
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
