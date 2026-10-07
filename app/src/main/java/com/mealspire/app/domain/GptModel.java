package com.mealspire.app.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model families the user can pick for AI on their ChatGPT plan. Luna (fast,
 * cheap on the plan's limits) is the default; Sol is the stronger option.
 *
 * <p>The real slug always comes from the signed-in account's model catalog
 * ({@code GET /v1/models}) — the IDs below are only the documented OpenAI
 * model IDs used to recognise the family, in order of preference. A family
 * missing from the catalog stays unavailable; no other model is substituted.
 */
public enum GptModel {
    // https://developers.openai.com/api/docs/models/gpt-6-luna
    LUNA("GPT Luna", "luna", "gpt-6-luna"),
    // https://developers.openai.com/api/docs/models/gpt-6.1-sol (+ starsze gpt-6-sol)
    SOL("GPT Sol", "sol", "gpt-6.1-sol", "gpt-6-sol");

    public static final GptModel DEFAULT = LUNA;

    public final String label;
    private final Pattern familyPattern;
    private final String[] knownIds;

    GptModel(String label, String family, String... knownIds) {
        this.label = label;
        this.familyPattern = Pattern.compile("gpt-(\\d+(?:\\.\\d+)?)-" + family);
        this.knownIds = knownIds;
    }

    /** Preferred documented ID present among {@code listed}, else the newest {@code gpt-N[.M]-family}. */
    String resolve(java.util.List<String> listed) {
        for (String id : knownIds) {
            if (listed.contains(id)) {
                return id;
            }
        }
        String best = "";
        double bestVersion = -1;
        for (String slug : listed) {
            Matcher m = familyPattern.matcher(slug);
            if (m.matches()) {
                double version = Double.parseDouble(m.group(1));
                if (version > bestVersion) {
                    bestVersion = version;
                    best = slug;
                }
            }
        }
        return best;
    }

    /** Parses a stored name; unknown or empty means the default (Luna). */
    public static GptModel fromName(String name) {
        for (GptModel model : values()) {
            if (model.name().equals(name)) {
                return model;
            }
        }
        return DEFAULT;
    }
}
