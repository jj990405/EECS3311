package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class CalendarUsabilityTest {
    @Test void everyWeekShowsCourseAndTitleAndEmptyDayOpensEditor() throws Exception {
        UUID courseId = UUID.randomUUID();
        Course course = new Course(courseId, UUID.randomUUID(), "EECS2021", "#3366CC", Difficulty.MEDIUM);
        Deadline later = new Deadline(UUID.randomUUID(), courseId, "Quiz 3", "Quiz",
                LocalDate.of(2026, 11, 18), "", false);
        AtomicReference<LocalDate> selectedDay = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            CalendarPanel panel = new CalendarPanel(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 3),
                    List.of(later), Map.of(courseId, course), deadline -> { }, selectedDay::set);
            assertTrue(buttons(panel).stream().anyMatch(button -> "[EECS2021] Quiz 3"
                            .equals(button.getAccessibleContext().getAccessibleName())),
                    "A deadline in any week should have a readable label");
            buttons(panel).stream().filter(button -> button.getText().equals("5"))
                    .findFirst().orElseThrow().doClick();
            assertEquals(LocalDate.of(2026, 11, 5), selectedDay.get());
        });
    }

    @Test void oldParagraphSummariesRemainReadableInNotesTab() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StudyNotesPanel panel = new StudyNotesPanel("lecture.pdf",
                    "Page 2\nDesign patterns define reusable solutions. A Factory creates objects.\n"
                            + "Sources: lecture.pdf p.2", () -> { });
            JTextPane text = descendants(panel).stream().filter(JTextPane.class::isInstance)
                    .map(JTextPane.class::cast).findFirst().orElseThrow();
            String visible = text.getText();
            assertTrue(visible.contains("Page 2"));
            assertTrue(visible.contains("• Design patterns define reusable solutions."));
            assertTrue(visible.contains("• A Factory creates objects."));
            assertFalse(visible.contains("solutions. A Factory"));
            assertTrue(visible.contains("Sources: lecture.pdf p.2"));
        });
    }

    private static List<JButton> buttons(Component parent) {
        return descendants(parent).stream().filter(JButton.class::isInstance)
                .map(JButton.class::cast).toList();
    }

    private static List<Component> descendants(Component parent) {
        java.util.ArrayList<Component> all = new java.util.ArrayList<>();
        all.add(parent);
        if (parent instanceof Container container)
            for (Component child : container.getComponents()) all.addAll(descendants(child));
        return all;
    }
}
