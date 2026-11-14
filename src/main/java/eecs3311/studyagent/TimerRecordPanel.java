package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Daily study sessions and weekly total with a selectable week. */
public final class TimerRecordPanel extends JPanel {
    private final AgentController controller;
    private final JLabel weekLabel = new JLabel();
    private final JTextArea records = new JTextArea();
    private LocalDate monday;

    public TimerRecordPanel(AgentController controller) {
        super(new BorderLayout(0, 12));
        this.controller = controller;
        monday = startOfWeek(LocalDate.now());
        setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel navigation = new JPanel(new GridLayout(1, 3, 8, 0));
        button(navigation, "Previous week", () -> changeWeek(-1));
        button(navigation, "This week", () -> { monday = startOfWeek(LocalDate.now()); refresh(); });
        button(navigation, "Next week", () -> changeWeek(1));
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.add(weekLabel, BorderLayout.NORTH);
        header.add(navigation, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        records.setEditable(false);
        add(new JScrollPane(records), BorderLayout.CENTER);
        refresh();
    }

    private LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void button(JPanel parent, String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        parent.add(button);
    }

    private void changeWeek(int offset) { monday = monday.plusWeeks(offset); refresh(); }

    public void refresh() {
        weekLabel.setText("Week of " + monday);
        StringBuilder text = new StringBuilder();
        long total = 0;
        java.util.Map<LocalDate, Long> daily = new java.util.TreeMap<>();
        for (StudySession session : controller.studySessions()) {
            LocalDate day = session.startedAt().toLocalDate();
            if (day.isBefore(monday) || !day.isBefore(monday.plusDays(7))) continue;
            daily.merge(day, session.seconds(), Long::sum);
            total += session.seconds();
        }
        daily.forEach((day, seconds) -> text.append(day).append(" · ")
                .append(seconds / 60).append(" min ").append(seconds % 60).append(" sec\n"));
        if (text.length() == 0) text.append("No records for this week.\n");
        text.append("\nWeekly total: ").append(total / 60).append(" min ")
                .append(total % 60).append(" sec");
        records.setText(text.toString());
    }
}
