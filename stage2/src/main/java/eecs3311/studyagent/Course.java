package eecs3311.studyagent;

import java.util.Objects;
import java.util.UUID;

public record Course(UUID id, UUID semesterId, String name, String colorHex, Difficulty difficulty) {
    public Course {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(semesterId, "semesterId");
        Objects.requireNonNull(difficulty, "difficulty");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Course name is required.");
        }
        if (colorHex == null || !colorHex.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Course color must be a six-digit hex value such as #3366CC.");
        }
        name = name.trim();
        colorHex = colorHex.toUpperCase();
    }

    @Override
    public String toString() {
        return name + " (" + difficulty + ")";
    }
}
