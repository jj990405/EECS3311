package eecs3311.studyagent;

public enum Category {
    SYLLABUS("Syllabus"),
    LECTURE_MATERIALS("Lecture Materials"),
    ASSIGNMENT_SPECIFICATIONS("Assignment Specifications"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
