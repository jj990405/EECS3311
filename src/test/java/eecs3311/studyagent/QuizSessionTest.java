package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class QuizSessionTest {
    private StudyArtifacts.Quiz quiz() {
        var q = new StudyArtifacts.Question("SOLID", "Which principle applies?", List.of("Single Responsibility", "Open Closed", "Observer", "Command"), 1, "Open Closed allows extension.");
        return new StudyArtifacts.Quiz(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(q, q));
    }
    @Test void navigatingPreservesAnswersAndSubmissionRequiresEveryAnswer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger submissions = new AtomicInteger();
            QuizSessionPanel panel = new QuizSessionPanel(quiz(), null, submissions::incrementAndGet, () -> {});
            JList<?> list = find(panel, JList.class);
            list.setSelectedIndex(1);
            assertFalse(button(panel, "Submit quiz").isEnabled());
            panel.getActionMap().get("answer1").actionPerformed(null);
            assertFalse(panel.allAnswered()); list.setSelectedIndex(0);
            panel.getActionMap().get("answer2").actionPerformed(null);
            assertTrue(panel.allAnswered()); assertEquals(List.of(2, 1), panel.answers());
            list.setSelectedIndex(1); assertTrue(button(panel, "Submit quiz").isEnabled());
            button(panel, "Submit quiz").doClick(); assertEquals(1, submissions.get());
        });
    }
    @Test void reviewShowsWrongAndCorrectAnswersWithoutAllowingEdits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            QuizSessionPanel panel = new QuizSessionPanel(quiz(), List.of(0, 1), () -> {}, () -> {});
            assertTrue(components(panel).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> "Correct answer".equals(label.getText())));
            assertTrue(components(panel).stream().filter(JRadioButton.class::isInstance).noneMatch(Component::isEnabled));
            panel.getActionMap().get("answer3").actionPerformed(null); assertEquals(List.of(0, 1), panel.answers());
            find(panel, JList.class).setSelectedIndex(1);
            assertTrue(components(panel).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> "Your answer · Correct".equals(label.getText())));
        });
    }
    private static JButton button(Container c, String text) { return components(c).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> text.equals(b.getText())).findFirst().orElseThrow(); }
    private static <T> T find(Container c, Class<T> type) { return components(c).stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(); }
    private static java.util.List<Component> components(Component c) { var result = new java.util.ArrayList<Component>(); result.add(c); if(c instanceof Container parent) for(Component child:parent.getComponents())result.addAll(components(child));return result; }
}
