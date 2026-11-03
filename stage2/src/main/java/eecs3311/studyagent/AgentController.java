package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Facade shared by the GUI and CLI. User actions enter through this class. */
public final class AgentController {
    public enum Period { ALL, THIS_WEEK, THIS_MONTH }
    public record Progress(int completed, int total, int overdue) { }

    private final DataStore store;
    private final PriorityStrategy recommender = new PriorityRecommender();
    private final CommandInvoker invoker = new CommandInvoker();
    private final PromptBuilder prompts = new PromptBuilder();
    private final ResponseParser parser = new ResponseParser();
    private final LlmClient llm;
    private final StudyTimer timer = new StudyTimer();
    private final StudyArtifacts artifacts;
    private final InteractionHistory history;

    public AgentController(Path dataDirectory) throws IOException {
        this(dataDirectory, new HttpLlmClient(Path.of(System.getProperty("project2.root", "."))));
    }

    public AgentController(Path dataDirectory, LlmClient llm) throws IOException {
        this.store = new DataStore(dataDirectory);
        this.llm = llm;
        this.artifacts = new StudyArtifacts(dataDirectory);
        this.history = new InteractionHistory(dataDirectory);
    }

    public void addChangeListener(Runnable listener) { store.addChangeListener(listener); }
    public List<Semester> semesters() { return store.semesters(); }
    public List<Course> courses() { return store.courses(); }
    public List<MaterialFile> materials() { return store.materials(); }
    public List<WorkspaceNode> workspace() { return WorkspaceTree.build(store); }
    public Semester semester(UUID id) { return store.semester(id); }
    public Course course(UUID id) { return store.course(id); }
    public Deadline deadline(UUID id) { return store.deadline(id); }
    public Path materialPath(UUID id) { return store.materialPath(id); }
    public List<String> materialPages(UUID id) throws IOException { return store.materialPages(id); }
    public MaterialFile assignLectureWeek(UUID id, Integer week) throws IOException {
        return store.assignLectureWeek(id, week);
    }
    public List<CommandInvoker.Proposal> pendingProposals() { return invoker.pending(); }
    public void approveProposal(UUID id) throws IOException { invoker.approve(id); history.add("approved", id.toString()); }
    public void rejectProposal(UUID id) { invoker.reject(id); try { history.add("rejected", id.toString()); }
        catch (IOException ex) { throw new IllegalStateException(ex); } }
    public List<InteractionHistory.Entry> history() throws IOException { return history.list(); }
    public void deleteHistory() throws IOException { history.deleteAll(); }

    public List<CommandInvoker.Proposal> suggestSyllabusDeadlines(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (file.category() != Category.SYLLABUS || !file.textAvailable())
            throw new IllegalArgumentException("Select a syllabus with extractable text.");
        Course course = store.course(file.courseId());
        Semester semester = store.semester(course.semesterId());
        String context = "Semester " + semester.startDate() + " to " + semester.endDate()
                + "; course " + course.name() + "; source " + file.originalName() + "\n"
                + prompts.pages(store.materialPages(materialId), 30000);
        String response = llm.complete(prompts.system(PromptBuilder.Task.DEADLINE_EXTRACTION),
                prompts.user(PromptBuilder.Task.DEADLINE_EXTRACTION, context, "Extract assessment deadlines."));
        List<CommandInvoker.Proposal> result = new java.util.ArrayList<>();
        DeadlineValidator validator = new DeadlineValidator();
        for (ResponseParser.DeadlineDraft draft : parser.deadlines(response)) {
            validator.validate(draft, semester);
            result.add(invoker.propose("Add " + course.name() + " / " + draft.title() + " / "
                    + (draft.dueDate() == null ? "TBD" : draft.dueDate()) + " / evidence: " + draft.evidence(),
                    new AddDeadlineCommand(store, course.id(), draft)));
        }
        history.add("syllabus proposals", file.originalName() + ": " + result.size());
        return result;
    }

