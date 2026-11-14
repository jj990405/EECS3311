package eecs3311.studyagent;

import java.util.*;

/** Persistent per-file inverted index over every stored source page. */
record SourceIndex(List<String> pages, Map<String,List<Integer>> terms) {
    SourceIndex { pages=List.copyOf(pages); terms=Map.copyOf(terms); }
    static List<String> tokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("lab\\s*test","lab test")
                .replaceAll("(?<=\\p{L})(?=\\d)"," ").split("[^\\p{L}\\p{N}]+"))
                .filter(s->!s.isBlank()&&s.length()<80).distinct().toList();
    }
    static SourceIndex build(List<String> pages) {
        Map<String,List<Integer>> words=new TreeMap<>();
        for(int i=0;i<pages.size();i++) for(String term:tokens(pages.get(i)))
            words.computeIfAbsent(term,key->new ArrayList<>()).add(i+1);
        return new SourceIndex(pages,words);
    }
    static SourceIndex decode(Map<String,Object> record) {
        List<String> pages=Json.array(record.get("pages")).stream().map(Object::toString).toList();
        if(!record.containsKey("terms")) return build(pages);
        Map<String,List<Integer>> terms=new HashMap<>();
        Json.object(record.get("terms")).forEach((word,raw)->terms.put(word,
                Json.array(raw).stream().map(n->((Number)n).intValue()).toList()));
        return new SourceIndex(pages,terms);
    }
    Map<String,Object> encode() { return Map.of("version",1,"pages",pages,"terms",terms); }
}
