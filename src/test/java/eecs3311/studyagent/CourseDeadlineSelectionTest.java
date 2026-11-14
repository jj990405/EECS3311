package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CourseDeadlineSelectionTest {
    @TempDir Path root;

    @Test void clickingTheTitleSelectsAndOpensThatDeadlineAndKeyboardKeepsTheTarget() throws Exception {
        var agent = new AgentController(root, (s,u) -> "{}");
        var semester = agent.createSemester("Fall", LocalDate.of(2026,9,1), LocalDate.of(2026,12,31));
        var course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        var first = agent.addDeadline(course.id(), "First", "Quiz", LocalDate.of(2026,11,14), "");
        var second = agent.addDeadline(course.id(), "Second", "Lab", LocalDate.of(2026,11,15), "");
        var opened = new AtomicReference<Deadline>();
        SwingUtilities.invokeAndWait(() -> {
            var page = new CoursePage(agent, ()->{}, ()->{}, ()->{}, ()->{}, ()->{});
            page.setDeadlineActions(opened::set, d -> new JPopupMenu());
            page.showCourse(course);
            JTextArea text = find(page, "Second");
            assertNotNull(text);
            text.dispatchEvent(new MouseEvent(text, MouseEvent.MOUSE_PRESSED, 1, 0, 4, 4, 1, false, MouseEvent.BUTTON1));
            text.dispatchEvent(new MouseEvent(text, MouseEvent.MOUSE_CLICKED, 2, 0, 4, 4, 1, false, MouseEvent.BUTTON1));
            assertEquals(second.id(), page.selectedDeadlineId());
            assertEquals(second, opened.get());
            assertNotEquals(first.id(), opened.get().id());
            JPanel card = (JPanel)text.getParent();
            assertEquals(AppTheme.BLUE_TINT, text.getBackground());
            card.getActionMap().get("openDeadline").actionPerformed(new java.awt.event.ActionEvent(card,0,"open"));
            assertEquals(second, opened.get());
            assertNotNull(card.getActionMap().get("deadlineMenu"));
        });
    }

    @Test void changingCourseOrRemovingTheCardDoesNotKeepAnInvisibleDeadlineSelection() throws Exception {
        var agent = new AgentController(root, (s,u) -> "{}");
        var semester = agent.createSemester("Fall", LocalDate.of(2026,9,1), LocalDate.of(2026,12,31));
        var first = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        var second = agent.createCourse(semester.id(), "EECS2030", "#8877AA", Difficulty.LOW);
        var deadline = agent.addDeadline(first.id(), "Only first course", "Quiz", LocalDate.of(2026,11,14), "");
        SwingUtilities.invokeAndWait(() -> {
            var page = new CoursePage(agent, ()->{}, ()->{}, ()->{}, ()->{}, ()->{});
            page.showCourse(first);
            var text=find(page,"Only first course");
            text.dispatchEvent(new MouseEvent(text, MouseEvent.MOUSE_PRESSED, 1, 0, 4, 4, 1, false, MouseEvent.BUTTON1));
            assertEquals(deadline.id(), page.selectedDeadlineId());
            page.showCourse(first);
            assertEquals(deadline.id(), page.selectedDeadlineId());
            page.showCourse(second);
            assertNull(page.selectedDeadlineId());
            page.clear();
            assertNull(page.selectedDeadlineId());
        });
    }

    private JTextArea find(Container parent,String title) {
        for (var child:parent.getComponents()) {
            if (child instanceof JTextArea text && text.getText().equals(title)) return text;
            if (child instanceof Container container) { var found=find(container,title); if(found!=null)return found; }
        }
        return null;
    }
}
