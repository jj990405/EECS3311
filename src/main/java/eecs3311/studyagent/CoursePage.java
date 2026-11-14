package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Course details use real deadline and file rows instead of a text dump. */
public final class CoursePage extends JPanel {
    private final AgentController controller;
    private final JLabel heading = new JLabel("Select a course");
    private final JLabel summary = new JLabel("Choose a course in the workspace to see its work and files.");
    private final JPanel content = new CourseContent();
    private final JScrollPane scroll = new JScrollPane(content);

    public CoursePage(AgentController controller, Runnable calendar, Runnable references,
                      Runnable fileSummary, Runnable quiz, Runnable weakTopics) {
        super(new BorderLayout());
        this.controller = controller;
        setBackground(AppTheme.BACKGROUND);
        JPanel top = new JPanel(new BorderLayout(0, 7));
        top.setBackground(AppTheme.BACKGROUND);
        top.setBorder(AppTheme.padding(26, 30, 18, 30));
        heading.setFont(AppTheme.TITLE);
        heading.setForeground(AppTheme.TEXT);
        top.add(heading, BorderLayout.NORTH);
        summary.setForeground(AppTheme.MUTED);
        top.add(summary, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BACKGROUND);
        content.setBorder(AppTheme.padding(0, 30, 20, 30));
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 2, 8, 8));
        actions.setBackground(AppTheme.SURFACE);
        actions.setBorder(AppTheme.padding(10, 14, 10, 14));
        action(actions, "Calendar", calendar);
        action(actions, "Reference files", references);
        action(actions, "Study notes", fileSummary);
        action(actions, "Practice quiz", quiz);
        action(actions, "Weak topics", weakTopics);
        add(actions, BorderLayout.SOUTH);
    }

    private void action(JPanel parent, String label, Runnable task) {
        JButton button = AppTheme.button(label, false);
        button.addActionListener(event -> task.run());
        parent.add(button);
    }

    public void showCourse(Course course) {
        heading.setText(course.name());
        AgentController.Progress progress = controller.progress(course.id(), LocalDate.now());
        summary.setText(controller.semester(course.semesterId()).name() + "   ·   "
                + progress.completed() + " of " + progress.total() + " deadlines complete"
                + (progress.overdue() > 0 ? "   ·   " + progress.overdue() + " overdue" : ""));
        content.removeAll();
        List<Deadline> deadlines = controller.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now());
        section("Deadlines", deadlines.size());
        if (deadlines.isEmpty()) empty("No deadlines yet. Add one or analyze your uploaded syllabus.");
        for (Deadline deadline : deadlines) {
            String date = deadline.dueDate() == null ? "TBD" : deadline.dueDate().toString();
            row(date, deadline.title(), deadline.completed() ? "Complete" : deadline.type());
        }
        List<MaterialFile> materials = controller.materials().stream()
                .filter(m -> m.courseId().equals(course.id())).toList();
        content.add(javax.swing.Box.createVerticalStrut(24));
        section("Files", materials.size());
        if (materials.isEmpty()) empty("No files yet. Add a syllabus or lecture material to this course.");
        for (MaterialFile material : materials)
            row(material.category().toString(), material.originalName(),
                    material.lectureWeek() == null ? "" : "Week " + material.lectureWeek());
        content.revalidate();
        content.repaint();
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new java.awt.Point(0, 0)));
    }

    public void clear() {
        heading.setText("Select a course");
        summary.setText("Choose a course in the workspace to see its work and files.");
        content.removeAll();
        content.revalidate();
        content.repaint();
    }

    private void section(String name, int count) {
        JLabel label = new JLabel(name + "   " + count);
        label.setFont(AppTheme.SECTION);
        label.setForeground(AppTheme.TEXT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(AppTheme.padding(0, 0, 12, 0));
        content.add(label);
    }

    private void empty(String message) {
        JLabel label = new JLabel(message);
        label.setForeground(AppTheme.MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(AppTheme.padding(12, 0, 18, 0));
        content.add(label);
    }

    private void row(String leading, String title, String trailing) {
        JPanel row = new JPanel(new BorderLayout(0, 5));
        row.setBackground(AppTheme.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                AppTheme.padding(12, 16, 12, 16)));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel first = new JLabel(leading + (trailing.isBlank() ? "" : " · " + trailing));
        first.setFont(AppTheme.SMALL);
        first.setForeground(AppTheme.MUTED);
        first.setToolTipText(first.getText());
        row.add(first, BorderLayout.NORTH);
        JTextArea middle = new JTextArea(title);
        middle.setEditable(false);
        ((javax.swing.text.DefaultCaret) middle.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        middle.setFocusable(false);
        middle.setLineWrap(true);
        middle.setWrapStyleWord(true);
        middle.setRows(Math.max(1, (title.length() + 29) / 30));
        middle.setFont(AppTheme.BODY.deriveFont(Font.BOLD));
        middle.setForeground(AppTheme.TEXT);
        middle.setBackground(AppTheme.SURFACE);
        row.add(middle, BorderLayout.CENTER);
        row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        content.add(row);
    }

    private static final class CourseContent extends JPanel implements javax.swing.Scrollable {
        @Override public java.awt.Dimension getPreferredScrollableViewportSize() { return new java.awt.Dimension(450, 600); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) { return Math.max(24, visible.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
