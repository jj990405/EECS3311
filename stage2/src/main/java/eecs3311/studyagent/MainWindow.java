package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/** Swing viewer for the implemented workspace and schedule functions. */
public final class MainWindow extends JFrame {
    private final AgentController controller;
    private final JTree tree = new JTree();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Due", "Course", "Type", "Title", "Scope", "Done"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel status = new JLabel(" ");
    private final List<UUID> rowIds = new ArrayList<>();
    private final java.util.Set<UUID> notified = new java.util.HashSet<>();
    private javax.swing.Timer studyDisplayTimer;
    private UUID selectedCourseId;

    public MainWindow(AgentController controller) {
        super("Semester Study and Deadline Management Agent");
        this.controller = controller;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1150, 680);
        setLocationRelativeTo(null);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(actions, "New semester", this::addSemester);
        button(actions, "New course", this::addCourse);
        button(actions, "Upload file", this::uploadFile);
        button(actions, "New deadline", this::addDeadline);
        button(actions, "Calendar", this::showCalendar);
        button(actions, "Reminders", this::showReminders);
        button(actions, "Notify time", this::configureNotification);
        button(actions, "Start study", this::startStudy);
        button(actions, "Pause", controller::pauseStudy);
        button(actions, "Resume", controller::resumeStudy);
        button(actions, "Stop study", this::stopStudy);
        button(actions, "Ask sources", this::askSources);
        button(actions, "Weekly brief", this::weeklyBrief);
        button(actions, "AI syllabus", this::aiSyllabus);
        button(actions, "AI week", this::aiWeek);
        button(actions, "Review proposals", this::reviewProposals);
        button(actions, "History", this::showHistory);
        button(actions, "Self assess", this::selfAssess);
        button(actions, "Edit deadline", this::editDeadline);
        button(actions, "Toggle done", this::toggleDone);
        button(actions, "Delete deadline", this::deleteDeadline);
        button(actions, "Show all", () -> { selectedCourseId = null; refresh(); });
        JScrollPane actionScroll = new JScrollPane(actions,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        actionScroll.setPreferredSize(new java.awt.Dimension(1000, 58));
        add(actionScroll, BorderLayout.NORTH);

        tree.addTreeSelectionListener(this::selectionChanged);
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) openSelectedFile();
            }
        });
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree),
                new JScrollPane(table));
        split.setDividerLocation(330);
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        controller.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));
        refresh();
        javax.swing.Timer notificationTimer = new javax.swing.Timer(60_000, event -> showDueNotifications());
        notificationTimer.start();
        SwingUtilities.invokeLater(this::showDueNotifications);
    }

    private void button(JPanel parent, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(e -> perform(action));
        parent.add(button);
    }

    private void perform(Runnable action) {
        try {
            action.run();
        } catch (CancellationException ignored) {
            // Cancelled input makes no change.
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to complete action",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String ask(String label, String initial) {
        String value = (String) JOptionPane.showInputDialog(this, label, "Study Agent",
                JOptionPane.QUESTION_MESSAGE, null, null, initial);
        if (value == null) throw new CancellationException();
        return value.trim();
    }

    private LocalDate date(String label, String initial, boolean optional) {
        String value = ask(label + " (YYYY-MM-DD" + (optional ? ", blank for TBD" : "") + ")", initial);
        if (optional && value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Use a valid date such as 2026-11-10.");
        }
    }

    private <T> T choose(String label, List<T> choices) {
        if (choices.isEmpty()) throw new IllegalStateException("Create the parent item first.");
        Object answer = JOptionPane.showInputDialog(this, label, "Study Agent",
                JOptionPane.QUESTION_MESSAGE, null, choices.toArray(), choices.get(0));
        if (answer == null) throw new CancellationException();
        @SuppressWarnings("unchecked") T selected = (T) answer;
        return selected;
    }

    private Semester chooseSemester() { return choose("Choose semester", controller.semesters()); }
    private Course chooseCourse() { return choose("Choose course", controller.courses()); }

    private void addSemester() {
        try {
            controller.createSemester(ask("Semester name", "Fall 2026"),
                    date("Start date", "2026-09-01", false), date("End date", "2026-12-31", false));
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void addCourse() {
        Semester semester = chooseSemester();
        String name = ask("Course name", "");
        String color = ask("Color (#RRGGBB)", "#3366CC");
        Difficulty difficulty = choose("Course difficulty", List.of(Difficulty.values()));
        try {
            controller.createCourse(semester.id(), name, color, difficulty);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void uploadFile() {
        Course course = chooseCourse();
        Category category = choose("File category", List.of(Category.values()));
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            MaterialFile material = controller.uploadMaterial(course.id(), category, chooser.getSelectedFile().toPath());
            if (!material.extractionWarning().isBlank()) {
                JOptionPane.showMessageDialog(this, material.extractionWarning(), "File stored", JOptionPane.WARNING_MESSAGE);
            }
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void addDeadline() {
        Course course = chooseCourse();
        try {
            controller.addDeadline(course.id(), ask("Deadline title", ""), ask("Type (e.g. Assignment)", ""),
                    date("Due date", "", true), ask("Covered topics / scope", ""));
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showCalendar() {
        String value = ask("Month (YYYY-MM)", java.time.YearMonth.now().toString());
        java.time.YearMonth month = java.time.YearMonth.parse(value);
        CalendarPanel calendar = new CalendarPanel(month, LocalDate.now(),
                controller.schedule(AgentController.Period.ALL, selectedCourseId, "", LocalDate.now()));
        JOptionPane.showMessageDialog(this, calendar, "Monthly calendar", JOptionPane.PLAIN_MESSAGE);
    }

    private void showReminders() {
        var items = controller.reminders(LocalDate.now(), 7);
        String message = items.isEmpty() ? "No deadlines due in the next seven days."
                : items.stream().map(d -> d.dueDate() + " " + controller.course(d.courseId()).name()
                        + " " + d.title()).collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, message, "Upcoming deadlines", JOptionPane.INFORMATION_MESSAGE);
    }

    private void configureNotification() {
        Deadline deadline = selectedDeadline();
        java.time.LocalTime time = java.time.LocalTime.parse(ask("Due time (HH:MM)", deadline.dueTime().toString()));
        int minutes = Integer.parseInt(ask("Notify minutes before", Integer.toString(deadline.notifyBeforeMinutes())));
        try { controller.setNotification(deadline.id(), time, minutes); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showDueNotifications() {
        var newlyDue = controller.checkNotifications(java.time.LocalDateTime.now()).stream()
                .filter(d -> notified.add(d.id())).toList();
        if (!newlyDue.isEmpty()) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            status.setText("Due soon: " + newlyDue.stream().map(Deadline::title)
                    .collect(java.util.stream.Collectors.joining(", ")));
        }
    }

    private void startStudy() {
        Course course = chooseCourse();
        String topic = ask("Study topic", "");
        int minutes = Integer.parseInt(ask("Duration in minutes (1-240)", "25"));
        controller.startStudy(course.id(), topic, minutes);
        status.setText("Studying " + course.name() + " — timer running");
        if (studyDisplayTimer != null) studyDisplayTimer.stop();
        studyDisplayTimer = new javax.swing.Timer(1000, event -> {
            long remaining = controller.remainingStudySeconds();
            status.setText("Studying " + course.name() + " — " + remaining / 60 + ":"
                    + String.format("%02d", remaining % 60) + " remaining");
            if (remaining == 0 && controller.studyActive()) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                stopStudy();
            }
        });
        studyDisplayTimer.start();
    }

    private void stopStudy() {
        try {
            if (studyDisplayTimer != null) studyDisplayTimer.stop();
            StudySession session = controller.stopStudy();
            JOptionPane.showMessageDialog(this, "Saved " + session.seconds() + " seconds for " + session.topic());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void askSources() {
        Course course = chooseCourse();
        String question = ask("Question about uploaded materials", "");
        try {
            GroundedAnswer answer = controller.answerQuestion(course.id(), question);
            String citations = answer.citations().stream().map(c -> c.fileName() + " p." + c.page())
                    .collect(java.util.stream.Collectors.joining("\n"));
            JOptionPane.showMessageDialog(this, answer.answer() + "\n\nSources:\n" + citations,
                    "Material answer", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void weeklyBrief() {
        try {
            JOptionPane.showMessageDialog(this, controller.weeklyBriefing(chooseCourse().id(), LocalDate.now()),
                    "Weekly briefing", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private MaterialFile chooseMaterial(Category category) {
        return choose("Choose " + category + " file", controller.materials().stream()
                .filter(m -> m.category() == category).toList());
    }

    private void aiSyllabus() {
        try {
            var proposals = controller.suggestSyllabusDeadlines(chooseMaterial(Category.SYLLABUS).id());
            JOptionPane.showMessageDialog(this, proposals.size() + " suggestions ready. Open Review proposals.");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void aiWeek() {
        try {
            var proposal = controller.suggestLectureWeek(chooseMaterial(Category.LECTURE_MATERIALS).id());
            JOptionPane.showMessageDialog(this, proposal.description() + "\nOpen Review proposals to approve.");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void reviewProposals() {
        var proposal = choose("Choose a proposal", controller.pendingProposals());
        int decision = JOptionPane.showOptionDialog(this, proposal.description(), "Review AI proposal",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[] {"Approve", "Reject", "Cancel"}, "Cancel");
        try {
            if (decision == 0) controller.approveProposal(proposal.id());
            if (decision == 1) controller.rejectProposal(proposal.id());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showHistory() {
        try {
            String content = controller.history().stream().map(e -> e.time() + " " + e.event() + " " + e.detail())
                    .collect(java.util.stream.Collectors.joining("\n"));
            javax.swing.JTextArea area = new javax.swing.JTextArea(content.isBlank() ? "No interactions yet." : content, 18, 65);
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Agent history", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void selfAssess() {
        Course course = chooseCourse();
        String topic = ask("Topic to assess", "");
        int confidence = Integer.parseInt(ask("Confidence from 1 to 5", "3"));
        try {
            controller.saveSelfAssessment(course.id(), topic, confidence);
            JOptionPane.showMessageDialog(this, "Saved. Use the CLI 'weak advice' command for AI guidance.");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private Deadline selectedDeadline() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) throw new IllegalStateException("Select a deadline in the table first.");
        return controller.deadline(rowIds.get(table.convertRowIndexToModel(selectedRow)));
    }

    private void editDeadline() {
        Deadline old = selectedDeadline();
        try {
            controller.editDeadline(old.id(), ask("Title", old.title()), ask("Type", old.type()),
                    date("Due date", old.dueDate() == null ? "" : old.dueDate().toString(), true),
                    ask("Scope", old.scope()));
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void toggleDone() {
        Deadline old = selectedDeadline();
        try {
            controller.setCompleted(old.id(), !old.completed());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void deleteDeadline() {
        Deadline old = selectedDeadline();
        int choice = JOptionPane.showConfirmDialog(this, "Delete " + old.title() + "?", "Confirm deletion",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            controller.deleteDeadline(old.id());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void selectionChanged(TreeSelectionEvent event) {
        Object selected = tree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode treeNode)) return;
        Object value = treeNode.getUserObject();
        if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.COURSE) {
            selectedCourseId = branch.id();
            refreshSchedule();
        }
    }

    private void openSelectedFile() {
        Object selected = tree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode node)
                || !(node.getUserObject() instanceof WorkspaceLeaf leaf)) return;
        perform(() -> {
            try {
                var path = controller.materialPath(leaf.file().id());
                if (!Files.isRegularFile(path)) throw new IllegalStateException("Stored file is missing: " + path);
                Desktop.getDesktop().open(path.toFile());
            } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        });
    }

    private void refresh() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Workspace");
        for (WorkspaceNode node : controller.workspace()) root.add(toSwingNode(node));
        tree.setModel(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        if (selectedCourseId != null) {
            var nodes = root.breadthFirstEnumeration();
            while (nodes.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                if (node.getUserObject() instanceof WorkspaceBranch branch
                        && branch.kind() == WorkspaceBranch.Kind.COURSE
                        && branch.id().equals(selectedCourseId)) {
                    tree.setSelectionPath(new TreePath(node.getPath()));
                    break;
                }
            }
        }
        refreshSchedule();
    }

    private DefaultMutableTreeNode toSwingNode(WorkspaceNode node) {
        DefaultMutableTreeNode result = new DefaultMutableTreeNode(node);
        for (WorkspaceNode child : node.children()) result.add(toSwingNode(child));
        return result;
    }

    private void refreshSchedule() {
        tableModel.setRowCount(0);
        rowIds.clear();
        List<Deadline> deadlines = controller.schedule(AgentController.Period.ALL, selectedCourseId, "",
                LocalDate.now());
        for (Deadline deadline : deadlines) {
            Course course = controller.course(deadline.courseId());
            tableModel.addRow(new Object[] {deadline.dueDate() == null ? "TBD" : deadline.dueDate(),
                    course.name(), deadline.type(), deadline.title(), deadline.scope(),
                    deadline.completed() ? "Yes" : "No"});
            rowIds.add(deadline.id());
        }
        if (selectedCourseId == null) {
            status.setText(deadlines.isEmpty() ? "No deadlines yet. Use New deadline to add one."
                    : "All courses | " + deadlines.size() + " deadlines | Select a course to see progress");
        } else {
            Course course = controller.course(selectedCourseId);
            AgentController.Progress p = controller.progress(selectedCourseId, LocalDate.now());
            status.setText(course.name() + " | " + p.completed() + "/" + p.total()
                    + " completed | " + p.overdue() + " overdue"
                    + (deadlines.isEmpty() ? " | No deadlines for this course yet" : ""));
        }
    }
}
