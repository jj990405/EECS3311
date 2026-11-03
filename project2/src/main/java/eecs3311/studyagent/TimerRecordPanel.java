package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(navigation, "Previous week", () -> changeWeek(-1));
        button(navigation, "This week", () -> { monday = startOfWeek(LocalDate.now()); refresh(); });
        button(navigation, "Next week", () -> changeWeek(1));
        navigation.add(weekLabel);
        add(navigation, BorderLayout.NORTH);
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
        for (StudySession session : controller.studySessions()) {
            LocalDate day = session.startedAt().toLocalDate();
            if (day.isBefore(monday) || !day.isBefore(monday.plusDays(7))) continue;
            text.append(day).append(" · ").append(session.seconds() / 60).append(" min\n");
            total += session.seconds();
        }
        if (text.length() == 0) text.append("No records for this week.\n");
        text.append("\nWeekly total: ").append(total / 60).append(" min");
        records.setText(text.toString());
    }
}