    public CommandInvoker.Proposal suggestLectureWeek(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (file.category() != Category.LECTURE_MATERIALS || !file.textAvailable())
            throw new IllegalArgumentException("Select lecture material with extractable text.");
        Integer week = parser.week(llm.complete(prompts.system(PromptBuilder.Task.WEEK_MATCH),
                prompts.user(PromptBuilder.Task.WEEK_MATCH,
                        "Course: " + store.course(file.courseId()).name() + "\n"
                                + prompts.pages(store.materialPages(materialId), 20000),
                        "Suggest the lecture week. Use null if the source does not support a week.")));
        if (week == null) throw new IllegalArgumentException("No supported lecture week was found.");
        CommandInvoker.Proposal proposal = invoker.propose("Assign " + file.originalName() + " to lecture week " + week,
                new AssignWeekCommand(store, materialId, week));
        history.add("lecture week proposal", proposal.description());
        return proposal;
    }

    public Semester createSemester(String name, LocalDate start, LocalDate end) throws IOException {
        return store.createSemester(name, start, end);
    }

    public Course createCourse(UUID semesterId, String name, String colorHex, Difficulty difficulty)
            throws IOException {
        return store.createCourse(semesterId, name, colorHex, difficulty);
    }

    public MaterialFile uploadMaterial(UUID courseId, Category category, Path path) throws IOException {
        return store.uploadMaterial(courseId, category, path);
    }

    public Deadline addDeadline(UUID courseId, String title, String type, LocalDate dueDate, String scope)
            throws IOException {
        return store.createDeadline(courseId, title, type, dueDate, scope);
    }

    public Deadline editDeadline(UUID id, String title, String type, LocalDate dueDate, String scope)
            throws IOException {
        return store.editDeadline(id, title, type, dueDate, scope);
    }

    public Deadline setCompleted(UUID id, boolean completed) throws IOException {
        return store.setCompleted(id, completed);
    }

    public void deleteDeadline(UUID id) throws IOException { store.deleteDeadline(id); }

    public List<Deadline> schedule(Period period, UUID courseId, String query, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<UUID, Course> byId = store.courses().stream().collect(Collectors.toMap(Course::id, Function.identity()));
        String term = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        List<Deadline> selected = store.deadlines().stream()
                .filter(d -> courseId == null || d.courseId().equals(courseId))
                .filter(d -> switch (period) {
                    case ALL -> true;
                    case THIS_WEEK -> d.dueDate() != null && !d.dueDate().isBefore(weekStart)
                            && !d.dueDate().isAfter(weekEnd);
                    case THIS_MONTH -> d.dueDate() != null && d.dueDate().getYear() == today.getYear()
                            && d.dueDate().getMonth() == today.getMonth();
                })
                .filter(d -> term.isEmpty() || d.title().toLowerCase(java.util.Locale.ROOT).contains(term)
                        || d.type().toLowerCase(java.util.Locale.ROOT).contains(term))
                .toList();
        return recommender.order(selected, byId);
    }

    public Progress progress(UUID courseId, LocalDate today) {
        store.course(courseId);
        List<Deadline> items = store.deadlines().stream().filter(d -> d.courseId().equals(courseId)).toList();
        int completed = (int) items.stream().filter(Deadline::completed).count();
        int overdue = (int) items.stream().filter(d -> !d.completed() && d.dueDate() != null
                && d.dueDate().isBefore(today)).count();
        return new Progress(completed, items.size(), overdue);
    }

    public String calendar(java.time.YearMonth month) {
        return new CalendarView().render(month, schedule(Period.ALL, null, "", month.atDay(1)));
    }

    public List<Deadline> reminders(LocalDate today, int daysAhead) {
        return new ReminderService().upcoming(schedule(Period.ALL, null, "", today), today, daysAhead);
    }
    public Deadline setNotification(UUID deadlineId, java.time.LocalTime dueTime, int minutesBefore) throws IOException {
        return store.setNotification(deadlineId, dueTime, minutesBefore);
    }
    public List<Deadline> checkNotifications(java.time.LocalDateTime now) {
        return new ReminderService().dueNotifications(store.deadlines(), now);
    }

