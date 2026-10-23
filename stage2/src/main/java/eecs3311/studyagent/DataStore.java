package eecs3311.studyagent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local persistence. Each entity has its own atomically replaced properties file. */
public final class DataStore {
    private final Path root;
    private final Map<UUID, Semester> semesters = new HashMap<>();
    private final Map<UUID, Course> courses = new HashMap<>();
    private final Map<UUID, Deadline> deadlines = new HashMap<>();
    private final Map<UUID, MaterialFile> materials = new HashMap<>();
    private final DocumentReaderFactory readers = new DocumentReaderFactory();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public DataStore(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        for (String directory : List.of("semesters", "courses", "deadlines", "materials", "files", "extracted")) {
            Files.createDirectories(this.root.resolve(directory));
        }
        load();
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        listeners.forEach(Runnable::run);
    }

    public synchronized List<Semester> semesters() {
        return semesters.values().stream().sorted(Comparator.comparing(Semester::startDate)
                .thenComparing(Semester::name)).toList();
    }

    public synchronized List<Course> courses() {
        return courses.values().stream().sorted(Comparator.comparing(Course::name)).toList();
    }

    public synchronized List<Deadline> deadlines() {
        return List.copyOf(deadlines.values());
    }

    public synchronized List<MaterialFile> materials() {
        return materials.values().stream().sorted(Comparator.comparing(MaterialFile::originalName)).toList();
    }

    public synchronized Semester semester(UUID id) {
        Semester item = semesters.get(id);
        if (item == null) throw new IllegalArgumentException("Unknown semester: " + id);
        return item;
    }

    public synchronized Course course(UUID id) {
        Course item = courses.get(id);
        if (item == null) throw new IllegalArgumentException("Unknown course: " + id);
        return item;
    }

    public synchronized Deadline deadline(UUID id) {
        Deadline item = deadlines.get(id);
        if (item == null) throw new IllegalArgumentException("Unknown deadline: " + id);
        return item;
    }

    public synchronized MaterialFile material(UUID id) {
        MaterialFile item = materials.get(id);
        if (item == null) throw new IllegalArgumentException("Unknown material: " + id);
        return item;
    }

    public synchronized Semester createSemester(String name, LocalDate start, LocalDate end) throws IOException {
        Semester item = new Semester(UUID.randomUUID(), name, start, end);
        if (semesters.values().stream().anyMatch(s -> s.name().equalsIgnoreCase(item.name()))) {
            throw new IllegalArgumentException("A semester with that name already exists.");
        }
        save("semesters", item.id(), props("name", item.name(), "start", start.toString(), "end", end.toString()));
        semesters.put(item.id(), item);
        changed();
        return item;
    }

    public synchronized Course createCourse(UUID semesterId, String name, String colorHex,
                                            Difficulty difficulty) throws IOException {
        semester(semesterId);
        Course item = new Course(UUID.randomUUID(), semesterId, name, colorHex, difficulty);
        if (courses.values().stream().anyMatch(c -> c.semesterId().equals(semesterId)
                && c.name().equalsIgnoreCase(item.name()))) {
            throw new IllegalArgumentException("A course with that name already exists in this semester.");
        }
        save("courses", item.id(), props("semester", semesterId.toString(), "name", item.name(),
                "color", item.colorHex(), "difficulty", difficulty.name()));
        courses.put(item.id(), item);
        changed();
        return item;
    }

    public synchronized Deadline createDeadline(UUID courseId, String title, String type,
                                                 LocalDate dueDate, String scope) throws IOException {
        Course course = course(courseId);
        validateDate(course, dueDate);
        Deadline item = new Deadline(UUID.randomUUID(), courseId, title, type, dueDate, scope, false);
        saveDeadline(item);
        deadlines.put(item.id(), item);
        changed();
        return item;
    }

    public synchronized Deadline editDeadline(UUID id, String title, String type,
                                               LocalDate dueDate, String scope) throws IOException {
        Deadline existing = deadline(id);
        validateDate(course(existing.courseId()), dueDate);
        Deadline updated = new Deadline(id, existing.courseId(), title, type, dueDate, scope, existing.completed());
        saveDeadline(updated);
        deadlines.put(id, updated);
        changed();
        return updated;
    }

    public synchronized Deadline setCompleted(UUID id, boolean completed) throws IOException {
        Deadline updated = deadline(id).withCompleted(completed);
        saveDeadline(updated);
        deadlines.put(id, updated);
        changed();
        return updated;
    }

    public synchronized void deleteDeadline(UUID id) throws IOException {
        deadline(id);
        Files.delete(root.resolve("deadlines").resolve(id + ".properties"));
        deadlines.remove(id);
        changed();
    }

