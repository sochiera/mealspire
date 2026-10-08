package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dzieli luźny tekst przepisu („Składniki: a, b.\n\nKroki…") na bloki do
 * czytelnego ekranu: nagłówki, pozycje listy (składniki, punktory), kroki
 * numerowane i akapity. Treść nie ginie — zmienia się tylko jej podział.
 */
public final class RecipeLayout {

    public enum Type { HEADING, ITEM, STEP, PARAGRAPH }

    /** Jeden blok ekranu przepisu; {@code number} tylko dla kroków. */
    public static final class Block {
        private final Type type;
        private final String text;
        private final String number;

        Block(Type type, String text, String number) {
            this.type = type;
            this.text = text;
            this.number = number;
        }

        public Type getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getNumber() {
            return number;
        }
    }

    // Etykieta to krótki początek linii zakończony dwukropkiem, po którym jest
    // spacja albo koniec linii — „12:30" ani długie zdanie nią nie są.
    private static final int MAX_LABEL_LENGTH = 40;
    private static final Pattern STEP = Pattern.compile("^(\\d{1,2})[.)]\\s+(.+)$");
    private static final Pattern BULLET = Pattern.compile("^[-•*–]\\s+(.+)$");

    private RecipeLayout() {
    }

    public static List<Block> parse(String details) {
        if (details == null || details.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Block> blocks = new ArrayList<>();
        for (String raw : details.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher step = STEP.matcher(line);
            if (step.matches()) {
                blocks.add(new Block(Type.STEP, step.group(2).trim(), step.group(1)));
                continue;
            }
            Matcher bullet = BULLET.matcher(line);
            if (bullet.matches()) {
                blocks.add(new Block(Type.ITEM, bullet.group(1).trim(), null));
                continue;
            }
            int colon = labelEnd(line);
            if (colon < 0) {
                blocks.add(new Block(Type.PARAGRAPH, line, null));
                continue;
            }
            String label = line.substring(0, colon).trim();
            String rest = line.substring(colon + 1).trim();
            blocks.add(new Block(Type.HEADING, label, null));
            if (rest.isEmpty()) {
                continue;
            }
            if (label.toLowerCase().startsWith("składniki")) {
                for (String item : splitItems(rest)) {
                    blocks.add(new Block(Type.ITEM, item, null));
                }
            } else {
                blocks.add(new Block(Type.PARAGRAPH, rest, null));
            }
        }
        return blocks;
    }

    /** Index of the label's colon, or -1 when the line is not "Label: …". */
    private static int labelEnd(String line) {
        int colon = line.indexOf(':');
        if (colon <= 0 || colon > MAX_LABEL_LENGTH) {
            return -1;
        }
        if (colon + 1 < line.length() && !Character.isWhitespace(line.charAt(colon + 1))) {
            return -1;
        }
        if (Character.isDigit(line.charAt(colon - 1))) {
            return -1;
        }
        return colon;
    }

    /** Splits ingredient separators, preserving decimal commas and parenthesized text. */
    private static List<String> splitItems(String text) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && depth > 0) {
                depth--;
            }
            boolean decimalComma = c == ',' && i > 0 && i + 1 < text.length()
                    && Character.isDigit(text.charAt(i - 1))
                    && Character.isDigit(text.charAt(i + 1));
            if (c == ',' && depth == 0 && !decimalComma) {
                addItem(items, current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addItem(items, current.toString());
        return items;
    }

    private static void addItem(List<String> items, String raw) {
        String item = raw.trim();
        while (item.endsWith(".")) {
            item = item.substring(0, item.length() - 1).trim();
        }
        if (!item.isEmpty()) {
            items.add(item);
        }
    }
}
