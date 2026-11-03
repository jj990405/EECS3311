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
            check(agent.calendar(java.time.YearMonth.of(2026, 11)).contains("Project 2"), "Monthly calendar entry");
            check(agent.reminders(LocalDate.of(2026, 11, 8), 2).size() == 2, "Due-soon reminders");

            expectInvalid(() -> agent.addDeadline(harder.id(), "Invalid", "Assignment",
                    LocalDate.of(2027, 1, 1), ""), "Out-of-semester deadline must fail");
            expectInvalid(() -> agent.createCourse(semester.id(), "EECS3311", "#123456", Difficulty.MEDIUM),
                    "Duplicate course must fail");

            Path source = temporary.resolve("notes.txt");
            Files.writeString(source, "Study notes");
            MaterialFile material = agent.uploadMaterial(harder.id(), Category.LECTURE_MATERIALS, source);
            check(Files.readString(agent.materialPath(material.id())).equals("Study notes"), "Uploaded file bytes");
            check(agent.materialPages(material.id()).equals(List.of("Study notes")), "TXT extraction persists");
            Path projectRoot = Path.of(System.getProperty("project2.root", "."));
            Path stage2Pdf = projectRoot.resolve("Project-Stage2-Instruction.pdf");
            if (Files.exists(stage2Pdf)) {
                List<String> pdfPages = new PdfReader().extractText(stage2Pdf);
                check(pdfPages.size() == 5, "Stage 2 instructions should have five PDF pages");
                check(pdfPages.stream().anyMatch(p -> p.contains("October 20") || p.contains("Oct 20")),
                        "PDF extraction should find the commit period");
            }
            ResponseParser parser = new ResponseParser();
            check(parser.week("{\"week\":3,\"reason\":\"matches lecture\"}") == 3,
                    "Structured week response");
            check(parser.deadlines("{\"deadlines\":[{\"title\":\"Test\",\"type\":\"Exam\","
                    + "\"dueDate\":\"2026-11-01\",\"scope\":\"Ch 1\",\"evidence\":\"Nov 1\"}]}")
                    .get(0).dueDate().equals(LocalDate.of(2026, 11, 1)), "Structured deadline response");
            expectInvalid(() -> agent.uploadMaterial(harder.id(), Category.LECTURE_MATERIALS, source),
                    "Duplicate material must fail");
            check(agent.workspace().get(0).children().size() == 2, "Composite workspace contains both courses");

            AgentController aiAgent = new AgentController(temporary.resolve("data"), (system, user) ->
                    system.contains("DEADLINE_EXTRACTION")
                            ? "{\"deadlines\":[{\"title\":\"AI suggested exam\",\"type\":\"Exam\","
                            + "\"dueDate\":\"2026-11-15\",\"scope\":\"Ch 1\",\"evidence\":\"Nov 15 exam\"}]}"
                            : "{\"week\":4,\"reason\":\"lecture heading\"}");
            Path syllabusPath = temporary.resolve("syllabus.txt");
            Files.writeString(syllabusPath, "Exam on Nov 15, 2026");
            MaterialFile syllabus = aiAgent.uploadMaterial(harder.id(), Category.SYLLABUS, syllabusPath);
            var proposedDeadline = aiAgent.suggestSyllabusDeadlines(syllabus.id()).get(0);
            check(aiAgent.schedule(AgentController.Period.ALL, null, "AI suggested", LocalDate.now()).isEmpty(),
                    "Model output must not mutate schedule before approval");
            aiAgent.approveProposal(proposedDeadline.id());
            check(aiAgent.schedule(AgentController.Period.ALL, null, "AI suggested", LocalDate.now()).size() == 1,
                    "Approved deadline is stored");
            var proposedWeek = aiAgent.suggestLectureWeek(material.id());
            aiAgent.rejectProposal(proposedWeek.id());
            check(aiAgent.materials().stream().filter(m -> m.id().equals(material.id())).findFirst().orElseThrow()
                    .lectureWeek() == null, "Rejected week has no effect");
            aiAgent.startStudy(harder.id(), "Design patterns");
            aiAgent.pauseStudy();
            aiAgent.resumeStudy();
            aiAgent.stopStudy();
            check(aiAgent.studySessions(harder.id()).size() == 1, "Timer records a study session");

            agent.setCompleted(sameDayHard.id(), true);
            AgentController.Progress progress = agent.progress(harder.id(), LocalDate.of(2026, 11, 11));
            check(progress.completed() == 1 && progress.total() == 2 && progress.overdue() == 0,
                    "Progress counts completed and undated items correctly");
            check(changes.get() == 9, "Observer should run once for each successful change");

            AgentController reloaded = new AgentController(temporary.resolve("data"));
            check(reloaded.semesters().size() == 1 && reloaded.courses().size() == 2,
                    "Semester and course survive restart");
            check(reloaded.schedule(AgentController.Period.ALL, null, "", LocalDate.now()).size() == 5,
                    "Deadlines survive restart");
            check(reloaded.deadline(sameDayHard.id()).completed(), "Completion survives restart");
            check(reloaded.materials().size() == 2, "Material metadata survives restart");
            check(reloaded.studySessions(harder.id()).size() == 1, "Study sessions survive restart");

            reloaded.editDeadline(late.id(), "Revised essay", "Assignment", LocalDate.of(2026, 11, 21), "New scope");
            check(reloaded.deadline(late.id()).dueDate().equals(LocalDate.of(2026, 11, 21)), "Edit persists");
            reloaded.deleteDeadline(undated.id());
            check(reloaded.schedule(AgentController.Period.ALL, null, "", LocalDate.now()).size() == 4,
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
