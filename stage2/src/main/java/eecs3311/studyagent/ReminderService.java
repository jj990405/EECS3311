package eecs3311.studyagent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Computes due-soon reminders from current persisted deadlines. */
public final class ReminderService {
    public List<Deadline> upcoming(List<Deadline> deadlines, LocalDate today, int daysAhead) {
        if (daysAhead < 0) throw new IllegalArgumentException("Days ahead must be nonnegative");
        return deadlines.stream().filter(d -> !d.completed() && d.dueDate() != null
                && !d.dueDate().isBefore(today) && !d.dueDate().isAfter(today.plusDays(daysAhead))).toList();
    }
    public List<Deadline> dueNotifications(List<Deadline> deadlines, LocalDateTime now) {
        return deadlines.stream().filter(d -> {
            if (d.completed() || d.dueDate() == null) return false;
            LocalDateTime due = d.dueDate().atTime(d.dueTime());
            return !now.isBefore(due.minusMinutes(d.notifyBeforeMinutes())) && !now.isAfter(due);
        }).toList();
    }
}
