package eecs3311.studyagent;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Deadline(UUID id, UUID courseId, String title, String type, LocalDate dueDate,
                       String scope, boolean completed) {
    public Deadline {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(courseId, "courseId");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Deadline title is required.");
        }
        title = title.trim();
        type = type == null ? "" : type.trim();
        scope = scope == null ? "" : scope.trim();
    }

    public Deadline withCompleted(boolean value) {
        return new Deadline(id, courseId, title, type, dueDate, scope, value);
    }
}
