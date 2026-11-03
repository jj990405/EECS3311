package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Course workspace showing progress, upcoming work, material names, and study actions. */
public final class CoursePage extends JPanel {
    private final AgentController controller;
    private final JLabel heading = new JLabel("Select a course in the workspace");
    private final JTextArea content = new JTextArea();

    public CoursePage(AgentController controller, Runnable calendar, Runnable references,
                      Runnable summary, Runnable quiz, Runnable weakTopics) {
        super(new BorderLayout(0, 12));
        this.controller = controller;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14));
        add(heading, BorderLayout.NORTH);
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        add(new JScrollPane(content), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        action(actions, "Calendar", calendar);
        action(actions, "Reference files", references);
        action(actions, "Summarize", summary);
        action(actions, "Quiz", quiz);
        action(actions, "Weak topics", weakTopics);
        add(actions, BorderLayout.SOUTH);
    }

    private void action(JPanel parent, String label, Runnable task) {
        JButton button = new JButton(label);
        button.addActionListener(event -> task.run());
        parent.add(button);
    }

    public void showCourse(Course course) {
        heading.setText(course.name() + " · " + controller.semester(course.semesterId()).name()
                + " · " + course.difficulty() + " difficulty");
        AgentController.Progress progress = controller.progress(course.id(), LocalDate.now());
        StringBuilder text = new StringBuilder("Progress: " + progress.completed() + "/" + progress.total()
                + " complete; " + progress.overdue() + " overdue\n\nDeadlines\n");
        List<Deadline> deadlines = controller.schedule(AgentController.Period.ALL, course.id(), "", LocalDate.now());
        if (deadlines.isEmpty()) text.append("No deadlines yet.\n");
        for (Deadline deadline : deadlines) text.append("• ")
                .append(deadline.dueDate() == null ? "TBD" : deadline.dueDate()).append(" · ")
                .append(deadline.title()).append(deadline.completed() ? " ✓" : "").append('\n');
        text.append("\nMaterials\n");
        List<MaterialFile> materials = controller.materials().stream()
                .filter(m -> m.courseId().equals(course.id())).toList();
        if (materials.isEmpty()) text.append("No files yet.\n");
        for (MaterialFile material : materials) text.append("• ").append(material.category()).append(" · ")
                .append(material.originalName())
                .append(material.lectureWeek() == null ? "" : " · week " + material.lectureWeek())
                .append('\n');
        content.setText(text.toString());
        content.setCaretPosition(0);
    }
}
