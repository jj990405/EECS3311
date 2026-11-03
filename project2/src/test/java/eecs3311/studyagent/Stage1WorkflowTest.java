package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Stage1WorkflowTest {
    @TempDir Path root;

    private AgentController controller(LlmClient client) throws Exception {
        return new AgentController(root.resolve("data"), client);
    }

    @Test void longSummaryProcessesEverySectionAndSavesIt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AgentController agent = controller((system, user) -> {
            calls.incrementAndGet();
            return "{\"summary\":\"Section " + calls.get() + "\",\"citations\":[{\"fileName\":\"long.txt\",\"page\":1}]}";
        });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.HIGH);
        Path source = root.resolve("long.txt");
        Files.writeString(source, "Design patterns. ".repeat(1800));
        MaterialFile file = agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, source);
        String summary = agent.generateSummary(file.id()).text();
        assertTrue(calls.get() >= 2, "A long source must reach the model in multiple sections");
        assertTrue(summary.contains("Section 1"));
        assertTrue(summary.contains("Section " + calls.get()));
        assertEquals(1, agent.summaries(file.id()).size());
    }

    @Test void syllabusWeekWeightReviewAndDuplicateReplacementPersist() throws Exception {
        AgentController agent = controller((system, user) ->
                "{\"deadlines\":[{\"title\":\"Quiz 1\",\"type\":\"Quiz\",\"dueDate\":null,"
                        + "\"weight\":10,\"scope\":\"Patterns\",\"evidence\":\"Week 5 quiz\"}]}");
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path first = Files.createDirectories(root.resolve("first")).resolve("syllabus.txt");
        Files.writeString(first, "Week 5 quiz: Design patterns.");
        MaterialFile original = agent.uploadMaterial(course.id(), Category.SYLLABUS, first);
        var proposal = agent.suggestSyllabusDeadlines(original.id()).get(0);
        assertEquals(LocalDate.of(2026, 9, 29), agent.syllabusCandidate(proposal.id()).draft().dueDate());
        assertEquals(10.0, agent.syllabusCandidate(proposal.id()).draft().weight());
        agent.reviseSyllabusCandidate(proposal.id(), new ResponseParser.DeadlineDraft("Quiz 1", "Quiz",
                LocalDate.of(2026, 9, 30), "Patterns and UML", "Week 5 quiz", 15.0));
        agent.approveProposal(proposal.id());
        assertTrue(agent.history().stream().anyMatch(entry -> entry.event().equals("approved")
                && entry.detail().contains("Quiz 1")));
        Deadline saved = agent.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).get(0);
        assertEquals(15.0, saved.weight());
        assertEquals("Week 5 quiz", saved.sourceText());
        var duplicate = agent.suggestSyllabusDeadlines(original.id()).get(0);
        agent.reviseSyllabusCandidate(duplicate.id(), new ResponseParser.DeadlineDraft("Quiz 1", "Quiz",
                LocalDate.of(2026, 9, 30), "Updated scope", "Week 5 quiz", 20.0));
        assertTrue(agent.hasSyllabusDuplicate(duplicate.id()));
        agent.mergeSyllabusCandidate(duplicate.id());
        agent.approveProposal(duplicate.id());
        assertEquals(1, agent.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now()).size());
        assertEquals(20.0, agent.deadline(saved.id()).weight());
        Path second = Files.createDirectories(root.resolve("second")).resolve("syllabus.txt");
        Files.writeString(second, "Updated syllabus text.");
        MaterialFile updated = agent.uploadMaterial(course.id(), Category.SYLLABUS, second, true);
        assertEquals(1, agent.materials().size());
        assertNotEquals(original.id(), updated.id());
        assertFalse(Files.exists(agent.materialPath(updated.id()).getParent().resolve(original.storedName())));
        assertTrue(agent.materialPages(updated.id()).get(0).contains("Updated syllabus"));
    }

    @Test void standaloneTimerNotificationsAndHistorySurviveRestart() throws Exception {
        AgentController agent = controller((system, user) -> { throw new java.io.IOException("offline"); });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        agent.startStudy(15);
        StudySession session = agent.stopStudy();
        assertNull(session.courseId());
        Deadline deadline = agent.addDeadline(course.id(), "Project", "Assignment",
                LocalDate.of(2026, 11, 5), "Stage 2");
        agent.setNotification(deadline.id(), LocalTime.NOON, 60);
        LocalDateTime afterMissedTime = LocalDateTime.of(2026, 11, 6, 9, 0);
        assertEquals(1, agent.unseenNotifications(afterMissedTime).size());
        agent.markNotificationShown(agent.deadline(deadline.id()));
        assertTrue(agent.unseenNotifications(afterMissedTime).isEmpty());
        assertTrue(agent.weeklyBriefing(course.id(), LocalDate.of(2026, 11, 2)).contains("Project"));
        var firstRecord = agent.history().get(0);
        int before = agent.history().size();
        agent.deleteHistory(firstRecord);
        assertEquals(before - 1, agent.history().size());
        AgentController reopened = new AgentController(root.resolve("data"), (system, user) -> "{}");
        assertEquals(1, reopened.studySessions().size());
        assertTrue(reopened.unseenNotifications(afterMissedTime).isEmpty());
    }

    @Test void referenceSelectionUsesSyllabusContextAndOnlyReturnsLectures() throws Exception {
        AgentController agent = controller((system, user) -> {
            assertTrue(user.contains("Topics: Facade"));
            assertTrue(user.contains("Lecture about Facade"));
            return "{\"fileNames\":[\"syllabus.txt\",\"lecture.txt\"]}";
        });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path syllabus = root.resolve("syllabus.txt");
        Path lecture = root.resolve("lecture.txt");
        Files.writeString(syllabus, "Topics: Facade");
        Files.writeString(lecture, "Lecture about Facade");
        agent.uploadMaterial(course.id(), Category.SYLLABUS, syllabus);
        agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, lecture);
        Deadline deadline = agent.addDeadline(course.id(), "Quiz", "Quiz", LocalDate.of(2026, 11, 5), "Facade");
        var selected = agent.suggestReferences(deadline.id());
        assertEquals(1, selected.size());
        assertEquals("lecture.txt", selected.get(0).originalName());
    }

    @Test void weekScopeQuizAndCorrectedEssayGradeUseAllSources() throws Exception {
        AgentController agent = controller((system, user) -> {
            assertTrue(user.contains("lecture1.txt"));
            assertTrue(user.contains("lecture2.txt"));
            if (system.contains("QUIZ")) return "{\"questions\":[{\"topic\":\"patterns\","
                    + "\"question\":\"What is Facade?\",\"choices\":[\"Interface\",\"Other\",\"Third\",\"Fourth\"],"
                    + "\"answerIndex\":0,\"explanation\":\"Simplifies access\"}]}";
            if (system.contains("ESSAY_QUESTION"))
                return "{\"topic\":\"patterns\",\"question\":\"Explain Facade\"}";
            return "{\"score\":70,\"feedback\":\"Add an example\"}";
        });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path first = root.resolve("lecture1.txt");
        Path second = root.resolve("lecture2.txt");
        Files.writeString(first, "Facade is a unified interface.");
        Files.writeString(second, "Facade hides subsystem complexity.");
        MaterialFile one = agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, first);
        MaterialFile two = agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, second);
        List<UUID> scope = List.of(one.id(), two.id());
        var quiz = agent.generateQuiz(scope, 1, Difficulty.HIGH);
        assertEquals(1, quiz.questions().size());
        var essay = agent.generateEssayPrompt(scope);
        AgentController reopened = new AgentController(root.resolve("data"), (system, user) -> {
            assertTrue(user.contains("lecture1.txt"));
            assertTrue(user.contains("lecture2.txt"));
            return "{\"score\":70,\"feedback\":\"Add an example\"}";
        });
        var savedPrompt = reopened.essayPrompts(course.id()).get(0);
        assertEquals(2, savedPrompt.materialIds().size());
        var draft = reopened.suggestEssayGrade(savedPrompt.materialIds(), savedPrompt.question(), "It simplifies use.");
        var proposal = reopened.proposeEssayGrade(draft, 85);
        assertTrue(reopened.essayGrades(one.id()).isEmpty());
        reopened.approveProposal(proposal.id());
        assertEquals(85, reopened.essayGrades(one.id()).get(0).score());
    }

    @Test void topicAccuracyAndLectureAdviceUseRealQuizResultsAndUploadedFiles() throws Exception {
        AgentController agent = controller((system, user) -> {
            if (system.contains("QUIZ")) return "{\"questions\":["
                    + "{\"topic\":\"Patterns\",\"question\":\"Facade?\",\"choices\":[\"A\",\"B\",\"C\",\"D\"],"
                    + "\"answerIndex\":0,\"explanation\":\"A\"},"
                    + "{\"topic\":\"UML\",\"question\":\"Class?\",\"choices\":[\"A\",\"B\",\"C\",\"D\"],"
                    + "\"answerIndex\":0,\"explanation\":\"A\"}]}";
            if (system.contains("WEAK_TOPICS")) {
                assertTrue(user.contains("Patterns"));
                assertTrue(user.contains("50"));
                assertTrue(user.contains("lecture.txt"));
                return "{\"explanation\":\"Review Patterns\",\"lecturesToReview\":[\"invented.pdf\",\"lecture.txt\"]}";
            }
            throw new IllegalArgumentException("Unexpected AI task");
        });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path lecture = root.resolve("lecture.txt");
        Files.writeString(lecture, "Facade and UML classes.");
        MaterialFile material = agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, lecture);
        var quiz = agent.generateQuiz(material.id(), 2, Difficulty.MEDIUM);
        agent.gradeQuiz(quiz, List.of(1, 0));
        agent.gradeQuiz(quiz, List.of(0, 0));
        agent.saveSelfAssessment(course.id(), "Patterns", 2);
        var patterns = agent.topicStatistics(course.id()).stream()
                .filter(topic -> topic.topic().equals("Patterns")).findFirst().orElseThrow();
        assertEquals(1, patterns.correct());
        assertEquals(2, patterns.total());
        assertEquals(50, patterns.accuracyPercent());
        assertEquals("Patterns", agent.weakTopics(course.id()).get(0).getKey());
        String advice = agent.weakTopicAdvice(course.id());
        assertTrue(advice.contains("lecture.txt"));
        assertFalse(advice.contains("invented.pdf"));
    }

    @Test void questionHistoryContainsAnswerAndSource() throws Exception {
        AgentController agent = controller((system, user) -> {
            if (system.contains("REFERENCE_SELECTION")) return "{\"fileNames\":[\"lecture.txt\"]}";
            return "{\"answer\":\"Facade provides one interface.\","
                    + "\"citations\":[{\"fileName\":\"lecture.txt\",\"page\":1}]}";
        });
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Path lecture = root.resolve("lecture.txt");
        Files.writeString(lecture, "Facade provides one interface to subsystem operations.");
        agent.uploadMaterial(course.id(), Category.LECTURE_MATERIALS, lecture);
        agent.answerQuestion(course.id(), "What does Facade provide?");
        assertTrue(agent.history().stream().anyMatch(entry -> entry.event().equals("question answered")
                && entry.detail().contains("Facade provides one interface.")
                && entry.detail().contains("lecture.txt")));
    }

    @Test void syllabusOrdinalDatesMatchOriginalText() {
        Semester semester = new Semester(UUID.randomUUID(), "Fall", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31));
        var draft = new ResponseParser.DeadlineDraft("Lab Test 1", "Test",
                LocalDate.of(2026, 10, 7), "Patterns", "Lab Test 1 Oct. 7th", 7.0);
        assertTrue(new DeadlineValidator().warnings(draft, semester,
                "Lab Test 1 Oct. 7th – 7%; Lab Test 2 Nov. 18th – 8%").isEmpty());
    }
}
