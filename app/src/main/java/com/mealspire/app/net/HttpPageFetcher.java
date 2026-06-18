package com.mealspire.app.net;

import com.mealspire.app.domain.HtmlTextExtractor;
import com.mealspire.app.domain.PageFetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Downloads a web page over HttpURLConnection and reduces it to plain text. Call
 * from a background thread. The output is capped so a huge page doesn't blow up
 * the prompt token count.
 */
public final class HttpPageFetcher implements PageFetcher {

    private static final int TIMEOUT_MS = 20000;
    private static final int MAX_CHARS = 6000;

    @Override
    public String fetchText(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mealspire/1.0");

            int status = connection.getResponseCode();
            if (status >= 400) {
                throw new IOException("Nie udało się pobrać strony (HTTP " + status + ").");
            }
            String html = readBody(connection.getInputStream());
            String text = HtmlTextExtractor.toPlainText(html);
            return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
