package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.Scrollable;
import java.awt.Rectangle;

/** Month grid with course colours and a selectable expanded week, as in SD03. */
public final class CalendarPanel extends JPanel {
    private final YearMonth month;
    private final LocalDate today;
    private final List<Deadline> deadlines;
    private final Map<UUID, Course> courses;
    private final Consumer<Deadline> onDeadline;
    private final Consumer<LocalDate> onDate;
    private LocalDate expandedMonday;

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines) {
        this(month, today, deadlines, Map.of(), deadline -> { }, date -> { });
    }

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines,
                         Map<UUID, Course> courses) {
        this(month, today, deadlines, courses, deadline -> { }, date -> { });
    }

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines,
                         Map<UUID, Course> courses, Consumer<Deadline> onDeadline) {
        this(month, today, deadlines, courses, onDeadline, date -> { });
    }

    public CalendarPanel(YearMonth month, LocalDate today, List<Deadline> deadlines,
                         Map<UUID, Course> courses, Consumer<Deadline> onDeadline,
                         Consumer<LocalDate> onDate) {
        super(new BorderLayout(8, 8));
        this.month = month;
        this.today = today;
        this.deadlines = List.copyOf(deadlines);
        this.courses = Map.copyOf(courses);
        this.onDeadline = onDeadline;
        this.onDate = onDate;
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
        JPanel grid = new MonthGrid();
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
            dateButton.setToolTipText("Open schedule for " + date);
            dateButton.addActionListener(event -> openDay(date));
            dateButton.setFont(AppTheme.BODY.deriveFont(date.equals(today) ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
            dateButton.setForeground(date.equals(today) ? AppTheme.BLUE : AppTheme.TEXT);
            dateButton.setHorizontalAlignment(JButton.LEFT);
            dateButton.setContentAreaFilled(false);
            dateButton.setBorder(AppTheme.padding(1, 2, 5, 2));
            cell.add(dateButton, BorderLayout.NORTH);
            JPanel entries = new JPanel();
            entries.setOpaque(false);
            entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
            java.awt.event.MouseAdapter openEmptyDay = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent event) { openDay(date); }
            };
            cell.addMouseListener(openEmptyDay);
            entries.addMouseListener(openEmptyDay);
            for (Deadline item : items) {
                Course course = courses.get(item.courseId());
                String detail = (course == null ? "" : "[" + course.name() + "] ") + item.title();
                String courseLabel = course == null ? "" : "[" + escapeHtml(course.name()) + "]<br>";
                JButton entry = new JButton("<html><body width='92'>" + courseLabel
                        + escapeHtml(item.title()) + "</body></html>");
                entry.getAccessibleContext().setAccessibleName(detail);
                entry.setFont(AppTheme.SMALL);
                entry.setHorizontalAlignment(JButton.LEFT);
                entry.setVerticalAlignment(JButton.TOP);
                entry.setBackground(AppTheme.RAISED);
                entry.setForeground(AppTheme.TEXT);
                entry.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, courseColor(item)),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
                entry.setToolTipText(detail);
                entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
                entry.addActionListener(event -> onDeadline.accept(item));
                entries.add(entry);
            }
            cell.add(entries, BorderLayout.CENTER);
            grid.add(cell);
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        long count = deadlines.stream().filter(d -> d.dueDate() != null
                && !d.dueDate().isBefore(expandedMonday)
                && d.dueDate().isBefore(expandedMonday.plusDays(7))).count();
        JLabel footer = new JLabel("Week of " + expandedMonday + " · " + count + " deadline"
                + (count == 1 ? "" : "s") + " · Click any day to add or edit a deadline");
        footer.setForeground(AppTheme.MUTED);
        footer.setBorder(AppTheme.padding(8, 14, 8, 14));
        add(footer, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private void openDay(LocalDate date) {
        expandedMonday = monday(date);
        render();
        onDate.accept(date);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class MonthGrid extends JPanel implements Scrollable {
        MonthGrid() { super(new GridLayout(0, 7, 4, 4)); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(740, 510); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 40;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(80, visible.height - 40);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