    public void startStudy(UUID courseId, String topic) { store.course(courseId); timer.start(courseId, topic); }
    public void startStudy(UUID courseId, String topic, int minutes) {
        store.course(courseId);
        if (minutes < 1 || minutes > 240) throw new IllegalArgumentException("Choose 1-240 minutes.");
        timer.start(courseId, topic, minutes * 60L);
    }
    public void pauseStudy() { timer.pause(); }
    public void resumeStudy() { timer.resume(); }
    public long remainingStudySeconds() { return timer.remainingSeconds(); }
    public boolean studyActive() { return timer.active(); }
    public StudySession stopStudy() throws IOException { return store.recordStudySession(timer.stop()); }
    public List<StudySession> studySessions(UUID courseId) { return store.studySessions(courseId); }
    public long weeklyStudySeconds(UUID courseId, LocalDate weekDate) {
        LocalDate monday = weekDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return studySessions(courseId).stream().filter(s -> !s.startedAt().toLocalDate().isBefore(monday)
                && !s.startedAt().toLocalDate().isAfter(monday.plusDays(6)))
                .mapToLong(StudySession::seconds).sum();
    }

    public List<MaterialFile> suggestReferences(UUID deadlineId) throws IOException {
        Deadline deadline = store.deadline(deadlineId);
        List<MaterialFile> candidates = store.materials().stream().filter(m -> m.courseId().equals(deadline.courseId())
                && m.textAvailable()).toList();
        return new ReferenceSelector().select(llm, prompts, parser,
                "Select files relevant to " + deadline.title() + " / " + deadline.scope() + "\n"
                        + candidateSnippets(candidates), candidates);
    }

    private String candidateSnippets(List<MaterialFile> candidates) throws IOException {
        StringBuilder result = new StringBuilder();
        for (MaterialFile file : candidates) {
            List<String> pages = store.materialPages(file.id());
            if (!pages.isEmpty()) result.append(file.originalName()).append(": ")
                    .append(pages.get(0), 0, Math.min(400, pages.get(0).length())).append('\n');
        }
        return result.toString();
    }

