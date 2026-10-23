package eecs3311.studyagent;

import java.time.LocalDate;

/** Rejects model dates outside the course semester and empty or unsupported suggestions. */
public final class DeadlineValidator {
    public void validate(ResponseParser.DeadlineDraft draft, Semester semester) {
        if (draft.title().isBlank() || draft.type().isBlank() || draft.evidence().isBlank())
            throw new IllegalArgumentException("AI deadline is missing title, type, or source evidence.");
        LocalDate date = draft.dueDate();
        if (date != null && !semester.contains(date))
            throw new IllegalArgumentException("Suggested date is outside the semester: " + date);
    }
}
