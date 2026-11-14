package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

class AiInteractionTest {
    @Test void explicitChatCommandsOpenToolsWithoutMisroutingStudyQuestions() {
        assertEquals(AiFeedback.Action.QUIZ, AiFeedback.action("Quiz!"));
        assertEquals(AiFeedback.Action.QUIZ, AiFeedback.action("퀴즈 내줘"));
        assertEquals(AiFeedback.Action.SLIDE_QUIZ, AiFeedback.action("slide quiz"));
        assertEquals(AiFeedback.Action.NOTES, AiFeedback.action("Study notes"));
        for (String question : java.util.List.of("Explain Quiz 3's scope", "What is a quiz?", "Explain the Command pattern"))
            assertEquals(AiFeedback.Action.QUESTION, AiFeedback.action(question));
    }

    @Test void longResultsStartLandscapeInsideTheUsableMonitorIncludingSecondaryAndSmallScreens() {
        for (Rectangle monitor : java.util.List.of(new Rectangle(0, 0, 1920, 1040),
                new Rectangle(-1280, 40, 1280, 680), new Rectangle(0, 0, 800, 560),
                new Rectangle(0, 0, 600, 900))) {
            Rectangle result = ResultWindow.initialBounds(monitor);
            assertTrue(monitor.contains(result), result.toString());
            assertTrue(result.width > result.height);
            assertTrue(result.height <= 700);
        }
    }
}
