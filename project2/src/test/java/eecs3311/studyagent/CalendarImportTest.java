package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CalendarImportTest {
    @TempDir Path root;

    @Test void analyzesUploadedDeadlineSourcesOnlyAfterRequestAndAppliesOnlyApprovedItems() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AgentController agent = new AgentController(root.resolve("data"), (system, user) -> {
            calls.incrementAndGet();
            boolean assignment = user.contains("source assignment.txt");
            String title = assignment ? "Project 2" : "Quiz 1";
            String date = assignment ? "2026-11-10" : "2026-10-03";
            return "{\"deadlines\":[{\"title\":\"" + title + "\",\"type\":\"Assignment\","
                    + "\"dueDate\":\"" + date + "\",\"weight\":10,\"scope\":\"Course\","
                    + "\"evidence\":\"" + title + " due " + date + "\"}]}";
        });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path syllabus = root.resolve("syllabus.txt");
        Path assignment = root.resolve("assignment.txt");
        Path lecture = root.resolve("lecture.txt");
        Files.writeString(syllabus, "Quiz 1 due 2026-10-03");
        Files.writeString(assignment, "Project 2 due 2026-11-10");
        Files.writeString(lecture, "Design patterns overview");
        agent.uploadMaterial(course.id(), Category.SYLLABUS, syllabus);
        agent.uploadMaterial(course.id(), Category.ASSIGNMENT_SPECIFICATIONS, assignment);
        agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, lecture);
        assertEquals(0, calls.get(), "Upload must not spend API calls");
        var suggestions = agent.suggestCalendarFromFiles();
        assertEquals(2, suggestions.filesAnalyzed());
        assertEquals(2, calls.get());
        assertEquals(2, suggestions.proposals().size());
        assertTrue(agent.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).isEmpty(),
                "AI suggestions must not silently change the calendar");
        agent.approveProposal(suggestions.proposals().get(0).id());
        assertEquals(1, agent.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).size());
    }
}
