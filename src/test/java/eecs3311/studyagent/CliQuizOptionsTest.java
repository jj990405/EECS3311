package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliQuizOptionsTest {
    @TempDir Path root;
    @Test void pageScopedCliQuizUsesTheSelectedPhysicalSlide() throws Exception {
        AgentController agent=new AgentController(root.resolve("data"),(system,user)-> {
            assertTrue(user.contains("[Page 1]"));assertFalse(user.contains("[Page 2]"));
            assertTrue(user.contains("Open Closed"));assertFalse(user.contains("Command encapsulates"));
            return "{\"questions\":[{\"topic\":\"SOLID\",\"question\":\"What is Open Closed?\",\"choices\":[\"Extension\",\"Deletion\",\"One class\",\"No design\"],\"answerIndex\":0,\"explanation\":\"Open for extension.\"}]}";
        });
        var semester=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course=agent.createCourse(semester.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);
        agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,Path.of("docs/demo/sample-materials/week3-lecture.pdf"));
        InputStream beforeIn=System.in;PrintStream beforeOut=System.out;var output=new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("quiz create\npages\n1\n1\n1\n2\nmcq\nexit\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8));new CommandLineInterface(agent).run();
        } finally {System.setIn(beforeIn);System.setOut(beforeOut);}
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Error:"),output.toString(StandardCharsets.UTF_8));
        assertEquals(1,agent.quizzes(course.id()).size());
    }
    @Test void weekScopedEssayCountIsHonoredAndNoMcqIsCreated() throws Exception {
        AgentController agent = new AgentController(root.resolve("data"), (system,user) -> {
            assertTrue(system.contains("ESSAY_QUESTION")); assertTrue(user.contains("SOLID"));
            assertTrue(user.contains("Question difficulty: LOW"));
            assertTrue(user.contains(ExplanationStrategy.forDifficulty(Difficulty.HIGH).instruction()));
            return "{\"topic\":\"SOLID\",\"question\":\"Explain the Open Closed principle.\"}";
        });
        var semester = agent.createSemester("Fall", LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.HIGH);
        Path file = root.resolve("lecture.txt"); Files.writeString(file,"Week 3: SOLID principles.");
        var material = agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,file);
        agent.assignLectureWeek(material.id(),3);
        InputStream beforeIn=System.in; PrintStream beforeOut=System.out; var output=new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("quiz create\nweek\n3\n2\n1\nessay\nexit\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8)); new CommandLineInterface(agent).run();
        } finally { System.setIn(beforeIn); System.setOut(beforeOut); }
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Error:"));
        assertEquals(2,agent.essayPrompts(course.id()).size()); assertTrue(agent.quizzes(course.id()).isEmpty());
        assertTrue(agent.essayPrompts(course.id()).stream().allMatch(prompt->prompt.materialIds().equals(List.of(material.id()))));
    }
}
