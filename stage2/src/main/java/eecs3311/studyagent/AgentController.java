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
    private final PriorityRecommender recommender = new PriorityRecommender();
    private final CommandInvoker invoker = new CommandInvoker();
    private final PromptBuilder prompts = new PromptBuilder();
    private final ResponseParser parser = new ResponseParser();
    private final LlmClient llm;
    private final StudyTimer timer = new StudyTimer();
    private final StudyArtifacts artifacts;

    public AgentController(Path dataDirectory) throws IOException {
        this(dataDirectory, new HttpLlmClient(Path.of(System.getProperty("project2.root", "."))));
    }

    public AgentController(Path dataDirectory, LlmClient llm) throws IOException {
        this.store = new DataStore(dataDirectory);
        this.llm = llm;
        this.artifacts = new StudyArtifacts(dataDirectory);
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
    public void approveProposal(UUID id) throws IOException { invoker.approve(id); }
    public void rejectProposal(UUID id) { invoker.reject(id); }

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
        return invoker.propose("Assign " + file.originalName() + " to lecture week " + week,
                new AssignWeekCommand(store, materialId, week));
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

    public void startStudy(UUID courseId, String topic) { store.course(courseId); timer.start(courseId, topic); }
    public void pauseStudy() { timer.pause(); }
    public void resumeStudy() { timer.resume(); }
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
                "Select files relevant to " + deadline.title() + " / " + deadline.scope(), candidates);
    }

    public GroundedAnswer answerQuestion(UUID courseId, String question) throws IOException {
        store.course(courseId);
        if (question == null || question.isBlank()) throw new IllegalArgumentException("Enter a question.");
        List<MaterialFile> candidates = store.materials().stream().filter(m -> m.courseId().equals(courseId)
                && m.textAvailable()).toList();
        List<MaterialFile> selected = new ReferenceSelector().select(llm, prompts, parser, question, candidates);
        if (selected.isEmpty()) throw new IllegalArgumentException("No uploaded source supports this question.");
        StringBuilder context = new StringBuilder();
        for (MaterialFile file : selected) {
            context.append("\n[File: ").append(file.originalName()).append("]\n")
                    .append(prompts.pages(store.materialPages(file.id()), 8000));
        }
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.QA),
                prompts.user(PromptBuilder.Task.QA, context.toString(), question)));
        List<GroundedAnswer.Citation> citations = new java.util.ArrayList<>();
        for (Object raw : Json.array(response.get("citations"))) {
            Map<String, Object> citation = Json.object(raw);
            String name = Json.string(citation, "fileName");
            int page = ((Number) citation.get("page")).intValue();
            MaterialFile source = selected.stream().filter(m -> m.originalName().equals(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Answer cites an unselected file: " + name));
            if (page < 1 || page > store.materialPages(source.id()).size())
                throw new IllegalArgumentException("Answer cites an invalid page: " + page);
            citations.add(new GroundedAnswer.Citation(name, page));
        }
        if (citations.isEmpty()) throw new IllegalArgumentException("AI answer has no verifiable source citation.");
        return new GroundedAnswer(Json.string(response, "answer"), citations);
    }

    public StudyArtifacts.Summary generateSummary(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable()) throw new IllegalArgumentException("This file has no extractable text.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.SUMMARY),
                prompts.user(PromptBuilder.Task.SUMMARY, "File: " + file.originalName() + "\n"
                        + prompts.pages(store.materialPages(materialId), 25000), "Summarize the study material.")));
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
        return artifacts.saveSummary(materialId, Json.string(response, "summary") + "\nSources: "
                + String.join(", ", sources));
    }

    public List<StudyArtifacts.Summary> summaries(UUID materialId) throws IOException { return artifacts.summaries(materialId); }

    public StudyArtifacts.Quiz generateQuiz(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable()) throw new IllegalArgumentException("This file has no extractable text.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.QUIZ),
                prompts.user(PromptBuilder.Task.QUIZ, "File: " + file.originalName() + "\n"
                        + prompts.pages(store.materialPages(materialId), 25000),
                        "Create three multiple-choice study questions from the source.")));
        List<StudyArtifacts.Question> questions = new java.util.ArrayList<>();
        for (Object raw : Json.array(response.get("questions"))) {
            var q = Json.object(raw);
            questions.add(new StudyArtifacts.Question(Json.string(q, "topic"), Json.string(q, "question"),
                    Json.array(q.get("choices")).stream().map(Object::toString).toList(),
                    ((Number) q.get("answerIndex")).intValue(), Json.string(q, "explanation")));
        }
        return artifacts.saveQuiz(file.courseId(), materialId, questions);
    }

    public List<StudyArtifacts.Quiz> quizzes(UUID courseId) throws IOException { return artifacts.quizzes(courseId); }
    public StudyArtifacts.Attempt gradeQuiz(StudyArtifacts.Quiz quiz, List<Integer> answers) throws IOException {
        return artifacts.grade(quiz, answers);
    }
    public List<StudyArtifacts.Attempt> quizAttempts(UUID quizId) throws IOException { return artifacts.attempts(quizId); }

    public CommandInvoker.Proposal planScheduleChange(UUID courseId, String request) throws IOException {
        if (request == null || request.isBlank()) throw new IllegalArgumentException("Enter a schedule change request.");
        return new SchedulePlanner().plan(store, llm, prompts, parser, invoker, courseId, request);
    }

    public List<Map.Entry<String, Integer>> weakTopics(UUID courseId) throws IOException {
        store.course(courseId);
        return new WeakTopicAnalyzer().analyze(artifacts, courseId);
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
        return Json.string(response, "briefing");
    }
}