    public synchronized MaterialFile uploadMaterial(UUID courseId, Category category, Path source)
            throws IOException {
        course(courseId);
        if (!Files.isRegularFile(source)) throw new IllegalArgumentException("File does not exist: " + source);
        String originalName = source.getFileName().toString();
        if (materials.values().stream().anyMatch(m -> m.courseId().equals(courseId)
                && m.category() == category && m.originalName().equalsIgnoreCase(originalName))) {
            throw new IllegalArgumentException("This category already contains a file named " + originalName);
        }
        UUID id = UUID.randomUUID();
        String storedName = id + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destination = root.resolve("files").resolve(storedName);
        Files.copy(source, destination);
        List<String> pages = List.of();
        String warning = "";
        var reader = readers.getReader(originalName);
        if (reader.isEmpty()) {
            warning = "Unsupported file type; stored without extracted text.";
        } else {
            try {
                pages = reader.get().extractText(destination);
                if (pages.stream().allMatch(String::isBlank)) {
                    warning = "No extractable text was found; this file is excluded from AI features.";
                }
            } catch (IOException ex) {
                warning = "Text extraction failed: " + ex.getMessage();
            }
        }
        boolean textAvailable = !pages.isEmpty() && pages.stream().anyMatch(page -> !page.isBlank());
        Path extractedPath = root.resolve("extracted").resolve(id + ".txt");
        MaterialFile item = new MaterialFile(id, courseId, category, originalName, storedName,
                textAvailable, warning, null);
        try {
            if (textAvailable) Files.writeString(extractedPath, String.join("\f", pages), StandardCharsets.UTF_8);
            saveMaterial(item);
        } catch (IOException ex) {
            Files.deleteIfExists(destination);
            Files.deleteIfExists(extractedPath);
            throw ex;
        }
        materials.put(id, item);
        changed();
        return item;
    }

    public synchronized List<String> materialPages(UUID id) throws IOException {
        MaterialFile material = material(id);
        if (!material.textAvailable()) return List.of();
        Path path = root.resolve("extracted").resolve(id + ".txt");
        return List.of(Files.readString(path, StandardCharsets.UTF_8).split("\\f", -1));
    }

    public synchronized MaterialFile assignLectureWeek(UUID id, Integer week) throws IOException {
        MaterialFile item = material(id).withLectureWeek(week);
        if (item.category() != Category.LECTURE_MATERIALS) {
            throw new IllegalArgumentException("Lecture weeks can only be assigned to lecture materials.");
        }
        saveMaterial(item);
        materials.put(id, item);
        changed();
        return item;
    }

    private void saveMaterial(MaterialFile item) throws IOException {
        save("materials", item.id(), props("course", item.courseId().toString(),
                "category", item.category().name(), "original", item.originalName(),
                "stored", item.storedName(), "textAvailable", Boolean.toString(item.textAvailable()),
                "warning", item.extractionWarning(),
                "week", item.lectureWeek() == null ? "" : item.lectureWeek().toString()));
    }

    public synchronized Path materialPath(UUID id) {
        Path path = root.resolve("files").resolve(material(id).storedName()).normalize();
        if (!path.startsWith(root.resolve("files"))) {
            throw new IllegalStateException("Stored material path is invalid.");
        }
        return path;
    }

    private void validateDate(Course course, LocalDate date) {
        if (date != null && !semester(course.semesterId()).contains(date)) {
            throw new IllegalArgumentException("Due date must fall within the course's semester.");
        }
    }

    private void saveDeadline(Deadline item) throws IOException {
        save("deadlines", item.id(), props("course", item.courseId().toString(), "title", item.title(),
                "type", item.type(), "due", item.dueDate() == null ? "" : item.dueDate().toString(),
                "scope", item.scope(), "completed", Boolean.toString(item.completed())));
    }

    private void save(String directory, UUID id, Properties values) throws IOException {
        Path folder = root.resolve(directory);
        Path target = folder.resolve(id + ".properties");
        Path temporary = Files.createTempFile(folder, id.toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "EECS3311 study agent");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Properties props(String... pairs) {
        Properties result = new Properties();
        for (int i = 0; i < pairs.length; i += 2) result.setProperty(pairs[i], pairs[i + 1]);
        return result;
    }

    private void load() throws IOException {
        for (Path file : propertyFiles("semesters")) {
            Properties p = read(file);
            UUID id = id(file);
            semesters.put(id, new Semester(id, required(p, "name"), LocalDate.parse(required(p, "start")),
                    LocalDate.parse(required(p, "end"))));
        }
        for (Path file : propertyFiles("courses")) {
            Properties p = read(file);
            UUID id = id(file);
            Course item = new Course(id, UUID.fromString(required(p, "semester")), required(p, "name"),
                    required(p, "color"), Difficulty.valueOf(required(p, "difficulty")));
            semester(item.semesterId());
            courses.put(id, item);
        }
        for (Path file : propertyFiles("deadlines")) {
            Properties p = read(file);
            UUID id = id(file);
            UUID courseId = UUID.fromString(required(p, "course"));
            String date = required(p, "due");
            Deadline item = new Deadline(id, courseId, required(p, "title"), required(p, "type"),
                    date.isBlank() ? null : LocalDate.parse(date), required(p, "scope"),
                    Boolean.parseBoolean(required(p, "completed")));
            validateDate(course(courseId), item.dueDate());
            deadlines.put(id, item);
        }
        for (Path file : propertyFiles("materials")) {
            Properties p = read(file);
            UUID id = id(file);
            String week = p.getProperty("week", "");
            MaterialFile item = new MaterialFile(id, UUID.fromString(required(p, "course")),
                    Category.valueOf(required(p, "category")), required(p, "original"), required(p, "stored"),
                    Boolean.parseBoolean(p.getProperty("textAvailable", "false")), p.getProperty("warning", ""),
                    week.isBlank() ? null : Integer.parseInt(week));
            course(item.courseId());
            materials.put(id, item);
        }
    }

    private List<Path> propertyFiles(String directory) throws IOException {
        try (var files = Files.list(root.resolve(directory))) {
            return files.filter(p -> p.getFileName().toString().endsWith(".properties")).toList();
        }
    }

    private static Properties read(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        return p;
    }

    private static UUID id(Path file) {
        return UUID.fromString(file.getFileName().toString().replace(".properties", ""));
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null) throw new IllegalStateException("Missing " + key + " in saved data.");
        return value;
    }
}
