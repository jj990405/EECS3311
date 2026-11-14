package eecs3311.studyagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Retrieves the most relevant uploaded pages before sending bounded context to the LLM. */
public final class MaterialSearcher {
    public record Passage(String fileName, int page, String text, int score) { }
    @FunctionalInterface interface IndexReader { SourceIndex read(MaterialFile file) throws IOException; }

    public List<Passage> search(DataStore store, List<MaterialFile> files, String query, int limit)
            throws IOException {
        return search(files,query,limit,file -> SourceIndex.build(store.materialPages(file.id())));
    }
    List<Passage> search(List<MaterialFile> files,String query,int limit,IndexReader reader) throws IOException {
        if (limit < 1) throw new IllegalArgumentException("Passage limit must be positive");
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("lab\\s*test", "lab test")
                .replaceAll("final", "final exam");
        var stop = java.util.Set.of("how","what","which","does","for","the","and","this","that","from",
                "with","about","can","you","please","study","explain","should","would","could");
        String[] terms = SourceIndex.tokens(normalized).stream()
                .filter(t -> t.length() > 2 && !stop.contains(t)).distinct().toArray(String[]::new);
        List<Passage> passages = new ArrayList<>();
        for (MaterialFile file : files) {
            SourceIndex index=reader.read(file);
            List<String> pages = index.pages();
            int[] scores=new int[pages.size()];
            for(String term:terms) for(int page:index.terms().getOrDefault(term,List.of())) scores[page-1]+=3;
            for (int i = 0; i < pages.size(); i++) {
                String text = pages.get(i);
                if (text.isBlank()) continue;
                int score = scores[i];
                for (String term : terms) if (file.originalName().toLowerCase(Locale.ROOT).contains(term)) score++;
                if (assessmentQuestion(query) && file.category() == Category.SYLLABUS) score += 2;
                passages.add(new Passage(file.originalName(), i + 1,
                        text, score));
            }
        }
        List<Passage> ranked = passages.stream().sorted(Comparator.comparingInt(Passage::score).reversed()
                .thenComparing(Passage::fileName).thenComparingInt(Passage::page)).toList();
        List<Passage> diverse = new ArrayList<>();
        var perFile = new java.util.HashMap<String,Integer>();
        for (Passage page : ranked) {
            if (perFile.getOrDefault(page.fileName(),0) >= 4) continue;
            diverse.add(page); perFile.merge(page.fileName(),1,Integer::sum);
            if (diverse.size() == limit) break;
        }
        for (Passage page : ranked) {
            if (diverse.size() == limit) break;
            if (!diverse.contains(page)) diverse.add(page);
        }
        return List.copyOf(diverse);
    }
    static boolean assessmentQuestion(String query) {
        return query.toLowerCase(Locale.ROOT).matches("(?s).*(lab\\s*test|quiz|midterm|final|exam|assessment).*");
    }
}
