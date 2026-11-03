package eecs3311.studyagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Retrieves the most relevant uploaded pages before sending bounded context to the LLM. */
public final class MaterialSearcher {
    public record Passage(String fileName, int page, String text, int score) { }

    public List<Passage> search(DataStore store, List<MaterialFile> files, String query, int limit)
            throws IOException {
        if (limit < 1) throw new IllegalArgumentException("Passage limit must be positive");
        String[] terms = query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+", -1);
        List<Passage> passages = new ArrayList<>();
        for (MaterialFile file : files) {
            List<String> pages = store.materialPages(file.id());
            for (int i = 0; i < pages.size(); i++) {
                String text = pages.get(i);
                if (text.isBlank()) continue;
                String lower = text.toLowerCase(Locale.ROOT);
                int score = 0;
                for (String term : terms) if (term.length() > 2 && lower.contains(term)) score++;
                passages.add(new Passage(file.originalName(), i + 1,
                        text.substring(0, Math.min(text.length(), 4500)), score));
            }
        }
        return passages.stream().sorted(Comparator.comparingInt(Passage::score).reversed()
                .thenComparing(Passage::fileName).thenComparingInt(Passage::page)).limit(limit).toList();
    }
}
