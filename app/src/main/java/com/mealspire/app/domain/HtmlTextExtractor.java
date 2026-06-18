package com.mealspire.app.domain;

import java.util.regex.Pattern;

/**
 * Very small HTML → plain-text reducer: drops script/style blocks and tags,
 * decodes a handful of common entities, and collapses whitespace. Good enough to
 * feed a recipe page's text to the model without pulling in an HTML parser.
 */
public final class HtmlTextExtractor {

    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile(
            "<(script|style)[^>]*>.*?</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private HtmlTextExtractor() {
    }

    public static String toPlainText(String html) {
        if (html == null) {
            return "";
        }
        String text = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = decodeEntities(text);
        text = WHITESPACE.matcher(text).replaceAll(" ");
        return text.trim();
    }

    private static String decodeEntities(String text) {
        return text
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
