package eecs3311.studyagent;

import java.util.List;

public record GroundedAnswer(String answer, List<Citation> citations) {
    public record Citation(String fileName, int page) { }
    public GroundedAnswer {
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("Answer is empty");
        citations = List.copyOf(citations);
    }
}
