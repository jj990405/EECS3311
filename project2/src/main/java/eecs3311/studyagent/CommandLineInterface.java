package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/** Interactive CLI for the implemented semester and deadline workflow. */
public final class CommandLineInterface {
    private final AgentController controller;
    private final Scanner input = new Scanner(System.in);

    public CommandLineInterface(AgentController controller) {
        this.controller = controller;
    }

    public void run() {
        System.out.println("EECS3311 Study Agent");
        System.out.println(controller.semesters().isEmpty()
                ? "First time? Type setup to add a semester and course step by step."
                : "Type help for commands, or setup to add a course step by step.");
        printDueNotifications();
        while (true) {
            System.out.print("study> ");
            if (!input.hasNextLine()) return;
            String command = input.nextLine().trim().toLowerCase();
            try {
                switch (command) {
                    case "setup" -> setup();
                    case "help" -> help();
                    case "semester add" -> addSemester();
                    case "course add" -> addCourse();
                    case "material add" -> addMaterial();
                    case "deadline add" -> addDeadline();
                    case "deadline edit" -> editDeadline();
                    case "deadline delete" -> deleteDeadline();
                    case "timeline" -> timeline();
                    case "calendar" -> calendar();
                    case "reminders" -> reminders();
                    case "notifications" -> printDueNotifications();
                    case "deadline notify" -> configureNotification();
                    case "study week" -> studyWeek();
                    case "ai references" -> references();
                    case "ask" -> askQuestion();
                    case "summary create" -> createSummary();
                    case "summary list" -> listSummaries();
                    case "quiz create" -> createQuiz();
                    case "quiz take" -> takeQuiz();
                    case "ai schedule" -> planSchedule();
                    case "weak topics" -> weakTopics();
                    case "weekly brief" -> weeklyBrief();
                    case "essay grade" -> essayGrade();
                    case "essay create" -> essayCreate();
                    case "essay take" -> essayTake();
                    case "essay list" -> essayList();
                    case "history" -> history();
                    case "history delete" -> deleteHistory();
                    case "self assess" -> selfAssess();
                    case "weak advice" -> weakAdvice();
                    case "complete" -> complete();
                    case "progress" -> progress();
                    case "tree" -> showTree();
                    case "ai syllabus" -> suggestSyllabus();
                    case "ai week" -> suggestWeek();
                    case "proposals" -> proposals();
                    case "approve" -> approve();
                    case "reject" -> reject();
                    case "exit", "quit" -> { return; }
                    case "" -> { }
                    default -> System.out.println("Unknown command. Type help.");
                }
            } catch (IOException | RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void help() {
        System.out.println("Get started: setup, material add, tree");
        System.out.println("Plan: deadline add/edit/delete, timeline, calendar, reminders, complete, progress");
        System.out.println("Study: study week, ask, summary create/list, quiz create/take, weak topics");
        System.out.println("AI: ai syllabus, ai week, ai references, ai schedule, weekly brief, proposals, approve, reject");
        System.out.println("More: semester add, course add, deadline notify, notifications, self assess,");
        System.out.println("      weak advice, essay create/take/grade/list, history, history delete, exit");
        System.out.println("Dates: 20261231 or 2026-12-31. A blank deadline date means TBD.");
    }

    private String ask(String label) {
        System.out.print(label + ": ");
        return input.nextLine().trim();
    }

    private LocalDate date(String label, boolean optional) {
        while (true) {
            String value = ask(label + " (YYYYMMDD or YYYY-MM-DD"
                    + (optional ? ", blank for TBD" : "") + ")");
            if (optional && value.isBlank()) return null;
            try { return FlexibleDate.parse(value); }
            catch (IllegalArgumentException ex) { System.out.println(ex.getMessage()); }
        }
    }

    private String askDefault(String label, String suggested) {
        String value = ask(label + " [" + suggested + "]");
        return value.isBlank() ? suggested : value;
    }

    private LocalDate dateDefault(String label, LocalDate suggested) {
        while (true) {
            String value = askDefault(label + " (YYYYMMDD or YYYY-MM-DD)", suggested.toString());
            try { return FlexibleDate.parse(value); }
            catch (IllegalArgumentException ex) { System.out.println(ex.getMessage()); }
        }
    }

    private Difficulty chooseDifficulty() {
        while (true) {
            String value = askDefault("Difficulty: 1 Easy, 2 Medium, 3 Hard", "2");
            switch (value.toLowerCase()) {
                case "1", "easy", "low" -> { return Difficulty.LOW; }
                case "2", "medium" -> { return Difficulty.MEDIUM; }
                case "3", "hard", "high" -> { return Difficulty.HIGH; }
                default -> System.out.println("Choose 1, 2, or 3.");
            }
        }
    }

    private int number(String label, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(ask(label));
                if (value >= 1 && value <= max) return value - 1;
            } catch (NumberFormatException ignored) {
                // Keep this choice open so an incorrect number does not restart the task.
            }
            System.out.println("Choose a number from 1 to " + max + ".");
        }
    }

    private Semester chooseSemester() {
        List<Semester> items = controller.semesters();
        if (items.isEmpty()) throw new IllegalStateException("Create a semester first.");
        if (items.size() == 1) return items.get(0);
        for (int i = 0; i < items.size(); i++) {
            Semester s = items.get(i);
            System.out.printf("%d. %s (%s to %s)%n", i + 1, s.name(), s.startDate(), s.endDate());
        }
        return items.get(number("Semester number", items.size()));
    }

    private Course chooseCourse() {
        List<Course> items = controller.courses();
        if (items.isEmpty()) throw new IllegalStateException("Create a course first.");
        if (items.size() == 1) return items.get(0);
        for (int i = 0; i < items.size(); i++) {
            Course c = items.get(i);
            System.out.printf("%d. %s - %s%n", i + 1, controller.semester(c.semesterId()).name(), c);
        }
        return items.get(number("Course number", items.size()));
    }

    private Deadline chooseDeadline() {
        List<Deadline> items = controller.schedule(AgentController.Period.ALL, null, "", LocalDate.now());
        if (items.isEmpty()) throw new IllegalStateException("There are no deadlines yet.");
        for (int i = 0; i < items.size(); i++) {
            Deadline d = items.get(i);
            System.out.printf("%d. %s | %s | %s%s%n", i + 1, controller.course(d.courseId()).name(),
                    d.title(), d.dueDate() == null ? "TBD" : d.dueDate(), d.completed() ? " [done]" : "");
        }
        return items.get(number("Deadline number", items.size()));
    }

    private MaterialFile chooseMaterial(Category category) {
        List<MaterialFile> items = controller.materials().stream().filter(m -> m.category() == category).toList();
        if (items.isEmpty()) throw new IllegalStateException("No material in " + category);
        for (int i = 0; i < items.size(); i++) System.out.printf("%d. %s%n", i + 1, items.get(i).originalName());
        return items.get(number("Material number", items.size()));
    }

    private MaterialFile chooseReadableMaterial() {
        List<MaterialFile> items = controller.materials().stream().filter(MaterialFile::textAvailable).toList();
        if (items.isEmpty()) throw new IllegalStateException("Upload a readable material first.");
        for (int i = 0; i < items.size(); i++) System.out.printf("%d. %s%n", i + 1, items.get(i).originalName());
        return items.get(number("Material number", items.size()));
    }

    private CommandInvoker.Proposal chooseProposal() {
        List<CommandInvoker.Proposal> items = controller.pendingProposals();
        if (items.isEmpty()) throw new IllegalStateException("No pending proposals.");
        for (int i = 0; i < items.size(); i++) System.out.printf("%d. %s%n", i + 1, items.get(i).description());
        return items.get(number("Proposal number", items.size()));
    }

    private void suggestSyllabus() throws IOException {
        var results = controller.suggestSyllabusDeadlines(chooseMaterial(Category.SYLLABUS).id());
        System.out.println(results.size() + " deadline proposals. Review with 'proposals'.");
    }

    private void suggestWeek() throws IOException {
        var result = controller.suggestLectureWeek(chooseMaterial(Category.LECTURE_MATERIALS).id());
        System.out.println(result.description());
    }

    private void proposals() { for (var item : controller.pendingProposals()) System.out.println(item.description()); }
    private void approve() throws IOException { controller.approveProposal(chooseProposal().id()); System.out.println("Approved."); }
    private void reject() { controller.rejectProposal(chooseProposal().id()); System.out.println("Rejected."); }

    private void addSemester() throws IOException {
        Semester s = controller.createSemester(ask("Name"), date("Start date", false), date("End date", false));
        System.out.println("Created semester: " + s.name());
    }

    private void addCourse() throws IOException {
        Semester semester = chooseSemester();
        String name = ask("Course name");
        Course course = controller.createCourse(semester.id(), name,
                CourseColors.suggested(controller.courses().size()).hex(), chooseDifficulty());
        System.out.println("Created course: " + course.name() + ". Color assigned automatically.");
    }

    private void setup() throws IOException {
        System.out.println("Quick setup. Press Enter to accept a value in brackets.");
        Semester semester;
        if (controller.semesters().isEmpty()) {
            LocalDate today = LocalDate.now();
            int year = today.getYear();
            String term = today.getMonthValue() <= 4 ? "Winter" : today.getMonthValue() <= 8 ? "Summer" : "Fall";
            LocalDate start = LocalDate.of(year, term.equals("Winter") ? 1 : term.equals("Summer") ? 5 : 9, 1);
            LocalDate end = YearMonth.of(year, term.equals("Winter") ? 4 : term.equals("Summer") ? 8 : 12)
                    .atEndOfMonth();
            String name = askDefault("Semester name", term + " " + year);
            semester = controller.createSemester(name, dateDefault("Start date", start),
                    dateDefault("End date", end));
            System.out.println("Semester created: " + semester.name());
        } else {
            semester = chooseSemester();
            System.out.println("Using semester: " + semester.name());
        }
        do {
            String name;
            do { name = ask("Course name (e.g. EECS3311)"); }
            while (name.isBlank());
            Course course = controller.createCourse(semester.id(), name,
                    CourseColors.suggested(controller.courses().size()).hex(), chooseDifficulty());
            System.out.println("Added " + course.name() + ". Color assigned automatically.");
        } while (ask("Add another course? (y/N)").equalsIgnoreCase("y"));
        if (ask("Upload a syllabus now? (y/N)").equalsIgnoreCase("y")) {
            Course course = chooseCourse();
            Path source = Path.of(ask("Syllabus file path"));
            MaterialFile file = controller.uploadMaterial(course.id(), Category.SYLLABUS, source);
            System.out.println("Uploaded " + file.originalName() + ". Type ai syllabus to review deadline suggestions.");
        }
        System.out.println("Setup complete. Type material add for more files, or help for commands.");
    }

    private void addMaterial() throws IOException {
        Course course = chooseCourse();
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) System.out.printf("%d. %s%n", i + 1, categories[i]);
        Category category = categories[number("Category number", categories.length)];
        System.out.println("Enter one file path per line; leave the next line blank when finished.");
        int added = 0;
        while (true) {
            String path = ask("File path");
            if (path.isBlank()) break;
            Path source = Path.of(path);
            boolean duplicate = controller.materials().stream().anyMatch(m -> m.courseId().equals(course.id())
                    && m.category() == category && m.originalName().equalsIgnoreCase(source.getFileName().toString()));
            boolean replace = duplicate && ask("File already exists. Replace it? (y/N)").equalsIgnoreCase("y");
            if (duplicate && !replace) continue;
            MaterialFile material = controller.uploadMaterial(course.id(), category, source, replace);
            added++;
            System.out.println("Stored: " + material.originalName());
            if (!material.extractionWarning().isBlank()) System.out.println(material.extractionWarning());
            if (category == Category.LECTURE_MATERIALS && material.textAvailable()) {
                try {
                    var proposal = controller.suggestLectureWeek(material.id());
                    System.out.println(proposal.description());
                    if (ask("Approve this week? (y/N)").equalsIgnoreCase("y"))
                        controller.approveProposal(proposal.id());
                    else controller.rejectProposal(proposal.id());
                } catch (IOException | RuntimeException ex) {
                    System.out.println("Week suggestion unavailable: " + ex.getMessage()
                            + ". Use 'ai week' later or assign the week in the GUI.");
                }
            }
        }
        System.out.println(added + " file(s) stored.");
    }

