package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
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
    public record SyllabusCandidate(UUID materialId, UUID courseId, ResponseParser.DeadlineDraft draft,
                                    List<String> warnings) { }
    public record EssayGradeDraft(UUID materialId, String question, String answer, int score, String feedback) { }

    private final DataStore store;
    private final PriorityStrategy recommender = new PriorityRecommender();
    private final CommandInvoker invoker = new CommandInvoker();
    private final PromptBuilder prompts = new PromptBuilder();
    private final ResponseParser parser = new ResponseParser();
    private final LlmClient llm;
    private final StudyTimer timer = new StudyTimer();
    private final StudyArtifacts artifacts;
    private final InteractionHistory history;
    private final Path briefingMarker;
    private final Path notificationMarker;
    private final Map<UUID, SyllabusCandidate> syllabusCandidates = new java.util.HashMap<>();

    public AgentController(Path dataDirectory) throws IOException {
        this(dataDirectory, new HttpLlmClient(Path.of(System.getProperty("project2.root", "."))));
    }

    public AgentController(Path dataDirectory, LlmClient llm) throws IOException {
        this.store = new DataStore(dataDirectory);
        this.llm = llm;
        this.artifacts = new StudyArtifacts(dataDirectory);
        this.history = new InteractionHistory(dataDirectory);
        this.briefingMarker = dataDirectory.toAbsolutePath().normalize().resolve("last-weekly-brief.txt");
        this.notificationMarker = dataDirectory.toAbsolutePath().normalize().resolve("shown-notifications.txt");
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
    public void approveProposal(UUID id) throws IOException {
        String description = proposalDescription(id);
        invoker.approve(id);
        syllabusCandidates.remove(id);
        history.add("approved", description);
    }
    public void rejectProposal(UUID id) {
        String description = proposalDescription(id);
        invoker.reject(id);
        syllabusCandidates.remove(id);
        try { history.add("rejected", description); }
        catch (IOException ex) { throw new IllegalStateException(ex); } }
    private String proposalDescription(UUID id) {
        return invoker.pending().stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown proposal: " + id)).description();
    }
    public List<InteractionHistory.Entry> history() throws IOException { return history.list(); }
    public void deleteHistory(InteractionHistory.Entry entry) throws IOException { history.delete(entry); }
    public void deleteHistory() throws IOException { history.deleteAll(); }
    public SyllabusCandidate syllabusCandidate(UUID proposalId) { return syllabusCandidates.get(proposalId); }
    public boolean hasSyllabusDuplicate(UUID proposalId) {
        SyllabusCandidate candidate = syllabusCandidates.get(proposalId);
        if (candidate == null) return false;
        var draft = candidate.draft();
        return store.deadlines().stream().anyMatch(d -> d.courseId().equals(candidate.courseId())
                && d.title().equalsIgnoreCase(draft.title())
                && java.util.Objects.equals(d.dueDate(), draft.dueDate()));
    }

    public CommandInvoker.Proposal reviseSyllabusCandidate(UUID proposalId,
            ResponseParser.DeadlineDraft replacement) throws IOException {
        SyllabusCandidate previous = syllabusCandidates.get(proposalId);
        if (previous == null) throw new IllegalArgumentException("This is not a syllabus proposal.");
        Course course = store.course(previous.courseId());
        Semester semester = store.semester(course.semesterId());
        String source = String.join("\n", store.materialPages(previous.materialId()));
        DeadlineValidator validator = new DeadlineValidator();
        List<String> warnings = validator.warnings(replacement, semester, source);
        SyllabusCandidate revised = new SyllabusCandidate(previous.materialId(), course.id(), replacement, warnings);
        syllabusCandidates.put(proposalId, revised);
        return invoker.replace(proposalId, syllabusDescription(course, revised),
                new AddDeadlineCommand(store, course.id(), replacement));
    }

    public CommandInvoker.Proposal mergeSyllabusCandidate(UUID proposalId) {
        SyllabusCandidate candidate = syllabusCandidates.get(proposalId);
        if (candidate == null) throw new IllegalArgumentException("This is not a syllabus proposal.");
        var draft = candidate.draft();
        Deadline existing = store.deadlines().stream()
                .filter(d -> d.courseId().equals(candidate.courseId())
                        && d.title().equalsIgnoreCase(draft.title())
                        && java.util.Objects.equals(d.dueDate(), draft.dueDate()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No matching deadline to merge."));
        return invoker.replace(proposalId, "Merge into existing deadline: " + draft.title(),
                () -> store.editDeadline(existing.id(), draft.title(), draft.type(), draft.dueDate(),
                        draft.scope(), draft.weight(), draft.evidence()));
    }

    private String syllabusDescription(Course course, SyllabusCandidate candidate) {
        var draft = candidate.draft();
        return course.name() + " / " + draft.title() + " / "
                + (draft.dueDate() == null ? "TBD" : draft.dueDate())
                + (draft.weight() == null ? "" : " / " + draft.weight() + "%")
                + "\nScope: " + draft.scope() + "\nEvidence: " + draft.evidence()
                + (candidate.warnings().isEmpty() ? "" : "\nWarnings: " + String.join("; ", candidate.warnings()));
    }

    public List<CommandInvoker.Proposal> suggestSyllabusDeadlines(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (file.category() != Category.SYLLABUS || !file.textAvailable())
            throw new IllegalArgumentException("Select a syllabus with extractable text.");
        Course course = store.course(file.courseId());
        Semester semester = store.semester(course.semesterId());
        String sourceText = String.join("\n", store.materialPages(materialId));
        String context = "Semester " + semester.startDate() + " to " + semester.endDate()
                + "; course " + course.name() + "; source " + file.originalName() + "\n"
                + prompts.pages(store.materialPages(materialId), 30000);
        List<ResponseParser.DeadlineDraft> drafts = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            String response = llm.complete(prompts.system(PromptBuilder.Task.DEADLINE_EXTRACTION),
                    prompts.user(PromptBuilder.Task.DEADLINE_EXTRACTION, context,
                            "Extract assessment deadlines; cite exact source wording and include weights."));
            try { drafts = parser.deadlines(response); break; }
            catch (RuntimeException ex) { if (attempt == 1) throw ex; }
        }
        List<CommandInvoker.Proposal> result = new java.util.ArrayList<>();
        DeadlineValidator validator = new DeadlineValidator();
        for (ResponseParser.DeadlineDraft raw : drafts) {
            ResponseParser.DeadlineDraft draft = validator.resolveRelativeDate(raw, semester);
            List<String> warnings = new java.util.ArrayList<>(validator.warnings(draft, semester, sourceText));
            if (store.deadlines().stream().anyMatch(d -> d.courseId().equals(course.id())
                    && d.title().equalsIgnoreCase(draft.title())
                    && java.util.Objects.equals(d.dueDate(), draft.dueDate())))
                warnings.add("A deadline with the same title and date already exists; review before approval.");
            SyllabusCandidate candidate = new SyllabusCandidate(materialId, course.id(), draft, warnings);
            CommandInvoker.Proposal proposal = invoker.propose(syllabusDescription(course, candidate),
                    new AddDeadlineCommand(store, course.id(), draft));
            syllabusCandidates.put(proposal.id(), candidate);
            result.add(proposal);
        }
        history.add("syllabus proposals", file.originalName() + ": " + result.size());
        return result;
    }

    public CommandInvoker.Proposal suggestLectureWeek(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (file.category() != Category.LECTURE_MATERIALS || !file.textAvailable())
            throw new IllegalArgumentException("Select lecture material with extractable text.");
        List<MaterialFile> syllabi = store.materials().stream()
                .filter(m -> m.courseId().equals(file.courseId()) && m.category() == Category.SYLLABUS
                        && m.textAvailable()).toList();
        if (syllabi.isEmpty()) throw new IllegalStateException(
                "No readable syllabus is available. Assign the lecture week manually.");
        StringBuilder context = new StringBuilder("Course: " + store.course(file.courseId()).name());
        for (MaterialFile syllabus : syllabi) context.append("\nSyllabus " ).append(syllabus.originalName())
                .append("\n").append(prompts.pages(store.materialPages(syllabus.id()), 14000));
        context.append("\nLecture material " ).append(file.originalName()).append("\n")
                .append(prompts.pages(store.materialPages(materialId), 14000));
        Integer week = parser.week(llm.complete(prompts.system(PromptBuilder.Task.WEEK_MATCH),
                prompts.user(PromptBuilder.Task.WEEK_MATCH,
                        context.toString(),
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
    public MaterialFile uploadMaterial(UUID courseId, Category category, Path path, boolean replace)
            throws IOException {
        return store.uploadMaterial(courseId, category, path, replace);
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
    public List<Deadline> unseenNotifications(java.time.LocalDateTime now) throws IOException {
        java.util.Set<String> shown = Files.exists(notificationMarker)
                ? new java.util.HashSet<>(Files.readAllLines(notificationMarker)) : java.util.Set.of();
        return store.deadlines().stream().filter(d -> {
            if (d.completed() || d.dueDate() == null) return false;
            java.time.LocalDateTime notifyAt = d.dueDate().atTime(d.dueTime())
                    .minusMinutes(d.notifyBeforeMinutes());
            return !now.isBefore(notifyAt) && !shown.contains(notificationKey(d));
        }).toList();
    }
    public void markNotificationShown(Deadline deadline) throws IOException {
        Files.writeString(notificationMarker, notificationKey(deadline) + System.lineSeparator(),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
    private String notificationKey(Deadline deadline) {
        return deadline.id() + "/" + deadline.dueDate() + "/" + deadline.dueTime()
                + "/" + deadline.notifyBeforeMinutes();
    }

    public void startStudy(UUID courseId, String topic) { store.course(courseId); timer.start(courseId, topic); }
    public void startStudy(int minutes) {
        if (minutes != 15 && minutes != 30 && minutes != 60)
            throw new IllegalArgumentException("Choose a 15, 30 or 60 minute study timer.");
        timer.start(null, "Study", minutes * 60L);
    }
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
    public List<StudySession> studySessions() { return store.studySessions(null); }
    public long weeklyStudySeconds(UUID courseId, LocalDate weekDate) {
        LocalDate monday = weekDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return studySessions(courseId).stream().filter(s -> !s.startedAt().toLocalDate().isBefore(monday)
                && !s.startedAt().toLocalDate().isAfter(monday.plusDays(6)))
                .mapToLong(StudySession::seconds).sum();
    }

    public List<MaterialFile> suggestReferences(UUID deadlineId) throws IOException {
        Deadline deadline = store.deadline(deadlineId);
        List<MaterialFile> candidates = store.materials().stream().filter(m -> m.courseId().equals(deadline.courseId())
                && m.category() == Category.LECTURE_MATERIALS && m.textAvailable()).toList();
        if (candidates.isEmpty()) throw new IllegalStateException("Upload readable lecture materials first.");
        List<MaterialFile> contextFiles = store.materials().stream().filter(m -> m.courseId().equals(deadline.courseId())
                && m.textAvailable() && (m.category() == Category.SYLLABUS
                        || m.category() == Category.ASSIGNMENT_SPECIFICATIONS)).toList();
        return new ReferenceSelector().select(llm, prompts, parser,
                "Select files relevant to " + deadline.title() + " / " + deadline.scope() + "\n"
                        + "Syllabus and assignment context:\n" + candidateSnippets(contextFiles)
                        + "Lecture candidates:\n" + candidateSnippets(candidates), candidates, true);
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
        history.add("question answered", "Question: " + question + "\nAnswer: " + result.answer()
                + "\nSources: " + result.citations());
        return result;
    }

    public StudyArtifacts.Summary generateSummary(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        if (!file.textAvailable()) throw new IllegalArgumentException("This file has no extractable text.");
        List<String> pages = store.materialPages(materialId);
        List<String> parts = new java.util.ArrayList<>();
        List<String> sources = new java.util.ArrayList<>();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            String page = pages.get(pageIndex);
            if (page.isBlank()) continue;
            int pageNumber = pageIndex + 1;
            for (int offset = 0; offset < page.length(); offset += 12000) {
                String section = page.substring(offset, Math.min(offset + 12000, page.length()));
                Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.SUMMARY),
                        prompts.user(PromptBuilder.Task.SUMMARY,
                                "File: " + file.originalName() + "\n[Page " + pageNumber + "]\n" + section,
                                "Summarize this section of the study material. "
                                        + ExplanationStrategy.forDifficulty(
                                                store.course(file.courseId()).difficulty()).instruction())));
                List<?> citations = Json.array(response.get("citations"));
                if (citations.isEmpty()) throw new IllegalArgumentException("Summary requires a source citation.");
                for (Object raw : citations) {
                    var citation = Json.object(raw);
                    if (!file.originalName().equals(Json.string(citation, "fileName"))
                            || !(citation.get("page") instanceof Number number)
                            || number.intValue() != pageNumber)
                        throw new IllegalArgumentException("Summary contains a citation outside its source section.");
                }
                parts.add(Json.string(response, "summary"));
                String source = file.originalName() + " p." + pageNumber;
                if (!sources.contains(source)) sources.add(source);
            }
        }
        if (parts.isEmpty()) throw new IllegalArgumentException("This file has no nonblank text to summarize.");
        StudyArtifacts.Summary summary = artifacts.saveSummary(materialId,
                String.join("\n\n", parts) + "\nSources: " + String.join(", ", sources));
        history.add("summary saved", file.originalName());
        return summary;
    }

    public List<StudyArtifacts.Summary> summaries(UUID materialId) throws IOException { return artifacts.summaries(materialId); }

    public StudyArtifacts.Quiz generateQuiz(UUID materialId) throws IOException {
        MaterialFile file = store.material(materialId);
        return generateQuiz(materialId, 3, store.course(file.courseId()).difficulty());
    }

    public StudyArtifacts.Quiz generateQuiz(UUID materialId, int count, Difficulty difficulty) throws IOException {
        return generateQuiz(List.of(materialId), count, difficulty);
    }

    public StudyArtifacts.Quiz generateQuiz(List<UUID> materialIds, int count, Difficulty difficulty)
            throws IOException {
        String context = quizContext(materialIds);
        MaterialFile file = store.material(materialIds.get(0));
        if (count < 1 || count > 10 || difficulty == null)
            throw new IllegalArgumentException("Choose 1-10 questions and a difficulty.");
        List<StudyArtifacts.Question> questions = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.QUIZ),
                        prompts.user(PromptBuilder.Task.QUIZ, context,
                                "Create exactly " + count + " multiple-choice study questions from the source. "
                                        + "Difficulty: " + difficulty + ". "
                                        + ExplanationStrategy.forDifficulty(difficulty).instruction())));
                List<StudyArtifacts.Question> parsed = new java.util.ArrayList<>();
                for (Object raw : Json.array(response.get("questions"))) {
                    var q = Json.object(raw);
                    parsed.add(new StudyArtifacts.Question(Json.string(q, "topic"), Json.string(q, "question"),
                            Json.array(q.get("choices")).stream().map(Object::toString).toList(),
                            ((Number) q.get("answerIndex")).intValue(), Json.string(q, "explanation")));
                }
                if (parsed.size() != count) throw new IllegalArgumentException("AI returned the wrong question count.");
                questions = parsed;
                break;
            } catch (RuntimeException ex) {
                if (attempt == 1) throw ex;
            }
        }
        StudyArtifacts.Quiz quiz = artifacts.saveQuiz(file.courseId(), file.id(), questions);
        history.add("quiz generated", file.originalName() + ": " + questions.size() + " questions");
        return quiz;
    }

    public List<StudyArtifacts.Quiz> quizzes(UUID courseId) throws IOException { return artifacts.quizzes(courseId); }
    public StudyArtifacts.Attempt gradeQuiz(StudyArtifacts.Quiz quiz, List<Integer> answers) throws IOException {
        StudyArtifacts.Attempt result = artifacts.grade(quiz, answers);
        StringBuilder detail = new StringBuilder(result.correct() + "/" + quiz.questions().size() + " correct");
        for (int i = 0; i < quiz.questions().size(); i++)
            detail.append("\n").append(quiz.questions().get(i).topic()).append(": ")
                    .append(answers.get(i).equals(quiz.questions().get(i).answerIndex())
                            ? "correct" : "incorrect");
        history.add("quiz graded", detail.toString());
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
    public List<WeakTopicAnalyzer.TopicStats> topicStatistics(UUID courseId) throws IOException {
        store.course(courseId);
        return new WeakTopicAnalyzer().statistics(artifacts, courseId);
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
        List<MaterialFile> lectures = store.materials().stream()
                .filter(m -> m.courseId().equals(courseId) && m.category() == Category.LECTURE_MATERIALS)
                .toList();
        var response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.WEAK_TOPICS),
                prompts.user(PromptBuilder.Task.WEAK_TOPICS,
                        "Course " + course.name() + "; per-topic quiz accuracy and confidence: "
                                + topicStatistics(courseId).stream().map(stats -> stats.topic() + ": "
                                        + (stats.accuracyPercent() == null ? "no quiz answers"
                                                : stats.accuracyPercent() + "% (" + stats.correct() + "/"
                                                        + stats.total() + ")")
                                        + ", confidence " + stats.confidence()).toList()
                                + "; available lecture files: "
                                + lectures.stream().map(MaterialFile::originalName).toList(),
                        "Explain weak topics and recommend only lecture files from the supplied list. "
                                + ExplanationStrategy.forDifficulty(course.difficulty()).instruction())));
        List<String> verifiedLectures = Json.array(response.get("lecturesToReview")).stream()
                .map(Object::toString).filter(name -> lectures.stream()
                        .anyMatch(file -> file.originalName().equals(name))).distinct().toList();
        String advice = Json.string(response, "explanation") + "\nLectures to review: "
                + (verifiedLectures.isEmpty() ? "No matching uploaded lecture files." : verifiedLectures);
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
        String briefing;
        try {
            Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.WEEKLY_BRIEF),
                    prompts.user(PromptBuilder.Task.WEEKLY_BRIEF, context.toString(),
                            "Give a concise weekly briefing and suggest what to study next.")));
            briefing = Json.string(response, "briefing");
        } catch (IOException | IllegalStateException ex) {
            briefing = context.toString();
        }
        history.add("weekly briefing", course.name());
        return briefing;
    }

    public boolean weeklyBriefDue(LocalDate today) throws IOException {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return !Files.exists(briefingMarker) || !Files.readString(briefingMarker).trim().equals(monday.toString());
    }

    public void markWeeklyBriefShown(LocalDate today) throws IOException {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Files.writeString(briefingMarker, monday.toString());
    }

    public CommandInvoker.Proposal proposeEssayGrade(UUID materialId, String question, String answer) throws IOException {
        EssayGradeDraft draft = suggestEssayGrade(materialId, question, answer);
        return proposeEssayGrade(draft, draft.score());
    }

    public EssayGradeDraft suggestEssayGrade(UUID materialId, String question, String answer) throws IOException {
        return suggestEssayGrade(List.of(materialId), question, answer);
    }

    public EssayGradeDraft suggestEssayGrade(List<UUID> materialIds, String question, String answer)
            throws IOException {
        String context = quizContext(materialIds);
        MaterialFile file = store.material(materialIds.get(0));
        if (question.isBlank() || answer.isBlank())
            throw new IllegalArgumentException("Enter an essay question and answer for a readable material.");
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.ESSAY_GRADE),
                prompts.user(PromptBuilder.Task.ESSAY_GRADE,
                        context,
                        "Question: " + question + "\nStudent answer: " + answer + "\nGive a score and feedback.")));
        int score = ((Number) response.get("score")).intValue();
        String feedback = Json.string(response, "feedback");
        if (score < 0 || score > 100 || feedback.isBlank()) throw new IllegalArgumentException("Invalid essay grade.");
        history.add("essay grade proposal", file.originalName() + " score " + score);
        return new EssayGradeDraft(file.id(), question, answer, score, feedback);
    }

    public CommandInvoker.Proposal proposeEssayGrade(EssayGradeDraft draft, int approvedScore) {
        store.material(draft.materialId());
        if (approvedScore < 0 || approvedScore > 100) throw new IllegalArgumentException("Essay score must be 0-100.");
        return invoker.propose("Save essay grade " + approvedScore + "/100: " + draft.feedback(),
                () -> artifacts.saveEssayGrade(draft.materialId(), draft.question(), draft.answer(),
                        approvedScore, draft.feedback()));
    }
    public List<StudyArtifacts.EssayGrade> essayGrades(UUID materialId) throws IOException {
        return artifacts.essayGrades(materialId);
    }
    public StudyArtifacts.EssayPrompt generateEssayPrompt(UUID materialId) throws IOException {
        return generateEssayPrompt(List.of(materialId));
    }

    public StudyArtifacts.EssayPrompt generateEssayPrompt(List<UUID> materialIds) throws IOException {
        String context = quizContext(materialIds);
        MaterialFile file = store.material(materialIds.get(0));
        Map<String, Object> response = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.ESSAY_QUESTION),
                prompts.user(PromptBuilder.Task.ESSAY_QUESTION,
                        context,
                        "Generate one essay question grounded in this material. "
                                + ExplanationStrategy.forDifficulty(store.course(file.courseId()).difficulty()).instruction())));
        StudyArtifacts.EssayPrompt result = artifacts.saveEssayPrompt(file.courseId(), materialIds,
                Json.string(response, "topic"), Json.string(response, "question"));
        history.add("essay question generated", file.originalName() + " / " + result.topic());
        return result;
    }

    private String quizContext(List<UUID> materialIds) throws IOException {
        if (materialIds == null || materialIds.isEmpty())
            throw new IllegalArgumentException("Choose at least one material for the quiz.");
        UUID courseId = store.material(materialIds.get(0)).courseId();
        StringBuilder context = new StringBuilder();
        for (UUID id : materialIds) {
            MaterialFile file = store.material(id);
            if (!file.courseId().equals(courseId) || !file.textAvailable())
                throw new IllegalArgumentException("Quiz scope must contain readable files from one course.");
            context.append("File: ").append(file.originalName()).append('\n')
                    .append(prompts.pages(store.materialPages(id), Math.max(1000, 25000 / materialIds.size())))
                    .append('\n');
        }
        return context.toString();
    }
    public List<StudyArtifacts.EssayPrompt> essayPrompts(UUID courseId) throws IOException {
        store.course(courseId);
        return artifacts.essayPrompts(courseId);
    }
    public CommandInvoker.Proposal proposeEssayGrade(StudyArtifacts.EssayPrompt prompt, String answer) throws IOException {
        if (!essayPrompts(prompt.courseId()).stream().anyMatch(p -> p.id().equals(prompt.id())))
            throw new IllegalArgumentException("Unknown saved essay prompt.");
        EssayGradeDraft draft = suggestEssayGrade(prompt.materialIds(), prompt.question(), answer);
        return proposeEssayGrade(draft, draft.score());
    }
}
