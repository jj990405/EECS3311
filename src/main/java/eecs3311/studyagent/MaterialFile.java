package eecs3311.studyagent;

import java.util.Objects;
import java.util.UUID;

public record MaterialFile(UUID id, UUID courseId, Category category, String originalName,
                           String storedName, boolean textAvailable, String extractionWarning, Integer lectureWeek) {
    public MaterialFile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(category, "category");
        if (originalName == null || originalName.isBlank() || storedName == null || storedName.isBlank()) {
            throw new IllegalArgumentException("Material file names are required.");
        }
        if (lectureWeek != null && lectureWeek < 1) {
            throw new IllegalArgumentException("Lecture week must be positive.");
        }
        extractionWarning = extractionWarning == null ? "" : extractionWarning;
    }

    public MaterialFile withLectureWeek(Integer week) {
        return new MaterialFile(id, courseId, category, originalName, storedName,
                textAvailable, extractionWarning, week);
    }

    public MaterialFile withCategory(Category target) {
        return new MaterialFile(id, courseId, target, originalName, storedName,
                textAvailable, extractionWarning,
                target == Category.LECTURE_MATERIALS ? lectureWeek : null);
    }
}
