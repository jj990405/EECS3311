package eecs3311.studyagent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public record Deadline(UUID id, UUID courseId, String title, String type, LocalDate dueDate,
                       String scope, boolean completed, LocalTime dueTime, int notifyBeforeMinutes,
                       Double weight, String sourceText) {
    public Deadline(UUID id, UUID courseId, String title, String type, LocalDate dueDate,
                    String scope, boolean completed) {
        this(id, courseId, title, type, dueDate, scope, completed, LocalTime.of(23, 59), 1440, null, "");
    }
    public Deadline(UUID id, UUID courseId, String title, String type, LocalDate dueDate,
                    String scope, boolean completed, LocalTime dueTime, int notifyBeforeMinutes) {
        this(id, courseId, title, type, dueDate, scope, completed, dueTime, notifyBeforeMinutes, null, "");
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
        sourceText = sourceText == null ? "" : sourceText.trim();
        if (weight != null && (weight < 0 || weight > 100 || !Double.isFinite(weight)))
            throw new IllegalArgumentException("Deadline weight must be 0-100%.");
        Objects.requireNonNull(dueTime, "dueTime");
        if (notifyBeforeMinutes < 0) throw new IllegalArgumentException("Notification lead time must be nonnegative.");
    }

    public Deadline withCompleted(boolean value) {
        return new Deadline(id, courseId, title, type, dueDate, scope, value, dueTime, notifyBeforeMinutes,
                weight, sourceText);
    }

    public Deadline withNotification(LocalTime time, int minutesBefore) {
        return new Deadline(id, courseId, title, type, dueDate, scope, completed, time, minutesBefore,
                weight, sourceText);
    }
}
