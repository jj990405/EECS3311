package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuizGenerationTest {
    @TempDir Path root;

    private MaterialFile material(AgentController agent) throws Exception {
        var semester = agent.createSemester("Fall", LocalDate.of(2026,9,1), LocalDate.of(2026,12,31));
        var course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.HIGH);
        Path source = root.resolve("lecture.txt"); Files.writeString(source, "SOLID: one reason to change.");
        return agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, source);
    }

    @Test void easyQuestionStillUsesDifficultCourseExplanationStyleForMcqAndEssayFeedback() throws Exception {
        AgentController agent = new AgentController(root.resolve("data"), (system,user) -> {
            assertTrue(user.contains(ExplanationStrategy.forDifficulty(Difficulty.HIGH).instruction()));
            if (system.contains("ESSAY_GRADE")) return "{\"score\":75,\"feedback\":\"Define the single responsibility first.\"}";
            assertTrue(user.contains("Difficulty: LOW"));
            return "{\"questions\":[{\"topic\":\"SOLID\",\"question\":\"What is SRP?\",\"choices\":[\"One reason to change\",\"Two reasons\",\"Three reasons\",\"No reason\"],\"answerIndex\":0,\"explanation\":\"A class has one reason to change.\"}]}";
        });
        var file = material(agent);
        assertEquals(1, agent.generateQuiz(file.id(),1,Difficulty.LOW).questions().size());
        var draft = agent.suggestEssayGrade(file.id(), "Explain SRP", "One responsibility.");
        assertEquals(75,draft.score());
        assertTrue(agent.essayGrades(file.id()).isEmpty(), "Feedback must remain unsaved until approval");
    }

    @Test void malformedEssayIsRegeneratedBeforeAnyQuestionIsSaved() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AgentController agent = new AgentController(root.resolve("data"), (system,user) -> {
            assertTrue(user.contains("Question difficulty: LOW"));
            return calls.incrementAndGet()==1 ? "{\"topic\":\"\",\"question\":\"\"}"
                    : "{\"topic\":\"SOLID\",\"question\":\"Explain one reason to change.\"}";
        });
        var file = material(agent);
        var prompt = agent.generateEssayPrompt(List.of(file.id()), Difficulty.LOW);
        assertEquals(2,calls.get()); assertFalse(prompt.question().isBlank());
        assertEquals(1,agent.essayPrompts(file.courseId()).size());
    }
}
