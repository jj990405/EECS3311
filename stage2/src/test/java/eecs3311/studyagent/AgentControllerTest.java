package eecs3311.studyagent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free integration checks for persistence, ordering, validation, and progress. */
public final class AgentControllerTest {
    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("eecs3311-agent-test-");
        try {
            AgentController agent = new AgentController(temporary.resolve("data"));
            AtomicInteger changes = new AtomicInteger();
            agent.addChangeListener(changes::incrementAndGet);

            Semester semester = agent.createSemester("Fall 2026", LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31));
            Course easier = agent.createCourse(semester.id(), "HIST1000", "#3355AA", Difficulty.LOW);
            Course harder = agent.createCourse(semester.id(), "EECS3311", "#AA5533", Difficulty.HIGH);
            Deadline late = agent.addDeadline(easier.id(), "Essay", "Assignment",
                    LocalDate.of(2026, 11, 20), "Week 8");
            Deadline sameDayEasy = agent.addDeadline(easier.id(), "Quiz", "Quiz",
                    LocalDate.of(2026, 11, 10), "Week 7");
            Deadline sameDayHard = agent.addDeadline(harder.id(), "Project 2", "Project",
                    LocalDate.of(2026, 11, 10), "Implementation");
            Deadline undated = agent.addDeadline(harder.id(), "Final details", "Exam", null, "TBD");

            List<Deadline> ordered = agent.schedule(AgentController.Period.ALL, null, "",
                    LocalDate.of(2026, 11, 1));
            check(ordered.get(0).id().equals(sameDayHard.id()), "Harder course should win same-day tie");
            check(ordered.get(1).id().equals(sameDayEasy.id()), "Same-day easier course follows");
            check(ordered.get(2).id().equals(late.id()), "Later date follows");
            check(ordered.get(3).id().equals(undated.id()), "TBD should be listed last");
            check(agent.schedule(AgentController.Period.THIS_MONTH, harder.id(), "project",
                    LocalDate.of(2026, 11, 1)).size() == 1, "Month, course, and search filter");

            expectInvalid(() -> agent.addDeadline(harder.id(), "Invalid", "Assignment",
                    LocalDate.of(2027, 1, 1), ""), "Out-of-semester deadline must fail");
            expectInvalid(() -> agent.createCourse(semester.id(), "EECS3311", "#123456", Difficulty.MEDIUM),
                    "Duplicate course must fail");

            Path source = temporary.resolve("notes.txt");
            Files.writeString(source, "Study notes");
            MaterialFile material = agent.uploadMaterial(harder.id(), Category.LECTURE_MATERIALS, source);
            check(Files.readString(agent.materialPath(material.id())).equals("Study notes"), "Uploaded file bytes");
            expectInvalid(() -> agent.uploadMaterial(harder.id(), Category.LECTURE_MATERIALS, source),
                    "Duplicate material must fail");
            check(agent.workspace().get(0).children().size() == 2, "Composite workspace contains both courses");

            agent.setCompleted(sameDayHard.id(), true);
            AgentController.Progress progress = agent.progress(harder.id(), LocalDate.of(2026, 11, 11));
            check(progress.completed() == 1 && progress.total() == 2 && progress.overdue() == 0,
                    "Progress counts completed and undated items correctly");
            check(changes.get() == 9, "Observer should run once for each successful change");

            AgentController reloaded = new AgentController(temporary.resolve("data"));
            check(reloaded.semesters().size() == 1 && reloaded.courses().size() == 2,
                    "Semester and course survive restart");
            check(reloaded.schedule(AgentController.Period.ALL, null, "", LocalDate.now()).size() == 4,
                    "Deadlines survive restart");
            check(reloaded.deadline(sameDayHard.id()).completed(), "Completion survives restart");
            check(reloaded.materials().size() == 1, "Material metadata survives restart");

            reloaded.editDeadline(late.id(), "Revised essay", "Assignment", LocalDate.of(2026, 11, 21), "New scope");
            check(reloaded.deadline(late.id()).dueDate().equals(LocalDate.of(2026, 11, 21)), "Edit persists");
            reloaded.deleteDeadline(undated.id());
            check(reloaded.schedule(AgentController.Period.ALL, null, "", LocalDate.now()).size() == 3,
                    "Delete removes deadline");
            System.out.println("PASS: persistence, priority, filtering, validation, materials, progress, edits, observer");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(ThrowingAction action, String message) throws Exception {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
}
