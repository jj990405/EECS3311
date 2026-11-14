package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliProposalReviewTest {
    @TempDir Path root;

    @Test void numberedReviewMatchesApprovalAndOnlyTheChosenChangeIsApplied() throws Exception {
        var targetId = new AtomicReference<UUID>();
        var agent = new AgentController(root.resolve("data"), (system, user) -> {
            if (user.contains("move test CLI"))
                return "{\"action\":\"edit\",\"deadlineId\":\"" + targetId.get()
                        + "\",\"dueDate\":\"2026-11-24\"}";
            return "{\"action\":\"add\",\"title\":\"Other pending suggestion\","
                    + "\"type\":\"Assignment\",\"dueDate\":\"2026-11-25\",\"scope\":\"Unapproved\"}";
        });
        var semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        var course = agent.createCourse(semester.id(), "EECS3311", "#7799AA", Difficulty.HIGH);
        var target = agent.addDeadline(course.id(), "test CLI", "Quiz", LocalDate.of(2026, 11, 22), "Contract practice");
        targetId.set(target.id());
        for (int i = 0; i < 11; i++) agent.planScheduleChange(course.id(), "Add another suggestion " + i);

        String preview = runCli(agent, "ai schedule\nmove test CLI to November 24. keep its title and type.\nproposals\nexit\n");
        assertTrue(preview.contains("Proposed change (not saved):"), preview);
        assertTrue(preview.contains("The calendar is unchanged."), preview);
        assertTrue(preview.contains("11. Before: none\n    After:"), preview);
        String numberedTarget = "12. Before: test CLI / 2026-11-22 / Contract practice\n"
                + "    After: test CLI / 2026-11-24 / Contract practice";
        assertTrue(preview.contains(numberedTarget), preview);
        assertEquals(LocalDate.of(2026, 11, 22), agent.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).get(0).dueDate());

        String approved = runCli(agent, "approve\n12\ncalendar\n2026-11\nexit\n");
        assertTrue(approved.contains(numberedTarget), approved);
        assertTrue(approved.contains("Approved."), approved);
        assertTrue(approved.contains("2026-11-24  test CLI"), approved);
        assertFalse(approved.contains("Error:"), approved);
        assertEquals(11, agent.pendingProposals().size());
        assertEquals(1, agent.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).size());
        var reopened = new AgentController(root.resolve("data"), (s, u) -> { throw new AssertionError("No AI on reload"); });
        var saved = reopened.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).get(0);
        assertEquals(target.id(), saved.id());
        assertEquals("test CLI", saved.title());
        assertEquals("Quiz", saved.type());
        assertEquals("Contract practice", saved.scope());
        assertEquals(LocalDate.of(2026, 11, 24), saved.dueDate());
    }

    @Test void anEmptyProposalListExplainsThereIsNothingToApprove() throws Exception {
        var agent = new AgentController(root.resolve("empty"), (s, u) -> { throw new AssertionError("No AI"); });
        assertTrue(runCli(agent, "proposals\nexit\n").contains("No pending proposals."));
    }

    private String runCli(AgentController agent, String commands) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        var output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            new CommandLineInterface(agent).run();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
