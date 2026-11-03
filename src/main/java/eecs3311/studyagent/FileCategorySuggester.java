package eecs3311.studyagent;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Conservative, offline hints for obvious upload-folder mistakes. */
final class FileCategorySuggester {
    private static final Pattern NUMBERED_LECTURE = Pattern.compile(
            "(?i)^[a-z]{2,}[a-z0-9]*[-_ ]\\d{1,2}[-_ ]\\s*\\S+.*");
    private static final Pattern LECTURE = Pattern.compile(
            "(?i).*(lecture|slides?|tutorial|chapter|module|week[ _-]?\\d+|"
            + "design[ _-]?patterns|oop[ _-]?design|java[ _-]?sdk).*", Pattern.DOTALL);
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i).*(assignment|homework|project[ _-]?\\d+|project[ _-]?instruction|"
            + "stage[ _-]?[12](?:[ _-]|\\.|\\(|$)|rubric|deliverable|specification).*",
            Pattern.DOTALL);

    private FileCategorySuggester() { }

    static Optional<Category> suggest(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        if (name.contains("syllabus") || name.contains("course outline"))
            return Optional.of(Category.SYLLABUS);
        if (ASSIGNMENT.matcher(name).matches()) return Optional.of(Category.ASSIGNMENT_SPECIFICATIONS);
        if (LECTURE.matcher(name).matches() || NUMBERED_LECTURE.matcher(name).matches())
            return Optional.of(Category.LECTURE_MATERIALS);
        return Optional.empty();
    }

    static Category defaultDestination(String filename, Category selectedFolder) {
        return suggest(filename).orElse(selectedFolder == null ? Category.OTHER : selectedFolder);
    }
}
