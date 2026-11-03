package eecs3311.studyagent;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Semester(UUID id, String name, LocalDate startDate, LocalDate endDate) {
    public Semester {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Semester name is required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Semester end date must follow its start date.");
        }
        name = name.trim();
    }

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public String toString() {
        return name + " (" + startDate + " to " + endDate + ")";
    }
}
