package eecs3311.studyagent;

/** Adjusts explanation depth to the course difficulty while keeping source requirements fixed. */
public interface ExplanationStrategy {
    String instruction();

    static ExplanationStrategy forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case HIGH -> () -> "Explain each step carefully and define difficult terms.";
            case MEDIUM -> () -> "Explain the main steps and give one concrete example.";
            case LOW -> () -> "Answer briefly with the essential idea.";
        };
    }
}
