package eecs3311.studyagent;

import java.util.Locale;

/** Conservative title comparison for AI calendar imports with spelling mistakes. */
final class DeadlineTitleMatcher {
    private DeadlineTitleMatcher() { }

    static boolean same(String left, String right) {
        String[] first = tokens(left);
        String[] second = tokens(right);
        if (first.length != second.length) return false;
        for (int index = 0; index < first.length; index++) {
            String a = first[index];
            String b = second[index];
            if (a.equals(b)) continue;
            // Short words and all numbers must match exactly. This keeps
            // "Quiz 1" separate from "Quiz 2" and "Part A" from "Part B".
            if (a.length() < 8 || b.length() < 8 || a.matches("\\d+") || b.matches("\\d+"))
                return false;
            if (distance(a, b) > Math.min(3, Math.max(1, Math.min(a.length(), b.length()) / 3)))
                return false;
        }
        return true;
    }

    private static String[] tokens(String title) {
        String normalized = title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
        return normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
    }

    private static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++)
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1),
                        previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
