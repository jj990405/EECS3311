package eecs3311.studyagent;

import java.util.Objects;
import java.util.UUID;

public record MaterialFile(UUID id, UUID courseId, Category category, String originalName,
                           String storedName) {
    public MaterialFile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(category, "category");
        if (originalName == null || originalName.isBlank() || storedName == null || storedName.isBlank()) {
            throw new IllegalArgumentException("Material file names are required.");
        }
    }
}
