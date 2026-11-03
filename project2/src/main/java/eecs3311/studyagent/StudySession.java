package eecs3311.studyagent;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudySession(UUID id, UUID courseId, String topic, LocalDateTime startedAt, long seconds) {
    public StudySession {
        if (topic == null || topic.isBlank() || seconds < 0) throw new IllegalArgumentException("Invalid study session");
    }
}
