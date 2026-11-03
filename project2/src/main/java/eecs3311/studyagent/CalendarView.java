package eecs3311.studyagent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Text monthly calendar, shared by GUI dialog and CLI. */
public final class CalendarView {
    public String render(YearMonth month, List<Deadline> deadlines) {
        StringBuilder output = new StringBuilder(month + "\n Mo Tu We Th Fr Sa Su\n");
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        output.append("   ".repeat(offset));
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            boolean hasDeadline = deadlines.stream().anyMatch(d -> date.equals(d.dueDate()));
            output.append(String.format("%2d%s", day, hasDeadline ? "*" : " "));
            if (date.getDayOfWeek().getValue() == 7) output.append('\n');
        }
        output.append("\n* deadline\n");
        for (Deadline deadline : deadlines) {
            if (deadline.dueDate() != null && YearMonth.from(deadline.dueDate()).equals(month))
                output.append(deadline.dueDate()).append("  ").append(deadline.title())
                        .append(deadline.completed() ? " [done]" : "").append('\n');
        }
        return output.toString();
    }
}
