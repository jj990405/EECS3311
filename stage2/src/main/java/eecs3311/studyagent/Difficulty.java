package eecs3311.studyagent;

public enum Difficulty {
    HIGH, MEDIUM, LOW;

    public int priority() {
        return ordinal();
    }
}
