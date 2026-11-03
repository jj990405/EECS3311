package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileOrganizationTest {
    @TempDir Path root;

    @Test void detectsObviousLectureUploadsWithoutCallingAi() {
        assertEquals(Category.LECTURE_MATERIALS,
                FileCategorySuggester.suggest("EECS3311-14-Test Automation.pdf").orElseThrow());
        assertEquals(Category.LECTURE_MATERIALS,
                FileCategorySuggester.suggest("Lecture 4 - design.pdf").orElseThrow());
        assertEquals(Category.ASSIGNMENT_SPECIFICATIONS,
                FileCategorySuggester.suggest("Project 2 specification.pdf").orElseThrow());
        assertEquals(Category.SYLLABUS,
                FileCategorySuggester.suggest("EECS3311_F26_syllabus.pdf").orElseThrow());
        assertTrue(FileCategorySuggester.suggest("notes.pdf").isEmpty());
    }

    @Test void aiSuggestionRequiresReviewAndMovingKeepsOriginalAndText() throws Exception {
        Path data = root.resolve("data");
        AgentController agent = new AgentController(data, (system, user) -> {
            var input = Json.array(Json.parse(user));
            assertEquals(1, input.size());
            var file = Json.object(input.get(0));
            assertTrue(Json.string(file, "textSample").contains("design patterns"));
            return Json.write(Map.of("suggestions", java.util.List.of(Map.of(
                    "id", Json.string(file, "id"), "category", "LECTURE_MATERIALS",
                    "reason", "Lecture slides"))));
        });
        var semester = agent.createSemester("Fall 2026", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31));
        var course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path original = root.resolve("EECS3311-07-Structural Patterns.txt");
        Files.writeString(original, "Lecture 7: design patterns and examples.");
        var material = agent.uploadMaterial(course.id(), Category.ASSIGNMENT_SPECIFICATIONS, original);
        Path stored = agent.materialPath(material.id());
        byte[] bytes = Files.readAllBytes(stored);
        var suggestions = agent.suggestFileCategories();
        assertEquals(1, suggestions.size());
        assertEquals(Category.ASSIGNMENT_SPECIFICATIONS, agent.materials().get(0).category());
        agent.moveMaterial(material.id(), suggestions.get(0).category());
        assertArrayEquals(bytes, Files.readAllBytes(stored));
        assertTrue(agent.materialPages(material.id()).get(0).contains("design patterns"));
        AgentController reopened = new AgentController(data, (system, user) -> "{}");
        assertEquals(Category.LECTURE_MATERIALS, reopened.materials().get(0).category());
        assertEquals(stored, reopened.materialPath(material.id()));
    }

    @Test void clearRemovesAllStudyDataButLeavesConfigurationOutsideData() throws Exception {
        Path data = root.resolve("data");
        Files.writeString(root.resolve(".env"), "local test setting");
        AgentController agent = new AgentController(data, (system, user) -> "{}");
        var semester = agent.createSemester("Fall 2026", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31));
        var course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.LOW);
        Path file = root.resolve("lecture.txt");
        Files.writeString(file, "Lecture notes");
        agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, file);
        agent.addDeadline(course.id(), "Exam", "Exam", LocalDate.of(2026, 11, 10), "Unit 1");
        Files.writeString(data.resolve("interaction-history.jsonl"), "test history");
        Files.writeString(data.resolve("last-weekly-brief.txt"), "seen");
        Files.writeString(data.resolve("artifacts/summaries/test.json"), "test");
        agent.clearAllData();
        assertTrue(agent.semesters().isEmpty());
        assertTrue(agent.courses().isEmpty());
        assertTrue(agent.materials().isEmpty());
        assertTrue(agent.schedule(AgentController.Period.ALL, null, "", LocalDate.now()).isEmpty());
        assertFalse(Files.exists(data.resolve("interaction-history.jsonl")));
        assertFalse(Files.exists(data.resolve("last-weekly-brief.txt")));
        assertFalse(Files.exists(data.resolve("artifacts/summaries/test.json")));
        assertTrue(Files.exists(root.resolve(".env")));
        assertTrue(Files.exists(file));
        AgentController reopened = new AgentController(data, (system, user) -> "{}");
        assertTrue(reopened.courses().isEmpty());
        assertTrue(reopened.materials().isEmpty());
    }
}
