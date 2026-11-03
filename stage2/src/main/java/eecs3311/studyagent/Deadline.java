package eecs3311.studyagent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public record Deadline(UUID id, UUID courseId, String title, String type, LocalDate dueDate,
                       String scope, boolean completed, LocalTime dueTime, int notifyBeforeMinutes) {
    public Deadline(UUID id, UUID courseId, String title, String type, LocalDate dueDate,
                    String scope, boolean completed) {
        this(id, courseId, title, type, dueDate, scope, completed, LocalTime.of(23, 59), 1440);
    }
    public Deadline {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(courseId, "courseId");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Deadline title is required.");
        }
        title = title.trim();
        type = type == null ? "" : type.trim();
        scope = scope == null ? "" : scope.trim();
        Objects.requireNonNull(dueTime, "dueTime");
        if (notifyBeforeMinutes < 0) throw new IllegalArgumentException("Notification lead time must be nonnegative.");
    }

    public Deadline withCompleted(boolean value) {
        return new Deadline(id, courseId, title, type, dueDate, scope, value, dueTime, notifyBeforeMinutes);
    }

    public Deadline withNotification(LocalTime time, int minutesBefore) {
        return new Deadline(id, courseId, title, type, dueDate, scope, completed, time, minutesBefore);
    }
}
