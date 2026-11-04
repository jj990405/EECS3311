package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UndatedReminderTest {
    @TempDir Path root;

    @Test void remindersAppearOnceEachDayUntilDateIsSetOrAssessmentIsDone() throws Exception {
        Path data = root.resolve("data");
        AgentController agent = new AgentController(data, (system, user) -> "{}");
        Semester semester = agent.createSemester("Fall", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31));
        Course course = agent.createCourse(semester.id(), "EECS3311", "#3366CC", Difficulty.MEDIUM);
        Deadline exam = agent.addDeadline(course.id(), "Final Exam", "Exam", null, "Final topics");
        Deadline project = agent.addDeadline(course.id(), "Project", "Assignment", null, "Stage 3");
        LocalDate today = LocalDate.of(2026, 11, 3);
        assertEquals(2, agent.unseenUndatedReminders(today).size());
        agent.markUndatedRemindersShown(List.of(exam, project), today);
        assertEquals(0, agent.unseenUndatedReminders(today).size());

        AgentController reopened = new AgentController(data, (system, user) -> "{}");
        assertEquals(0, reopened.unseenUndatedReminders(today).size());
        assertEquals(2, reopened.unseenUndatedReminders(today.plusDays(1)).size());
        reopened.editDeadline(exam.id(), exam.title(), exam.type(), LocalDate.of(2026, 12, 10), exam.scope());
        assertEquals(1, reopened.unseenUndatedReminders(today.plusDays(1)).size());
        reopened.setCompleted(project.id(), true);
        assertEquals(0, reopened.unseenUndatedReminders(today.plusDays(1)).size());
    }
}
