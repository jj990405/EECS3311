package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        System.out.println("EECS3311 Study Agent - type help for commands.");
        while (true) {
            System.out.print("study> ");
            if (!input.hasNextLine()) return;
            String command = input.nextLine().trim().toLowerCase();
            try {
                switch (command) {
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
        System.out.println("semester add | course add | material add | deadline add | deadline edit");
        System.out.println("deadline delete | timeline | calendar | reminders | complete | progress | tree | exit");
        System.out.println("ai syllabus | ai week | proposals | approve | reject");
        System.out.println("Dates use YYYY-MM-DD. Leave a deadline date blank for TBD.");
    }

    private String ask(String label) {
        System.out.print(label + ": ");
        return input.nextLine().trim();
    }

    private LocalDate date(String label, boolean optional) {
        String value = ask(label + " (YYYY-MM-DD" + (optional ? ", blank for TBD" : "") + ")");
        if (optional && value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Use a valid ISO date, e.g. 2026-11-10.");
        }
    }

    private int number(String label, int max) {
        int value;
        try {
            value = Integer.parseInt(ask(label));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a number from 1 to " + max + ".");
        }
        if (value < 1 || value > max) throw new IllegalArgumentException("Choose 1 to " + max + ".");
        return value - 1;
    }

    private Semester chooseSemester() {
        List<Semester> items = controller.semesters();
        if (items.isEmpty()) throw new IllegalStateException("Create a semester first.");
        for (int i = 0; i < items.size(); i++) {
            Semester s = items.get(i);
            System.out.printf("%d. %s (%s to %s)%n", i + 1, s.name(), s.startDate(), s.endDate());
        }
        return items.get(number("Semester number", items.size()));
    }

    private Course chooseCourse() {
        List<Course> items = controller.courses();
        if (items.isEmpty()) throw new IllegalStateException("Create a course first.");
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
        String color = ask("Course color (#RRGGBB)");
        Difficulty difficulty = Difficulty.valueOf(ask("Difficulty (HIGH/MEDIUM/LOW)").toUpperCase());
        Course course = controller.createCourse(semester.id(), name, color, difficulty);
        System.out.println("Created course: " + course.name());
    }

    private void addMaterial() throws IOException {
        Course course = chooseCourse();
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) System.out.printf("%d. %s%n", i + 1, categories[i]);
        Category category = categories[number("Category number", categories.length)];
        MaterialFile material = controller.uploadMaterial(course.id(), category, Path.of(ask("File path")));
        System.out.println("Stored: " + material.originalName());
        if (!material.extractionWarning().isBlank()) System.out.println(material.extractionWarning());
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
            System.out.printf("  %s | %s | %s%s%n", controller.course(d.courseId()).name(), d.type(),
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
