package com.mealspire.app.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a single model answer that contains several {@link DishProposal}s into
 * a list. Proposals are recognised either by their "Nazwa:" label or, as a
 * fallback, by blank-line / "---" separators. Duplicates (by name) are dropped.
 */
public final class ProposalListParser {

    private final ProposalParser single = new ProposalParser();

    public List<DishProposal> parse(String text) {
        List<DishProposal> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);

        List<Integer> nameStarts = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (isNameLine(lines[i])) {
                nameStarts.add(i);
            }
        }

        if (!nameStarts.isEmpty()) {
            for (int s = 0; s < nameStarts.size(); s++) {
                int from = nameStarts.get(s);
                int to = (s + 1 < nameStarts.size()) ? nameStarts.get(s + 1) : lines.length;
                addParsed(result, join(lines, from, to));
            }
        } else {
            // No "Nazwa:" labels: split into blocks on blank / dashed separators.
            List<String> block = new ArrayList<>();
            for (String raw : lines) {
                if (isSeparator(raw)) {
                    addParsed(result, joinList(block));
                    block.clear();
                } else {
                    block.add(raw);
                }
            }
            addParsed(result, joinList(block));
        }

        return dedupeByName(result);
    }

    private void addParsed(List<DishProposal> out, String chunk) {
        if (chunk.trim().isEmpty()) {
            return;
        }
        DishProposal proposal = single.parse(chunk);
        if (!proposal.isEmpty()) {
            out.add(proposal);
        }
    }

    private static boolean isNameLine(String line) {
        String cleaned = line.trim().toLowerCase();
        while (cleaned.startsWith("-") || cleaned.startsWith("*") || cleaned.startsWith("•")
                || cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1).trim();
        }
        cleaned = cleaned.replace("**", "");
        return cleaned.startsWith("nazwa:");
    }

    private static boolean isSeparator(String line) {
        String t = line.trim();
        if (t.isEmpty()) {
            return true;
        }
        // A line made only of dashes / em dashes acts as a separator.
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c != '-' && c != '—' && c != '–') {
                return false;
            }
        }
        return t.length() >= 2;
    }

    private static String join(String[] lines, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(lines[i]);
            if (i < to - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String joinList(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static List<DishProposal> dedupeByName(List<DishProposal> proposals) {
        Map<String, DishProposal> byName = new LinkedHashMap<>();
        for (DishProposal proposal : proposals) {
            String key = proposal.getName().toLowerCase();
            if (!byName.containsKey(key)) {
                byName.put(key, proposal);
            }
        }
        return new ArrayList<>(byName.values());
    }
}
