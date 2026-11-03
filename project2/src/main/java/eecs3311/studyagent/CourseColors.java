package eecs3311.studyagent;

/** Named course colours keep colour codes out of the setup flow. */
final class CourseColors {
    record Choice(String name, String hex) { }

    static final Choice[] PALETTE = {
        new Choice("Red", "#D95763"), new Choice("Orange", "#EA8C42"),
        new Choice("Yellow", "#D2AC29"), new Choice("Green", "#48A87C"),
        new Choice("Blue", "#3B7DC2"), new Choice("Indigo", "#625DB5"),
        new Choice("Violet", "#9564A8")
    };

    private CourseColors() { }

    static Choice suggested(int courseCount) {
        return PALETTE[Math.floorMod(courseCount, PALETTE.length)];
    }
}
