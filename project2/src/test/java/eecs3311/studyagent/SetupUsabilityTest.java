package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetupUsabilityTest {
    @TempDir Path root;

    @Test void acceptsCompactAndSeparatedDatesButRejectsImpossibleDays() {
        assertEquals(LocalDate.of(2026, 12, 31), FlexibleDate.parse("20261231"));
        assertEquals(LocalDate.of(2026, 12, 31), FlexibleDate.parse("2026-12-31"));
        assertEquals(LocalDate.of(2026, 12, 31), FlexibleDate.parse("2026/12/31"));
        assertThrows(IllegalArgumentException.class, () -> FlexibleDate.parse("20260230"));
    }

    @Test void quickSetupCreatesCourseWithoutAskingForHexAndColorCanChange() throws Exception {
        AgentController agent = new AgentController(root.resolve("data"), (system, user) -> "{}");
        String output = runCli(agent, "setup\n\n\n\nEECS3311\n2\nn\nn\nexit\n");
        assertEquals(1, agent.semesters().size());
        assertEquals(1, agent.courses().size());
        assertEquals(CourseColors.PALETTE[0].hex(), agent.courses().get(0).colorHex());
        assertFalse(output.contains("#RRGGBB"));
        assertTrue(output.contains("Setup complete"));
        Course changed = agent.changeCourseColor(agent.courses().get(0).id(), CourseColors.PALETTE[4].hex());
        assertEquals(CourseColors.PALETTE[4].hex(), changed.colorHex());
        AgentController reopened = new AgentController(root.resolve("data"), (system, user) -> "{}");
        assertEquals(changed.colorHex(), reopened.courses().get(0).colorHex());
    }

    @Test void wrongDateRetriesWithinSemesterAddAndCourseAddNeedsNoRgbInput() throws Exception {
        AgentController agent = new AgentController(root.resolve("data"), (system, user) -> "{}");
        String output = runCli(agent, "semester add\nFall test\n20260230\n20260901\n20261231\n"
                + "course add\nEECS3311\n3\nexit\n");
        assertTrue(output.contains("Enter a real date"));
        assertEquals(LocalDate.of(2026, 9, 1), agent.semesters().get(0).startDate());
        assertEquals("EECS3311", agent.courses().get(0).name());
        assertEquals(Difficulty.HIGH, agent.courses().get(0).difficulty());
        assertFalse(output.contains("Course color (#RRGGBB)"));
    }

    @Test void quickSetupCanUploadSyllabusWithoutCallingAi() throws Exception {
        AgentController agent = new AgentController(root.resolve("data"),
                (system, user) -> { throw new AssertionError("Setup must not call the AI service"); });
        Path syllabus = root.resolve("syllabus.txt");
        Files.writeString(syllabus, "Course schedule and grading policy.");
        String output = runCli(agent, "setup\n\n\n\nEECS3311\n2\nn\ny\n" + syllabus + "\nexit\n");
        assertEquals(1, agent.materials().size());
        assertEquals(Category.SYLLABUS, agent.materials().get(0).category());
        assertTrue(output.contains("Uploaded syllabus.txt"));
    }

    private String runCli(AgentController agent, String commands) throws Exception {
        var previousIn = System.in;
        var previousOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            new CommandLineInterface(agent).run();
        } finally {
            System.setIn(previousIn);
            System.setOut(previousOut);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