    private void addDeadline() throws IOException {
        Course course = chooseCourse();
        Deadline deadline = controller.addDeadline(course.id(), ask("Title"), ask("Type"),
                date("Due date", true), ask("Covered topics / scope"));
        System.out.println("Created deadline: " + deadline.title());
    }

    private void editDeadline() throws IOException {
        Deadline old = chooseDeadline();
        System.out.println("Enter the replacement details.");
        controller.editDeadline(old.id(), ask("Title"), ask("Type"), date("Due date", true), ask("Scope"));
        System.out.println("Deadline updated.");
    }

    private void deleteDeadline() throws IOException {
        Deadline old = chooseDeadline();
        if (ask("Delete '" + old.title() + "'? (y/N)").equalsIgnoreCase("y")) {
            controller.deleteDeadline(old.id());
            System.out.println("Deadline deleted.");
        }
    }

    private void complete() throws IOException {
        Deadline item = chooseDeadline();
        controller.setCompleted(item.id(), !item.completed());
        System.out.println(item.completed() ? "Marked incomplete." : "Marked complete.");
    }

    private void progress() {
        Course course = chooseCourse();
        AgentController.Progress p = controller.progress(course.id(), LocalDate.now());
        System.out.printf("%s: %d/%d completed, %d overdue%n", course.name(), p.completed(), p.total(), p.overdue());
    }