    public GroundedAnswer answerQuestion(UUID courseId, String question) throws IOException {
        store.course(courseId);
        if (question == null || question.isBlank()) throw new IllegalArgumentException("Enter a question.");
        List<MaterialFile> candidates = store.materials().stream().filter(m -> m.courseId().equals(courseId)
                && m.textAvailable()).toList();
        List<MaterialFile> selected = new ReferenceSelector().select(llm, prompts, parser,
                question + "\n" + candidateSnippets(candidates), candidates);
        if (selected.isEmpty()) throw new IllegalArgumentException("No uploaded source supports this question.");
        StringBuilder context = new StringBuilder();
        List<MaterialSearcher.Passage> passages = new MaterialSearcher().search(store, selected, question, 8);
        if (passages.isEmpty()) throw new IllegalArgumentException("Selected sources contain no readable passages.");
        for (var passage : passages) context.append("\n[File: ").append(passage.fileName())
                .append("; Page: ").append(passage.page()).append("]\n").append(passage.text()).append('\n');
        context.append("\nExplanation style: ")
                .append(ExplanationStrategy.forDifficulty(store.course(courseId).difficulty()).instruction());
        context.append("\nRecent interaction context (not source evidence):\n");
        for (var entry : history.recent(5)) context.append(entry.event()).append(": ").append(entry.detail()).append('\n');
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.QA),
                prompts.user(PromptBuilder.Task.QA, context.toString(), question)));
        List<GroundedAnswer.Citation> citations = new java.util.ArrayList<>();
        for (Object raw : Json.array(response.get("citations"))) {
            Map<String, Object> citation = Json.object(raw);
            String name = Json.string(citation, "fileName");
            int page = ((Number) citation.get("page")).intValue();
            if (passages.stream().noneMatch(p -> p.fileName().equals(name) && p.page() == page))
                throw new IllegalArgumentException("Answer cites a page not supplied as evidence: " + name + " p." + page);
            citations.add(new GroundedAnswer.Citation(name, page));
        }
        if (citations.isEmpty()) throw new IllegalArgumentException("AI answer has no verifiable source citation.");
        GroundedAnswer result = new GroundedAnswer(Json.string(response, "answer"), citations);
        history.add("question answered", question);
        return result;
    }

    public StudyArtifacts.Summary generateSummary(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable()) throw new IllegalArgumentException("This file has no extractable text.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.SUMMARY),
                prompts.user(PromptBuilder.Task.SUMMARY, "File: " + file.originalName() + "\n"
                        + prompts.pages(store.materialPages(materialId), 25000),
                        "Summarize the study material. " + ExplanationStrategy.forDifficulty(
                                store.course(file.courseId()).difficulty()).instruction())));
        List<String> sources = new java.util.ArrayList<>();
        for (Object raw : Json.array(response.get("citations"))) {
            var citation = Json.object(raw);
            int page = ((Number) citation.get("page")).intValue();
            if (!file.originalName().equals(Json.string(citation, "fileName"))
                    || page < 1 || page > store.materialPages(materialId).size())
                throw new IllegalArgumentException("Summary contains an invalid citation.");
            sources.add(file.originalName() + " p." + page);
        }
        if (sources.isEmpty()) throw new IllegalArgumentException("Summary requires a source citation.");
        StudyArtifacts.Summary summary = artifacts.saveSummary(materialId, Json.string(response, "summary") + "\nSources: "
                + String.join(", ", sources));
        history.add("summary saved", file.originalName());
        return summary;
    }

    public List<StudyArtifacts.Summary> summaries(UUID materialId) throws IOException { return artifacts.summaries(materialId); }

    public StudyArtifacts.Quiz generateQuiz(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable()) throw new IllegalArgumentException("This file has no extractable text.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.QUIZ),
                prompts.user(PromptBuilder.Task.QUIZ, "File: " + file.originalName() + "\n"
                        + prompts.pages(store.materialPages(materialId), 25000),
                        "Create three multiple-choice study questions from the source. "
                                + ExplanationStrategy.forDifficulty(store.course(file.courseId()).difficulty()).instruction())));
        List<StudyArtifacts.Question> questions = new java.util.ArrayList<>();
        for (Object raw : Json.array(response.get("questions"))) {
            var q = Json.object(raw);
            questions.add(new StudyArtifacts.Question(Json.string(q, "topic"), Json.string(q, "question"),
                    Json.array(q.get("choices")).stream().map(Object::toString).toList(),
                    ((Number) q.get("answerIndex")).intValue(), Json.string(q, "explanation")));
        }
        StudyArtifacts.Quiz quiz = artifacts.saveQuiz(file.courseId(), materialId, questions);
        history.add("quiz generated", file.originalName() + ": " + questions.size() + " questions");
        return quiz;
    }

    public List<StudyArtifacts.Quiz> quizzes(UUID courseId) throws IOException { return artifacts.quizzes(courseId); }
    public StudyArtifacts.Attempt gradeQuiz(StudyArtifacts.Quiz quiz, List<Integer> answers) throws IOException {
        StudyArtifacts.Attempt result = artifacts.grade(quiz, answers);
        history.add("quiz graded", result.correct() + "/" + quiz.questions().size());
        return result;
    }
    public List<StudyArtifacts.Attempt> quizAttempts(UUID quizId) throws IOException { return artifacts.attempts(quizId); }

    public CommandInvoker.Proposal planScheduleChange(UUID courseId, String request) throws IOException {
        if (request == null || request.isBlank()) throw new IllegalArgumentException("Enter a schedule change request.");
        CommandInvoker.Proposal proposal = new SchedulePlanner().plan(store, llm, prompts, parser, invoker, courseId, request);
        history.add("schedule proposal", proposal.description());
        return proposal;
    }

    public List<Map.Entry<String, Integer>> weakTopics(UUID courseId) throws IOException {
        store.course(courseId);
        return new WeakTopicAnalyzer().analyze(artifacts, courseId);
    }
    public StudyArtifacts.SelfAssessment saveSelfAssessment(UUID courseId, String topic, int confidence) throws IOException {
        store.course(courseId);
        StudyArtifacts.SelfAssessment result = artifacts.saveAssessment(courseId, topic, confidence);
        history.add("self assessment", topic + " confidence " + confidence);
        return result;
    }

    public String weakTopicAdvice(UUID courseId) throws IOException {
        Course course = store.course(courseId);
        var ranking = weakTopics(courseId);
        if (ranking.isEmpty()) return "No quiz misses or self-assessments to analyze yet.";
        var response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.WEAK_TOPICS),
                prompts.user(PromptBuilder.Task.WEAK_TOPICS,
                        "Course " + course.name() + "; weak-topic scores: " + ranking,
                        "Explain weak topics and name lectures to review without inventing material. "
                                + ExplanationStrategy.forDifficulty(course.difficulty()).instruction())));
        String advice = Json.string(response, "explanation") + "\nLectures to review: "
                + Json.array(response.get("lecturesToReview"));
        history.add("weak topic advice", course.name());
        return advice;
    }

    public String weeklyBriefing(UUID courseId, LocalDate today) throws IOException {
        Course course = store.course(courseId);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        StringBuilder context = new StringBuilder("Course: " + course.name() + "\nWeek of " + monday + "\nDeadlines:\n");
        for (Deadline deadline : schedule(Period.THIS_WEEK, courseId, "", today))
            context.append(deadline.dueDate()).append(" ").append(deadline.title())
                    .append(deadline.completed() ? " completed" : " pending").append('\n');
        context.append("Study seconds this week: ").append(weeklyStudySeconds(courseId, today));
        context.append("\nWeak topics: ").append(weakTopics(courseId));
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.WEEKLY_BRIEF),
                prompts.user(PromptBuilder.Task.WEEKLY_BRIEF, context.toString(),
                        "Give a concise weekly briefing and suggest what to study next.")));
        String briefing = Json.string(response, "briefing");
        history.add("weekly briefing", course.name());
        return briefing;
    }

    public CommandInvoker.Proposal proposeEssayGrade(UUID materialId, String question, String answer) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable() || question.isBlank() || answer.isBlank())
            throw new IllegalArgumentException("Enter an essay question and answer for a readable material.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.ESSAY_GRADE),
                prompts.user(PromptBuilder.Task.ESSAY_GRADE,
                        "Source " + file.originalName() + "\n" + prompts.pages(store.materialPages(materialId), 20000),
                        "Question: " + question + "\nStudent answer: " + answer + "\nGive a score and feedback.")));
        int score = ((Number) response.get("score")).intValue();
        String feedback = Json.string(response, "feedback");
        if (score < 0 || score > 100 || feedback.isBlank()) throw new IllegalArgumentException("Invalid essay grade.");
        CommandInvoker.Proposal proposal = invoker.propose("Save essay grade " + score + "/100: " + feedback,
                () -> artifacts.saveEssayGrade(materialId, question, answer, score, feedback));
        history.add("essay grade proposal", file.originalName() + " score " + score);
        return proposal;
    }
    public List<StudyArtifacts.EssayGrade> essayGrades(UUID materialId) throws IOException {
        return artifacts.essayGrades(materialId);
    }
    public StudyArtifacts.EssayPrompt generateEssayPrompt(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable()) throw new IllegalArgumentException("This file has no extractable text.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.ESSAY_QUESTION),
                prompts.user(PromptBuilder.Task.ESSAY_QUESTION,
                        "File: " + file.originalName() + "\n" + prompts.pages(store.materialPages(materialId), 25000),
                        "Generate one essay question grounded in this material. "
                                + ExplanationStrategy.forDifficulty(store.course(file.courseId()).difficulty()).instruction())));
        StudyArtifacts.EssayPrompt result = artifacts.saveEssayPrompt(file.courseId(), materialId,
                Json.string(response, "topic"), Json.string(response, "question"));
        history.add("essay question generated", file.originalName() + " / " + result.topic());
        return result;
    }
    public List<StudyArtifacts.EssayPrompt> essayPrompts(UUID courseId) throws IOException {
        store.course(courseId);
        return artifacts.essayPrompts(courseId);
    }
    public CommandInvoker.Proposal proposeEssayGrade(StudyArtifacts.EssayPrompt prompt, String answer) throws IOException {
        if (!essayPrompts(prompt.courseId()).stream().anyMatch(p -> p.id().equals(prompt.id())))
            throw new IllegalArgumentException("Unknown saved essay prompt.");
        return proposeEssayGrade(prompt.materialId(), prompt.question(), answer);
    }
}
