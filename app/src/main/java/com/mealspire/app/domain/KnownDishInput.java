package com.mealspire.app.domain;

import java.util.regex.Pattern;

/**
 * Classifies the user's "add a dish I know" input as a URL or a free-text
 * description.
 */
public final class KnownDishInput {

    private static final Pattern URL = Pattern.compile("^https?://\\S+$",
            Pattern.CASE_INSENSITIVE);

    private KnownDishInput() {
    }

    public static boolean isUrl(String input) {
        return input != null && URL.matcher(input.trim()).matches();
    }
}