    private void timeline() {
        String selection = ask("Period (all/week/month)").toLowerCase();
        AgentController.Period period = switch (selection) {
            case "all" -> AgentController.Period.ALL;
            case "week" -> AgentController.Period.THIS_WEEK;
            case "month" -> AgentController.Period.THIS_MONTH;
            default -> throw new IllegalArgumentException("Choose all, week, or month.");
        };
        String filter = ask("Filter by course? (y/N)");
        UUID courseId = filter.equalsIgnoreCase("y") ? chooseCourse().id() : null;
        List<Deadline> results = controller.schedule(period, courseId, ask("Search title/type (optional)"),
                LocalDate.now());
        if (results.isEmpty()) {
            System.out.println("No results.");
            return;
        }
        String previousDate = "";
        for (Deadline d : results) {
            String date = d.dueDate() == null ? "TBD" : d.dueDate().toString();
            if (!date.equals(previousDate)) System.out.println("\n" + date);
            System.out.printf("  %s | %s | %s | %s%s%n",
                    d.dueDate() == null ? "TBD" : d.dueTime(),
                    controller.course(d.courseId()).name(), d.type(),
                    d.title(), d.completed() ? " [done]" : "");
            previousDate = date;
        }
    }

    private void calendar() {
        String month = ask("Month (YYYY-MM, blank for current month)");
        System.out.println(controller.calendar(month.isBlank() ? java.time.YearMonth.now() : java.time.YearMonth.parse(month)));
    }

