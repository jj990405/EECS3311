package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
        setBackground(AppTheme.BACKGROUND);
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
        JLabel monthHeading = new JLabel(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthHeading.setFont(AppTheme.SECTION);
        monthHeading.setForeground(AppTheme.TEXT);
        monthHeading.setBorder(AppTheme.padding(12, 14, 8, 14));
        add(monthHeading, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
        grid.setBackground(AppTheme.BACKGROUND);
        for (String day : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
            JLabel dayHeading = new JLabel(day, JLabel.CENTER);
            dayHeading.setForeground(AppTheme.MUTED);
            dayHeading.setFont(AppTheme.SMALL);
            grid.add(dayHeading);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) {
            JPanel blank = new JPanel();
            blank.setBackground(AppTheme.BACKGROUND);
            grid.add(blank);
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            List<Deadline> items = deadlines.stream().filter(d -> date.equals(d.dueDate())).toList();
            JPanel cell = new JPanel(new BorderLayout(2, 2));
            boolean expanded = monday(date).equals(expandedMonday);
            cell.setBackground(expanded ? AppTheme.RAISED : AppTheme.SURFACE);
            cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(date.equals(today) ? AppTheme.BLUE : AppTheme.BORDER),
                    AppTheme.padding(5, 6, 5, 6)));
            JButton dateButton = new JButton(Integer.toString(day));
            dateButton.setToolTipText("Show week of " + monday(date));
            dateButton.addActionListener(event -> { expandedMonday = monday(date); render(); });
            dateButton.setFont(AppTheme.BODY.deriveFont(date.equals(today) ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
            dateButton.setForeground(date.equals(today) ? AppTheme.BLUE : AppTheme.TEXT);
            dateButton.setHorizontalAlignment(JButton.LEFT);
            dateButton.setContentAreaFilled(false);
            dateButton.setBorder(AppTheme.padding(1, 2, 5, 2));
            cell.add(dateButton, BorderLayout.NORTH);
            JPanel entries = new JPanel();
            entries.setOpaque(false);
            entries.setLayout(expanded ? new BoxLayout(entries, BoxLayout.Y_AXIS)
                    : new FlowLayout(FlowLayout.LEFT, 4, 0));
            int visible = expanded ? 4 : 6;
            for (int i = 0; i < Math.min(items.size(), visible); i++) {
                Deadline item = items.get(i);
                Course course = courses.get(item.courseId());
                String detail = (course == null ? "" : course.name() + " · ") + item.title();
                if (expanded) {
                    JButton entry = new JButton(item.title());
                    entry.setHorizontalAlignment(JButton.LEFT);
                    entry.setMargin(new java.awt.Insets(1, 3, 1, 3));
                    entry.setBackground(AppTheme.RAISED);
                    entry.setForeground(AppTheme.TEXT);
                    entry.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, courseColor(item)),
                            BorderFactory.createEmptyBorder(2, 2, 2, 2)));
                    entry.setToolTipText(detail);
                    entry.addActionListener(event -> onDeadline.accept(item));
                    entries.add(entry);
                } else {
                    JLabel dot = new JLabel("●");
                    dot.setFont(AppTheme.BODY);
                    dot.setForeground(courseColor(item));
                    dot.setToolTipText(detail);
                    entries.add(dot);
                }
            }
            if (items.size() > visible) {
                JLabel more = new JLabel("+" + (items.size() - visible));
                more.setForeground(AppTheme.MUTED);
                entries.add(more);
            }
            cell.add(entries, BorderLayout.CENTER);
            grid.add(cell);
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        long count = deadlines.stream().filter(d -> d.dueDate() != null
                && !d.dueDate().isBefore(expandedMonday)
                && d.dueDate().isBefore(expandedMonday.plusDays(7))).count();
        JLabel footer = new JLabel("Week of " + expandedMonday + " · " + count + " deadline"
                + (count == 1 ? "" : "s") + " · Select a date to expand its week");
        footer.setForeground(AppTheme.MUTED);
        footer.setBorder(AppTheme.padding(8, 14, 8, 14));
        add(footer, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }
}
