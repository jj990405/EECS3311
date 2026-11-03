package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Month grid with course colours and a selectable expanded week, as in SD03. */
public final class CalendarPanel extends JPanel {
    private final YearMonth month;
    private final LocalDate today;
    private final List<Deadline> deadlines;
    private final Map<UUID, Course> courses;
    private final Consumer<Deadline> onDeadline;
    private LocalDate expandedMonday;

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines) {
        this(month, today, deadlines, Map.of(), deadline -> { });
    }

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines,
                         Map<UUID, Course> courses) {
        this(month, today, deadlines, courses, deadline -> { });
    }

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines,
                         Map<UUID, Course> courses, Consumer<Deadline> onDeadline) {
        super(new BorderLayout(8, 8));
        this.month = month;
        this.today = today;
        this.deadlines = List.copyOf(deadlines);
        this.courses = Map.copyOf(courses);
        this.onDeadline = onDeadline;
        LocalDate focus = YearMonth.from(today).equals(month) ? today : month.atDay(1);
        expandedMonday = monday(focus);
        setPreferredSize(new Dimension(740, 510));
        render();
    }

    private static LocalDate monday(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    private Color courseColor(Deadline deadline) {
        Course course = courses.get(deadline.courseId());
        return course == null ? new Color(54, 99, 156) : Color.decode(course.colorHex());
    }

    private void render() {
        removeAll();
        add(new JLabel(month.toString(), JLabel.CENTER), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
        for (String day : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            grid.add(new JLabel(day, JLabel.CENTER));
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) grid.add(new JPanel());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            List<Deadline> items = deadlines.stream().filter(d -> date.equals(d.dueDate())).toList();
            JPanel cell = new JPanel(new BorderLayout(2, 2));
            boolean expanded = monday(date).equals(expandedMonday);
            cell.setBorder(BorderFactory.createLineBorder(date.equals(today) ? new Color(28, 93, 163)
                    : expanded ? new Color(147, 178, 211) : Color.LIGHT_GRAY));
            JButton dateButton = new JButton(Integer.toString(day));
            dateButton.setToolTipText("Show week of " + monday(date));
            dateButton.addActionListener(event -> { expandedMonday = monday(date); render(); });
            cell.add(dateButton, BorderLayout.NORTH);
            JPanel entries = new JPanel();
            entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
            int visible = expanded ? 4 : 6;
            for (int i = 0; i < Math.min(items.size(), visible); i++) {
                Deadline item = items.get(i);
                Course course = courses.get(item.courseId());
                String detail = (course == null ? "" : course.name() + " · ") + item.title();
                if (expanded) {
                    JButton entry = new JButton(item.title());
                    entry.setHorizontalAlignment(JButton.LEFT);
                    entry.setMargin(new java.awt.Insets(1, 3, 1, 3));
                    entry.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, courseColor(item)),
                            BorderFactory.createEmptyBorder(2, 2, 2, 2)));
                    entry.setToolTipText(detail);
                    entry.addActionListener(event -> onDeadline.accept(item));
                    entries.add(entry);
                } else {
                    JLabel dot = new JLabel("●");
                    dot.setForeground(courseColor(item));
                    dot.setToolTipText(detail);
                    entries.add(dot);
                }
            }
            if (items.size() > visible) entries.add(new JLabel("+" + (items.size() - visible) + " more"));
            cell.add(entries, BorderLayout.CENTER);
            grid.add(cell);
        }
        add(new JScrollPane(grid), BorderLayout.CENTER);
        long count = deadlines.stream().filter(d -> d.dueDate() != null
                && !d.dueDate().isBefore(expandedMonday)
                && d.dueDate().isBefore(expandedMonday.plusDays(7))).count();
        add(new JLabel("Week of " + expandedMonday + " · " + count + " deadline"
                + (count == 1 ? "" : "s") + " · Click a date to expand its week"), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }
}
