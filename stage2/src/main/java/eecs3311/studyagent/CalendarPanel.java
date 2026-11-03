package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Month grid with the current week expanded below it, as in SD03. */
public final class CalendarPanel extends JPanel {
    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines) {
        super(new BorderLayout(8, 8));
        setPreferredSize(new Dimension(900, 620));
        add(new JLabel(month.toString(), JLabel.CENTER), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
        for (String day : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            grid.add(new JLabel(day, JLabel.CENTER));
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) grid.add(new JPanel());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createLineBorder(date.equals(today) ? Color.BLUE : Color.LIGHT_GRAY));
            cell.add(new JLabel(Integer.toString(day)), BorderLayout.NORTH);
            String labels = deadlines.stream().filter(d -> date.equals(d.dueDate()))
                    .map(d -> d.title() + (d.completed() ? " ✓" : ""))
                    .reduce((a, b) -> a + "\n" + b).orElse("");
            JTextArea text = new JTextArea(labels);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            cell.add(text, BorderLayout.CENTER);
            grid.add(cell);
        }
        add(new JScrollPane(grid), BorderLayout.CENTER);
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        StringBuilder week = new StringBuilder("Current week: ");
        for (Deadline d : deadlines) if (d.dueDate() != null && !d.dueDate().isBefore(monday)
                && !d.dueDate().isAfter(monday.plusDays(6)))
            week.append(d.dueDate()).append(" ").append(d.title()).append("; ");
        add(new JLabel(week.toString()), BorderLayout.SOUTH);
    }
}
