package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DocumentClassificationTest {
    @Test void uncertainFileReadsEverySegmentBeforeFinalDecision() throws Exception {
        List<String> pages = List.of("FIRST ".repeat(1900), "MIDDLE ".repeat(1900), "LAST ".repeat(1900));
        var source = DocumentClassification.source(UUID.randomUUID(), "unknown.pdf", pages);
        StringBuilder received = new StringBuilder(); AtomicInteger segments = new AtomicInteger();
        LlmClient llm = (system, user) -> {
            if (system.startsWith("Classify each")) return initial(source.id(), "OTHER", "MEDIUM");
            if (system.startsWith("Inspect this entire")) {
                var input = Json.object(Json.parse(user));
                received.append(Json.string(input, "text")); segments.incrementAndGet();
                return "{\"purpose\":\"assignment instructions\",\"evidence\":\"work to submit\",\"category\":\"ASSIGNMENT_SPECIFICATIONS\"}";
            }
            assertTrue(user.contains("allSegments"));
            return "{\"category\":\"ASSIGNMENT_SPECIFICATIONS\",\"confidence\":\"HIGH\",\"reason\":\"Whole document defines submitted work\"}";
        };
        var decisions = DocumentClassification.classify(List.of(source), llm);
        String expected = "\n[Page 1]\n" + pages.get(0) + "\n[Page 2]\n" + pages.get(1) + "\n[Page 3]\n" + pages.get(2);
        assertEquals(expected, received.toString(), "Full review must not truncate middle or last pages");
        assertTrue(segments.get() > 1); assertEquals(Category.ASSIGNMENT_SPECIFICATIONS, decisions.get(0).category());
        assertTrue(decisions.get(0).reason().contains("3 pages"));
    }
    @Test void missingAndDuplicateResultsAreFailuresInsteadOfSilentPartialSorting() {
        var a = DocumentClassification.source(UUID.randomUUID(), "lecture.txt", List.of("Lecture 1"));
        var b = DocumentClassification.source(UUID.randomUUID(), "lecture2.txt", List.of("Lecture 2"));
        assertThrows(IOException.class, () -> DocumentClassification.classify(List.of(a, b), (s,u) -> initial(a.id(), "LECTURE_MATERIALS", "HIGH")));
        assertThrows(IOException.class, () -> DocumentClassification.classify(List.of(a), (s,u) ->
                "{\"suggestions\":[" + row(a.id(), "LECTURE_MATERIALS", "HIGH") + "," + row(a.id(), "LECTURE_MATERIALS", "HIGH") + "]}"));
    }
    @Test void sampleSpansDocumentAndUnknownFilesDoNotInheritSyllabusFolder() {
        var source = DocumentClassification.source(UUID.randomUUID(), "notes.pdf", List.of("FIRST", "SECOND", "THIRD", "MIDDLE", "FIFTH", "LAST"));
        assertTrue(source.sample().contains("FIRST")); assertTrue(source.sample().contains("MIDDLE")); assertTrue(source.sample().contains("LAST"));
        assertEquals(Category.OTHER, source.hint().category()); assertFalse(source.hint().clear());
        assertEquals(Category.ASSIGNMENT_SPECIFICATIONS, DocumentClassification.hint("document.pdf", List.of("EECS3311 Fall 2026 Course Project Stage 1 Instruction")).category());
        assertEquals(Category.SYLLABUS, DocumentClassification.hint("document.pdf", List.of("EECS3311 Course Syllabus Fall 2026")).category());
    }
    @Test void unreadableFilesRemainForReviewInsteadOfInventingContent() throws Exception {
        var source = DocumentClassification.source(UUID.randomUUID(), "scan.pdf", List.of("", " "));
        AtomicInteger calls = new AtomicInteger();
        var decisions = DocumentClassification.classify(List.of(source), (s,u) -> {
            calls.incrementAndGet(); return initial(source.id(), "OTHER", "LOW");
        });
        assertEquals(1, calls.get()); assertEquals(Category.OTHER, decisions.get(0).category());
        assertTrue(decisions.get(0).reason().contains("no readable text"));
    }
    private static String initial(UUID id, String category, String confidence) { return "{\"suggestions\":[" + row(id, category, confidence) + "]}"; }
    private static String row(UUID id, String category, String confidence) {
        return Json.write(Map.of("id", id.toString(), "category", category, "reason", "document evidence", "confidence", confidence, "needsFullReview", false));
    }
}
