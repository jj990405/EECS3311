package eecs3311.studyagent;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Validated page ranges and bounded, page-labelled source batches. */
final class StudyPages {
    private StudyPages() { }
    static List<Integer> parse(String input, int total) {
        TreeSet<Integer> pages = new TreeSet<>();
        try {
            for (String part : input.split(",", -1)) {
                String[] bounds = part.trim().split("-", -1);
                if (bounds.length < 1 || bounds.length > 2) throw new NumberFormatException();
                int first = Integer.parseInt(bounds[0].trim());
                int last = bounds.length == 1 ? first : Integer.parseInt(bounds[1].trim());
                if (first < 1 || last < first || last > total) throw new NumberFormatException();
                for (int i = first; i <= last; i++) pages.add(i);
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Enter pages from 1 to " + total + ", for example 3 or 2-5,8.");
        }
        if (pages.isEmpty()) throw new IllegalArgumentException("Choose at least one page.");
        return List.copyOf(pages);
    }

    static List<String> batches(List<String> pages, int limit) {
        if (limit < 32) throw new IllegalArgumentException("Source batch limit must be at least 32 characters.");
        List<String> result = new ArrayList<>();
        StringBuilder batch = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            String page = pages.get(i);
            if (page.isBlank()) continue;
            for (int start = 0; start < page.length();) {
                String label = "\n[Page " + (i + 1) + "]\n";
                if (batch.length() + label.length() + 1 >= limit) {
                    result.add(batch.toString()); batch.setLength(0);
                }
                int end = Math.min(page.length(), start + limit - batch.length() - label.length());
                batch.append(label).append(page, start, end); start = end;
                if (batch.length() >= limit) { result.add(batch.toString()); batch.setLength(0); }
            }
        }
        if (!batch.isEmpty()) result.add(batch.toString());
        return List.copyOf(result);
    }

    static String selected(List<String> pages, List<Integer> selection) {
        if (selection == null || selection.isEmpty()) throw new IllegalArgumentException("Choose pages first.");
        StringBuilder result = new StringBuilder();
        for (int page : new TreeSet<>(selection)) {
            if (page < 1 || page > pages.size()) throw new IllegalArgumentException("Page outside the document: " + page);
            result.append("\n[Page ").append(page).append("]\n").append(pages.get(page - 1));
        }
        if (selection.stream().allMatch(p -> pages.get(p - 1).isBlank()))
            throw new IllegalArgumentException("These slides have no extractable text. Choose other pages; OCR is not available.");
        if (result.length() > 50000) throw new IllegalArgumentException("Choose fewer slides at once (selected text exceeds 50,000 characters).");
        return result.toString();
    }
}
