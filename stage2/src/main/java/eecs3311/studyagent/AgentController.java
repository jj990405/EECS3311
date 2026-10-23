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

    public AgentController(Path dataDirectory) throws IOException {
        this.store = new DataStore(dataDirectory);
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
}