    private void reminders() {
        String input = ask("Days ahead (blank for 7)");
        int days = input.isBlank() ? 7 : Integer.parseInt(input);
        List<Deadline> items = controller.reminders(LocalDate.now(), days);
        if (items.isEmpty()) System.out.println("No upcoming deadlines.");
        for (Deadline item : items) System.out.println(item.dueDate() + " " + controller.course(item.courseId()).name()
                + " " + item.title());
    }

    private void configureNotification() throws IOException {
        Deadline deadline = chooseDeadline();
        java.time.LocalTime time = java.time.LocalTime.parse(ask("Due time (HH:MM, 24-hour)"));
        int minutes = Integer.parseInt(ask("Notify minutes before due time"));
        controller.setNotification(deadline.id(), time, minutes);
        System.out.println("Notification time saved.");
    }

    private void printDueNotifications() {
        try {
            for (Deadline deadline : controller.unseenNotifications(java.time.LocalDateTime.now())) {
                System.out.println("DEADLINE NOTICE: " + controller.course(deadline.courseId()).name() + " "
                        + deadline.title() + " at " + deadline.dueDate() + " " + deadline.dueTime());
                controller.markNotificationShown(deadline);
            }
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    private void studyWeek() {
        LocalDate day = date("A date in the week", false);
        LocalDate monday = day.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        long total = 0;
        for (StudySession session : controller.studySessions()) {
            LocalDate sessionDay = session.startedAt().toLocalDate();
            if (sessionDay.isBefore(monday) || !sessionDay.isBefore(monday.plusDays(7))) continue;
            System.out.println(sessionDay + " " + session.seconds() / 60 + " min");
            total += session.seconds();
        }
        System.out.println("Week of " + monday + ": " + total / 60 + " min total.");
    }

    private void references() throws IOException {
        List<MaterialFile> files = controller.suggestReferences(chooseDeadline().id());
        if (files.isEmpty()) System.out.println("No matching uploaded references.");
        for (MaterialFile file : files) System.out.println(file.originalName());
    }

    private void askQuestion() throws IOException {
        Course course = chooseCourse();
        GroundedAnswer answer = controller.answerQuestion(course.id(), ask("Question"));
        System.out.println(answer.answer());
        for (GroundedAnswer.Citation citation : answer.citations())
            System.out.println("Source: " + citation.fileName() + " p." + citation.page());
    }

    private void createSummary() throws IOException {
        System.out.println(controller.generateSummary(chooseReadableMaterial().id()).text());
    }
    private void listSummaries() throws IOException {
        for (var item : controller.summaries(chooseReadableMaterial().id())) System.out.println(item.text() + "\n");
    }
    private void createQuiz() throws IOException {
        MaterialFile material = chooseReadableMaterial();
        int count = Integer.parseInt(ask("Question count (1-10)"));
        Difficulty difficulty = Difficulty.valueOf(ask("Difficulty (HIGH/MEDIUM/LOW)").toUpperCase());
        var quiz = controller.generateQuiz(material.id(), count, difficulty);
        System.out.println("Saved quiz " + quiz.id() + " with " + quiz.questions().size() + " questions.");
    }
    private void takeQuiz() throws IOException {
        List<StudyArtifacts.Quiz> quizzes = controller.quizzes(chooseCourse().id());
        if (quizzes.isEmpty()) throw new IllegalStateException("Generate a quiz first.");
        for (int i = 0; i < quizzes.size(); i++) System.out.printf("%d. %s%n", i + 1, quizzes.get(i).id());
        StudyArtifacts.Quiz quiz = quizzes.get(number("Quiz number", quizzes.size()));
        List<Integer> answers = new java.util.ArrayList<>();
        for (var question : quiz.questions()) {
            System.out.println(question.text());
            for (int i = 0; i < 4; i++) System.out.printf("%d. %s%n", i + 1, question.choices().get(i));
            answers.add(number("Answer", 4));
        }
        var result = controller.gradeQuiz(quiz, answers);
        System.out.println("Score: " + result.correct() + "/" + quiz.questions().size());
        for (int i = 0; i < answers.size(); i++) if (answers.get(i) != quiz.questions().get(i).answerIndex())
            System.out.println("Review " + quiz.questions().get(i).topic() + ": " + quiz.questions().get(i).explanation());
    }

    private void planSchedule() throws IOException {
        Course course = chooseCourse();
        System.out.println(controller.planScheduleChange(course.id(), ask("Schedule change request")).description());
        System.out.println("Review and approve it with 'proposals' and 'approve'.");
    }
    private void weakTopics() throws IOException {
        var topics = controller.topicStatistics(chooseCourse().id());
        if (topics.isEmpty()) System.out.println("No quiz or self-assessment topics yet.");
        for (var topic : topics) System.out.println(topic.topic() + " — "
                + (topic.accuracyPercent() == null ? "no quiz answers" : topic.accuracyPercent()
                        + "% quiz accuracy (" + topic.correct() + "/" + topic.total() + ")")
                + (topic.confidence() == null ? "" : ", self-confidence " + topic.confidence() + "/5"));
    }
    private void weeklyBrief() throws IOException {
        System.out.println(controller.weeklyBriefing(chooseCourse().id(), LocalDate.now()));
    }

    private void essayGrade() throws IOException {
        MaterialFile material = chooseReadableMaterial();
        System.out.println(controller.proposeEssayGrade(material.id(), ask("Essay question"), ask("Your answer"))
                .description());
        System.out.println("Approve to save this grade.");
    }
    private void essayCreate() throws IOException {
        var prompt = controller.generateEssayPrompt(chooseReadableMaterial().id());
        System.out.println("Saved essay question: " + prompt.question());
    }
    private void essayTake() throws IOException {
        Course course = chooseCourse();
        var prompts = controller.essayPrompts(course.id());
        if (prompts.isEmpty()) throw new IllegalStateException("Generate an essay question first.");
        for (int i = 0; i < prompts.size(); i++) System.out.printf("%d. %s%n", i + 1, prompts.get(i).question());
        var prompt = prompts.get(number("Essay question number", prompts.size()));
        System.out.println(controller.proposeEssayGrade(prompt, ask("Your answer")).description());
        System.out.println("Review and approve this grade to save it.");
    }
    private void essayList() throws IOException {
        for (var item : controller.essayGrades(chooseReadableMaterial().id()))
            System.out.println(item.score() + "/100 " + item.feedback());
    }
    private void history() throws IOException {
        for (var entry : controller.history()) System.out.println(entry.time() + " " + entry.event()
                + " — " + entry.detail());
    }
    private void deleteHistory() throws IOException {
        var entries = controller.history();
        if (entries.isEmpty()) { System.out.println("No history records."); return; }
        for (int i = 0; i < entries.size(); i++)
            System.out.printf("%d. %s %s — %s%n", i + 1, entries.get(i).time(),
                    entries.get(i).event(), entries.get(i).detail());
        var selected = entries.get(number("Record number", entries.size()));
        if (ask("Delete this record? (y/N)").equalsIgnoreCase("y")) {
            controller.deleteHistory(selected);
            System.out.println("Record deleted.");
        }
    }
    private void selfAssess() throws IOException {
        Course course = chooseCourse();
        controller.saveSelfAssessment(course.id(), ask("Topic"), Integer.parseInt(ask("Confidence (1-5)")));
        System.out.println("Self-assessment saved.");
    }
    private void weakAdvice() throws IOException {
        System.out.println(controller.weakTopicAdvice(chooseCourse().id()));
    }

    private void showTree() {
        List<WorkspaceNode> roots = controller.workspace();
        if (roots.isEmpty()) System.out.println("No semesters yet.");
        for (WorkspaceNode root : roots) printNode(root, "");
    }

    private void printNode(WorkspaceNode node, String indent) {
        System.out.println(indent + "- " + node.label());
        for (WorkspaceNode child : node.children()) printNode(child, indent + "  ");
    }
}
