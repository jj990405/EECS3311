package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/** Swing viewer for the implemented workspace and schedule functions. */
public final class MainWindow extends JFrame {
    private record QuizBuild(StudyArtifacts.Quiz multipleChoice,
                             StudyArtifacts.EssayPrompt essay) { }
    private final AgentController controller;
    private final JTree tree = new JTree();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Due", "Course", "Type", "Title", "Scope", "Done"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel status = new JLabel(" ");
    private final JTabbedPane contentTabs = new JTabbedPane();
    private final CoursePage coursePage;
    private final TimerPanel timerPanel;
    private final TimerRecordPanel timerRecords;
    private final ChatPanel chatPanel;
    private final JPanel calendarHost = new JPanel(new BorderLayout());
    private final JLabel fileHeading = new JLabel("Select a file in the workspace");
    private final JLabel fileMeta = new JLabel("Uploaded files are stored locally.");
    private final JTextArea filePreview = new JTextArea();
    private final JButton openFileButton = new JButton("Open original");
    private YearMonth visibleMonth = YearMonth.now();
    private String lastUploadDirectory = System.getProperty("user.home");
    private final List<UUID> rowIds = new ArrayList<>();
    private javax.swing.Timer studyDisplayTimer;
    private UUID selectedCourseId;
    private boolean aiBusy;

    public MainWindow(AgentController controller) {
        super("Semester Study and Deadline Management Agent");
        this.controller = controller;
        this.coursePage = new CoursePage(controller, () -> perform(this::showCalendar),
                () -> perform(this::showReferences), () -> perform(this::summarizeFile),
                () -> perform(this::generateQuiz), () -> perform(this::showWeakTopics));
        this.timerPanel = new TimerPanel(minutes -> perform(() -> startStudy(minutes)),
                () -> perform(controller::pauseStudy), () -> perform(controller::resumeStudy),
                () -> perform(this::stopStudy), () -> perform(this::showTimerRecords));
        this.timerRecords = new TimerRecordPanel(controller);
        this.chatPanel = new ChatPanel(question -> perform(() -> askSources(question)),
                change -> perform(() -> requestScheduleChange(change)));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);

        setJMenuBar(createMenuBar());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        button(actions, "New semester", this::addSemester);
        button(actions, "New course", this::addCourse);
        button(actions, "Upload files", this::uploadFiles);
        button(actions, "New deadline", this::addDeadline);
        button(actions, "Calendar", this::showCalendar);
        add(actions, BorderLayout.NORTH);

        tree.addTreeSelectionListener(this::selectionChanged);
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent event) { showTreeMenu(event); }
            @Override public void mouseReleased(java.awt.event.MouseEvent event) { showTreeMenu(event); }
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) openSelectedFile();
            }
        });
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        contentTabs.addTab("Timeline", new JScrollPane(table));
        JPanel calendarTab = new JPanel(new BorderLayout(0, 8));
        JPanel monthActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(monthActions, "Previous month", () -> changeMonth(-1));
        button(monthActions, "Today", () -> { visibleMonth = YearMonth.now(); refreshCalendar(); });
        button(monthActions, "Next month", () -> changeMonth(1));
        calendarTab.add(monthActions, BorderLayout.NORTH);
        calendarTab.add(calendarHost, BorderLayout.CENTER);
        contentTabs.addTab("Calendar", calendarTab);
        contentTabs.addTab("Files", createFilePanel());
        contentTabs.addTab("Course", coursePage);
        contentTabs.addTab("Study timer", timerPanel);
        contentTabs.addTab("Study records", timerRecords);
        contentTabs.addTab("Chat", chatPanel);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree),
                contentTabs);
        split.setDividerLocation(330);
        add(split, BorderLayout.CENTER);
        status.setToolTipText("Click to inspect undated deadlines");
        status.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                perform(MainWindow.this::openUndatedDeadline);
            }
        });
        add(status, BorderLayout.SOUTH);

        controller.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));
        refresh();
        javax.swing.Timer notificationTimer = new javax.swing.Timer(60_000, event -> showDueNotifications());
        notificationTimer.start();
        SwingUtilities.invokeLater(this::showDueNotifications);
        SwingUtilities.invokeLater(this::showWeeklyBriefIfNeeded);
    }

    private void button(JPanel parent, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(e -> perform(action));
        parent.add(button);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu workspace = new JMenu("Workspace");
        menuItem(workspace, "New semester", this::addSemester);
        menuItem(workspace, "New course", this::addCourse);
        menuItem(workspace, "Upload files", this::uploadFiles);
        menuItem(workspace, "Show all courses", () -> { selectedCourseId = null; tree.clearSelection(); refresh(); });
        bar.add(workspace);
        JMenu schedule = new JMenu("Schedule");
        menuItem(schedule, "New deadline", this::addDeadline);
        menuItem(schedule, "Calendar", this::showCalendar);
        menuItem(schedule, "Edit selected deadline", this::editDeadline);
        menuItem(schedule, "Toggle completed", this::toggleDone);
        menuItem(schedule, "Delete selected deadline", this::deleteDeadline);
        menuItem(schedule, "Reminders", this::showReminders);
        menuItem(schedule, "Notification time", this::configureNotification);
        bar.add(schedule);
        JMenu study = new JMenu("Study");
        menuItem(study, "Start timer", this::startStudy);
        menuItem(study, "Pause timer", controller::pauseStudy);
        menuItem(study, "Resume timer", controller::resumeStudy);
        menuItem(study, "Stop timer", this::stopStudy);
        menuItem(study, "Timer records", this::showTimerRecords);
        menuItem(study, "Self assessment", this::selfAssess);
        menuItem(study, "Weak topics", this::showWeakTopics);
        bar.add(study);
        JMenu ai = new JMenu("AI");
        menuItem(ai, "Ask sources", this::askSources);
        menuItem(ai, "Suggest reference files", this::showReferences);
        menuItem(ai, "Request schedule change", this::requestScheduleChange);
        menuItem(ai, "Weekly brief", this::weeklyBrief);
        menuItem(ai, "Summarize file", this::summarizeFile);
        menuItem(ai, "Saved summaries", this::showSummaries);
        menuItem(ai, "Generate quiz", this::generateQuiz);
        menuItem(ai, "Take saved quiz", this::takeQuiz);
        menuItem(ai, "Generate essay question", this::generateEssayQuestion);
        menuItem(ai, "Answer essay question", this::answerEssayQuestion);
        menuItem(ai, "Extract syllabus deadlines", this::aiSyllabus);
        menuItem(ai, "Suggest lecture week", this::aiWeek);
        menuItem(ai, "Review proposals", this::reviewProposals);
        bar.add(ai);
        JMenu history = new JMenu("History");
        menuItem(history, "View interactions", this::showHistory);
        bar.add(history);
        return bar;
    }

    private void menuItem(JMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> perform(action));
        menu.add(item);
    }

    private void popupItem(JPopupMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> perform(action));
        menu.add(item);
    }

    private void showTreeMenu(java.awt.event.MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        TreePath path = tree.getPathForLocation(event.getX(), event.getY());
        if (path == null) return;
        tree.setSelectionPath(path);
        Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        JPopupMenu menu = new JPopupMenu();
        if (value instanceof WorkspaceBranch branch) {
            if (branch.kind() == WorkspaceBranch.Kind.SEMESTER) {
                menuItemForSemester(menu, branch);
            } else if (branch.kind() == WorkspaceBranch.Kind.COURSE) {
                Course course = controller.course(branch.id());
                popupItem(menu, "Upload files...", () -> uploadFiles(course, chooseCategory()));
                popupItem(menu, "New deadline...", () -> addDeadline(course));
                popupItem(menu, "View calendar", this::showCalendar);
            } else {
                Course course = controller.course(branch.id());
                Category category = categoryOf(branch);
                popupItem(menu, "Upload files to " + category + "...", () -> uploadFiles(course, category));
            }
        } else if (value instanceof WorkspaceLeaf leaf) {
            popupItem(menu, "Show file details", () -> showMaterial(leaf.file()));
            popupItem(menu, "Open original", this::openSelectedFile);
            if (leaf.file().category() == Category.LECTURE_MATERIALS)
                popupItem(menu, "Assign lecture week...", () -> assignLectureWeek(leaf.file()));
            popupItem(menu, "Upload more to " + leaf.file().category() + "...",
                    () -> uploadFiles(controller.course(leaf.file().courseId()), leaf.file().category()));
        }
        if (menu.getComponentCount() > 0) menu.show(tree, event.getX(), event.getY());
    }

    private void menuItemForSemester(JPopupMenu menu, WorkspaceBranch branch) {
        Semester semester = controller.semesters().stream().filter(s -> s.id().equals(branch.id()))
                .findFirst().orElseThrow();
        popupItem(menu, "New course...", () -> addCourse(semester));
    }

    private Category categoryOf(WorkspaceBranch branch) {
        for (Category category : Category.values()) {
            if (category.toString().equals(branch.label())) return category;
        }
        throw new IllegalStateException("Unknown category: " + branch.label());
    }

    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        fileHeading.setFont(fileHeading.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        heading.add(fileHeading, BorderLayout.NORTH);
        heading.add(fileMeta, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        openFileButton.setEnabled(false);
        openFileButton.addActionListener(e -> perform(this::openSelectedFile));
        controls.add(openFileButton);
        JButton addFiles = new JButton("Upload more files");
        addFiles.addActionListener(e -> perform(this::uploadFiles));
        controls.add(addFiles);
        heading.add(controls, BorderLayout.SOUTH);
        panel.add(heading, BorderLayout.NORTH);
        filePreview.setEditable(false);
        filePreview.setLineWrap(true);
        filePreview.setWrapStyleWord(true);
        filePreview.setText("Choose a file on the left to see its extracted text here. Text extraction works without an AI key.");
        panel.add(new JScrollPane(filePreview), BorderLayout.CENTER);
        return panel;
    }

    private void changeMonth(int offset) {
        visibleMonth = visibleMonth.plusMonths(offset);
        refreshCalendar();
    }

    private void refreshCalendar() {
        Map<UUID, Course> courses = controller.courses().stream()
                .collect(java.util.stream.Collectors.toMap(Course::id, c -> c));
        calendarHost.removeAll();
        calendarHost.add(new CalendarPanel(visibleMonth, LocalDate.now(),
                controller.schedule(AgentController.Period.ALL, selectedCourseId, "", LocalDate.now()), courses,
                this::showDeadlineDetails),
                BorderLayout.CENTER);
        calendarHost.revalidate();
        calendarHost.repaint();
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

    private <T> void runAi(String activity, Callable<T> task, Consumer<T> success) {
        if (aiBusy) {
            status.setText("An AI request is already running.");
            return;
        }
        aiBusy = true;
        status.setText(activity + "...");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                aiBusy = false;
                try {
                    T result = get();
                    status.setText(activity + " complete.");
                    success.accept(result);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText(activity + " interrupted.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String message = cause == null ? ex.getMessage() : cause.getMessage();
                    status.setText(activity + " failed.");
                    JOptionPane.showMessageDialog(MainWindow.this, message, "AI request failed",
                            JOptionPane.ERROR_MESSAGE);
                } catch (RuntimeException ex) {
                    status.setText(activity + " needs attention.");
                    JOptionPane.showMessageDialog(MainWindow.this, ex.getMessage(), "Unable to complete action",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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
        JComboBox<T> box = new JComboBox<>();
        choices.forEach(box::addItem);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                JLabel row = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                String text = displayName(value);
                row.setText(text.length() > 110 ? text.substring(0, 107) + "..." : text);
                return row;
            }
        });
        box.setPreferredSize(new Dimension(480, 30));
        int answer = JOptionPane.showConfirmDialog(this, box, label, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) throw new CancellationException();
        return (T) box.getSelectedItem();
    }

    private String displayName(Object value) {
        if (value instanceof MaterialFile material)
            return controller.course(material.courseId()).name() + " / " + material.originalName();
        if (value instanceof CommandInvoker.Proposal proposal) return proposal.description();
        return String.valueOf(value);
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
        addCourse(chooseSemester());
    }

    private void addCourse(Semester semester) {
        String name = ask("Course name", "");
        String color = ask("Color (#RRGGBB)", "#3366CC");
        Difficulty difficulty = choose("Course difficulty", List.of(Difficulty.values()));
        try {
            controller.createCourse(semester.id(), name, color, difficulty);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private Category chooseCategory() { return choose("Choose file category", List.of(Category.values())); }

    private void uploadFiles() {
        Object value = selectedTreeValue();
        if (value instanceof WorkspaceLeaf leaf) {
            uploadFiles(controller.course(leaf.file().courseId()), leaf.file().category());
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.CATEGORY) {
            uploadFiles(controller.course(branch.id()), categoryOf(branch));
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.COURSE) {
            uploadFiles(controller.course(branch.id()), chooseCategory());
        } else {
            uploadFiles(chooseCourse(), chooseCategory());
        }
    }

    private Object selectedTreeValue() {
        Object selected = tree.getLastSelectedPathComponent();
        return selected instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
    }

    private void uploadFiles(Course course, Category category) {
        FileDialog chooser = new FileDialog(this, "Upload files to " + course.name() + " / " + category,
                FileDialog.LOAD);
        chooser.setMultipleMode(true);
        chooser.setDirectory(lastUploadDirectory);
        chooser.setVisible(true);
        File[] files = chooser.getFiles();
        if (files.length == 0) return;
        lastUploadDirectory = chooser.getDirectory();
        int added = 0;
        MaterialFile last = null;
        List<MaterialFile> lectureFiles = new ArrayList<>();
        List<MaterialFile> syllabusFiles = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (File file : files) {
            try {
                boolean duplicate = controller.materials().stream().anyMatch(m -> m.courseId().equals(course.id())
                        && m.category() == category && m.originalName().equalsIgnoreCase(file.getName()));
                boolean replace = false;
                if (duplicate) {
                    int choice = JOptionPane.showOptionDialog(this,
                            file.getName() + " already exists in " + category + ". Replace it?",
                            "Duplicate file", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            new Object[] {"Replace", "Skip", "Stop upload"}, "Skip");
                    if (choice == 2 || choice < 0) break;
                    if (choice == 1) continue;
                    replace = true;
                }
                MaterialFile material = controller.uploadMaterial(course.id(), category, file.toPath(), replace);
                added++;
                last = material;
                if (category == Category.LECTURE_MATERIALS && material.textAvailable()) lectureFiles.add(material);
                if (category == Category.SYLLABUS && material.textAvailable()) syllabusFiles.add(material);
                if (!material.extractionWarning().isBlank())
                    problems.add(file.getName() + ": " + material.extractionWarning());
            } catch (Exception ex) {
                problems.add(file.getName() + ": " + ex.getMessage());
            }
        }
        refresh();
        if (last != null) selectMaterial(last.id());
        status.setText(added + " file" + (added == 1 ? "" : "s") + " added to " + course.name()
                + " / " + category + (problems.isEmpty() ? "" : " | " + problems.size() + " need attention"));
        if (!problems.isEmpty()) JOptionPane.showMessageDialog(this, String.join("\n", problems),
                "Upload details", JOptionPane.WARNING_MESSAGE);
        if (!syllabusFiles.isEmpty()) suggestUploadedSyllabi(syllabusFiles);
        else if (!lectureFiles.isEmpty()) suggestUploadedWeeks(lectureFiles);
    }

    private void suggestUploadedSyllabi(List<MaterialFile> files) {
        runAi("Extracting uploaded syllabus deadlines", () -> {
            List<String> results = new ArrayList<>();
            for (MaterialFile file : files) {
                try {
                    int count = controller.suggestSyllabusDeadlines(file.id()).size();
                    results.add(file.originalName() + ": " + count + " deadline candidate(s)");
                } catch (Exception ex) {
                    results.add(file.originalName() + ": " + ex.getMessage()
                            + " (use New deadline to enter dates manually)");
                }
            }
            return results;
        }, results -> {
            showText("Syllabus extraction", String.join("\n", results));
            if (!controller.pendingProposals().isEmpty()) reviewProposals();
        });
    }

    private void suggestUploadedWeeks(List<MaterialFile> files) {
        runAi("Matching uploaded lecture weeks", () -> {
            List<String> results = new ArrayList<>();
            for (MaterialFile file : files) {
                try {
                    CommandInvoker.Proposal proposal = controller.suggestLectureWeek(file.id());
                    results.add(file.originalName() + ": " + proposal.description());
                } catch (Exception ex) {
                    results.add(file.originalName() + ": " + ex.getMessage()
                            + " (right-click the file to assign a week manually)");
                }
            }
            return results;
        }, results -> {
            showText("Lecture week suggestions", String.join("\n\n", results));
            if (!controller.pendingProposals().isEmpty()) reviewProposals();
        });
    }

    private void assignLectureWeek(MaterialFile file) {
        String current = file.lectureWeek() == null ? "" : file.lectureWeek().toString();
        String input = ask("Lecture week (1-18, blank to clear)", current);
        Integer week = input.isBlank() ? null : Integer.valueOf(input);
        if (week != null && (week < 1 || week > 18))
            throw new IllegalArgumentException("Lecture week must be 1-18.");
        try { controller.assignLectureWeek(file.id(), week); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void addDeadline() {
        addDeadline(chooseCourse());
    }

    private void addDeadline(Course course) {
        try {
            controller.addDeadline(course.id(), ask("Deadline title", ""), ask("Type (e.g. Assignment)", ""),
                    date("Due date", "", true), ask("Covered topics / scope", ""));
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showCalendar() {
        refreshCalendar();
        contentTabs.setSelectedIndex(1);
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
        try {
            var newlyDue = controller.unseenNotifications(java.time.LocalDateTime.now());
            if (newlyDue.isEmpty()) return;
            java.awt.Toolkit.getDefaultToolkit().beep();
            String message = newlyDue.stream().map(d -> controller.course(d.courseId()).name()
                    + " · " + d.title() + " · " + d.dueDate()).collect(java.util.stream.Collectors.joining("\n"));
            status.setText("Deadline notification: " + newlyDue.size() + " item(s)");
            JOptionPane.showMessageDialog(this, message, "Deadline notifications", JOptionPane.INFORMATION_MESSAGE);
            for (Deadline deadline : newlyDue) controller.markNotificationShown(deadline);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void startStudy() {
        Integer minutes = choose("Study timer length", List.of(15, 30, 60));
        startStudy(minutes);
    }

    private void startStudy(int minutes) {
        controller.startStudy(minutes);
        status.setText("Study timer running · " + minutes + " minutes");
        timerPanel.showRemaining(minutes * 60L);
        contentTabs.setSelectedComponent(timerPanel);
        if (studyDisplayTimer != null) studyDisplayTimer.stop();
        studyDisplayTimer = new javax.swing.Timer(1000, event -> {
            long remaining = controller.remainingStudySeconds();
            timerPanel.showRemaining(remaining);
            status.setText("Study timer · " + remaining / 60 + ":"
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
            timerPanel.showIdle();
            timerRecords.refresh();
            JOptionPane.showMessageDialog(this, "Saved " + session.seconds() + " seconds for " + session.topic());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showTimerRecords() {
        timerRecords.refresh();
        contentTabs.setSelectedComponent(timerRecords);
    }

    private void showWeakTopics() {
        Course course = chooseCourse();
        runAi("Analyzing weak topics", () -> {
            StringBuilder result = new StringBuilder("Quiz accuracy by topic:\n");
            var statistics = controller.topicStatistics(course.id());
            if (statistics.isEmpty()) result.append("No quiz or self-assessment results yet.\n");
            for (var topic : statistics) result.append(topic.topic()).append(": ")
                    .append(topic.accuracyPercent() == null ? "no quiz answers"
                            : topic.accuracyPercent() + "% (" + topic.correct() + "/" + topic.total() + ")")
                    .append(topic.confidence() == null ? "" : ", confidence " + topic.confidence() + "/5")
                    .append('\n');
            return result.append("\n").append(controller.weakTopicAdvice(course.id())).toString();
        },
                advice -> showText("Weak topics · " + course.name(), advice));
    }

    private void showReferences() {
        Deadline deadline;
        if (table.getSelectedRow() >= 0) deadline = selectedDeadline();
        else {
            Course course = selectedCourseId == null ? chooseCourse() : controller.course(selectedCourseId);
            deadline = choose("Choose deadline", controller.schedule(AgentController.Period.ALL,
                    course.id(), "", LocalDate.now()));
        }
        runAi("Finding reference files", () -> controller.suggestReferences(deadline.id()), files -> {
            if (files.isEmpty()) { showText("Reference files", "No matching uploaded files were found."); return; }
            MaterialFile selected = choose("References for " + deadline.title(), files);
            showMaterial(selected);
        });
    }

    private void requestScheduleChange() {
        String request = ask("Describe the schedule change", "");
        requestScheduleChange(request);
    }

    private void requestScheduleChange(String request) {
        Course course = selectedCourseId == null ? chooseCourse() : controller.course(selectedCourseId);
        runAi("Planning schedule change", () -> controller.planScheduleChange(course.id(), request),
                proposal -> {
                    chatPanel.append("Schedule proposal: " + proposal.description());
                    reviewProposal(proposal);
                });
    }

    private MaterialFile chooseReadableMaterial() {
        Object selected = selectedTreeValue();
        if (selected instanceof WorkspaceLeaf leaf && leaf.file().textAvailable()) return leaf.file();
        List<MaterialFile> files = controller.materials().stream()
                .filter(m -> m.textAvailable() && (selectedCourseId == null || m.courseId().equals(selectedCourseId)))
                .toList();
        if (files.isEmpty()) throw new IllegalStateException("Upload a readable PDF, PPTX, DOCX or TXT file first.");
        return choose("Choose a readable file", files);
    }

    private void summarizeFile() {
        MaterialFile material = chooseReadableMaterial();
        runAi("Summarizing " + material.originalName(), () -> controller.generateSummary(material.id()),
                summary -> showText("Summary · " + material.originalName(), summary.text()));
    }

    private void showSummaries() {
        MaterialFile material = chooseReadableMaterial();
        try {
            List<StudyArtifacts.Summary> saved = controller.summaries(material.id());
            if (saved.isEmpty()) { showText("Saved summaries", "No saved summary for this file yet."); return; }
            StudyArtifacts.Summary selected = choose("Choose summary", saved);
            showText("Summary · " + material.originalName(), selected.text());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void generateQuiz() {
        List<UUID> materials = chooseQuizMaterials();
        int count = Integer.parseInt(ask("Number of questions (1-10)", "3"));
        Difficulty difficulty = choose("Quiz difficulty", List.of(Difficulty.values()));
        String types = choose("Question types", List.of("Multiple choice", "Essay", "Both"));
        if (types.equals("Both") && count < 2)
            throw new IllegalArgumentException("Choose at least two questions when including both types.");
        runAi("Generating quiz", () -> {
            StudyArtifacts.Quiz mcq = types.equals("Essay") ? null
                    : controller.generateQuiz(materials, types.equals("Both") ? count - 1 : count, difficulty);
            StudyArtifacts.EssayPrompt essay = types.equals("Multiple choice") ? null
                    : controller.generateEssayPrompt(materials);
            return new QuizBuild(mcq, essay);
        }, built -> {
            if (built.multipleChoice() != null) takeQuiz(built.multipleChoice());
            if (built.essay() != null) answerEssayPrompt(built.essay());
        });
    }

    private List<UUID> chooseQuizMaterials() {
        String scope = choose("Quiz scope", List.of("One file", "Lecture week", "All lectures"));
        if (scope.equals("One file")) return List.of(chooseReadableMaterial().id());
        Course course = selectedCourseId == null ? chooseCourse() : controller.course(selectedCourseId);
        List<MaterialFile> lectures = controller.materials().stream()
                .filter(m -> m.courseId().equals(course.id()) && m.category() == Category.LECTURE_MATERIALS
                        && m.textAvailable()).toList();
        if (lectures.isEmpty()) throw new IllegalStateException("Upload readable lecture materials first.");
        if (scope.equals("Lecture week")) {
            List<Integer> weeks = lectures.stream().map(MaterialFile::lectureWeek)
                    .filter(java.util.Objects::nonNull).distinct().sorted().toList();
            if (weeks.isEmpty()) throw new IllegalStateException("Assign lecture weeks to files first.");
            Integer week = choose("Lecture week", weeks);
            lectures = lectures.stream().filter(m -> week.equals(m.lectureWeek())).toList();
        }
        return lectures.stream().map(MaterialFile::id).toList();
    }

    private void takeQuiz() {
        Course course = chooseCourse();
        try { takeQuiz(choose("Choose saved quiz", controller.quizzes(course.id()))); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void takeQuiz(StudyArtifacts.Quiz quiz) {
        List<Integer> answers = new ArrayList<>();
        for (StudyArtifacts.Question question : quiz.questions()) {
            String[] choices = question.choices().toArray(String[]::new);
            int selected = JOptionPane.showOptionDialog(this, question.text(), "Quiz · " + question.topic(),
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
            if (selected < 0) throw new CancellationException();
            answers.add(selected);
        }
        try {
            StudyArtifacts.Attempt result = controller.gradeQuiz(quiz, answers);
            StringBuilder review = new StringBuilder(result.correct() + "/" + quiz.questions().size() + " correct\n\n");
            for (int i = 0; i < quiz.questions().size(); i++) {
                StudyArtifacts.Question question = quiz.questions().get(i);
                review.append(i + 1).append(". ").append(question.text()).append("\nYour answer: ")
                        .append(question.choices().get(answers.get(i))).append("\nCorrect: ")
                        .append(question.choices().get(question.answerIndex())).append("\n")
                        .append(question.explanation()).append("\n\n");
            }
            showText("Quiz result", review.toString());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void generateEssayQuestion() {
        MaterialFile material = chooseReadableMaterial();
        runAi("Generating essay question", () -> controller.generateEssayPrompt(material.id()),
                prompt -> showText("Essay question saved", prompt.question()));
    }

    private void answerEssayQuestion() {
        Course course = chooseCourse();
        try {
            var prompt = choose("Choose essay question", controller.essayPrompts(course.id()));
            answerEssayPrompt(prompt);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void answerEssayPrompt(StudyArtifacts.EssayPrompt prompt) {
        JTextArea answerArea = new JTextArea(12, 55);
        int choice = JOptionPane.showConfirmDialog(this, new Object[] {prompt.question(),
                new JScrollPane(answerArea)}, "Write essay answer", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) throw new CancellationException();
        String answer = answerArea.getText().trim();
        if (answer.isBlank()) throw new IllegalArgumentException("Enter an essay answer.");
        runAi("Reviewing essay answer", () -> controller.suggestEssayGrade(
                prompt.materialIds(), prompt.question(), answer), draft -> {
            String revised = ask("Proposed score (0-100). Change it before saving if needed.\n"
                    + draft.feedback(), Integer.toString(draft.score()));
            CommandInvoker.Proposal proposal = controller.proposeEssayGrade(draft, Integer.parseInt(revised));
            reviewProposal(proposal);
        });
    }

    private void showText(String title, String content) {
        JTextArea area = new JTextArea(content, 18, 65);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void askSources() {
        String question = ask("Question about uploaded materials", "");
        askSources(question);
    }

    private void askSources(String question) {
        Course course = selectedCourseId == null ? chooseCourse() : controller.course(selectedCourseId);
        runAi("Searching course materials", () -> controller.answerQuestion(course.id(), question), answer -> {
            String citations = answer.citations().stream().map(c -> c.fileName() + " p." + c.page())
                    .collect(java.util.stream.Collectors.joining("\n"));
            chatPanel.append("Agent: " + answer.answer() + "\nSources: " + citations);
            int action = JOptionPane.showOptionDialog(this, answer.answer() + "\n\nSources:\n" + citations,
                    "Material answer", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                    new Object[] {"Open cited file", "Close"}, "Close");
            if (action == 0) {
                List<MaterialFile> files = controller.materials().stream()
                        .filter(m -> m.courseId().equals(course.id()) && answer.citations().stream()
                                .anyMatch(c -> c.fileName().equals(m.originalName()))).toList();
                MaterialFile selected = choose("Open a cited source", files);
                try { Desktop.getDesktop().open(controller.materialPath(selected.id()).toFile()); }
                catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
            }
        });
    }

    private void weeklyBrief() {
        Course course = chooseCourse();
        runAi("Preparing weekly briefing", () -> controller.weeklyBriefing(course.id(), LocalDate.now()),
                briefing -> showText("Weekly briefing · " + course.name(), briefing));
    }

    private void showWeeklyBriefIfNeeded() {
        if (controller.courses().isEmpty()) return;
        try {
            if (!controller.weeklyBriefDue(LocalDate.now())) return;
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        runAi("Preparing this week's briefing", () -> {
            StringBuilder briefing = new StringBuilder();
            for (Course course : controller.courses()) briefing.append(course.name()).append("\n")
                    .append(controller.weeklyBriefing(course.id(), LocalDate.now())).append("\n\n");
            controller.markWeeklyBriefShown(LocalDate.now());
            return briefing.toString();
        }, text -> showText("This week's briefing", text));
    }

    private MaterialFile chooseMaterial(Category category) {
        Object selected = selectedTreeValue();
        if (selected instanceof WorkspaceLeaf leaf && leaf.file().category() == category)
            return leaf.file();
        List<MaterialFile> files = controller.materials().stream()
                .filter(m -> m.category() == category
                        && (selectedCourseId == null || m.courseId().equals(selectedCourseId))).toList();
        if (files.isEmpty()) throw new IllegalStateException("Upload a " + category
                + " file to this course first.");
        if (files.size() == 1) return files.get(0);
        return choose("Choose " + category + " file", files);
    }

    private void aiSyllabus() {
        MaterialFile material = chooseMaterial(Category.SYLLABUS);
        runAi("Reading " + material.originalName(),
                () -> controller.suggestSyllabusDeadlines(material.id()), proposals -> {
                    if (proposals.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "No deadlines were found. You can add one with New deadline.",
                                "Syllabus result", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    JTextArea results = new JTextArea(proposals.stream()
                            .map(CommandInvoker.Proposal::description)
                            .collect(java.util.stream.Collectors.joining("\n\n")), 12, 55);
                    results.setEditable(false);
                    results.setLineWrap(true);
                    results.setWrapStyleWord(true);
                    int next = JOptionPane.showOptionDialog(this, new JScrollPane(results),
                            proposals.size() + " deadline proposals", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null,
                            new Object[] {"Review proposals", "Later"}, "Review proposals");
                    if (next == 0) reviewProposals();
                });
    }

    private void aiWeek() {
        MaterialFile material = chooseMaterial(Category.LECTURE_MATERIALS);
        runAi("Matching lecture week for " + material.originalName(),
                () -> controller.suggestLectureWeek(material.id()), proposal -> {
                    int next = JOptionPane.showOptionDialog(this, proposal.description(),
                            "Lecture week proposal", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null,
                            new Object[] {"Review proposal", "Later"}, "Review proposal");
                    if (next == 0) reviewProposals();
                });
    }

    private void reviewProposals() {
        var proposal = choose("Choose a proposal", controller.pendingProposals());
        reviewProposal(proposal);
    }

    private void reviewProposal(CommandInvoker.Proposal proposal) {
        AgentController.SyllabusCandidate candidate = controller.syllabusCandidate(proposal.id());
        Object[] options = candidate == null ? new Object[] {"Approve", "Reject", "Cancel"}
                : controller.hasSyllabusDuplicate(proposal.id())
                        ? new Object[] {"Edit", "Merge duplicate", "Reject", "Cancel"}
                        : new Object[] {"Edit", "Approve", "Reject", "Cancel"};
        JTextArea proposalText = new JTextArea(proposal.description(), 12, 55);
        proposalText.setEditable(false);
        proposalText.setLineWrap(true);
        proposalText.setWrapStyleWord(true);
        proposalText.setCaretPosition(0);
        int decision = JOptionPane.showOptionDialog(this, new JScrollPane(proposalText), "Review AI proposal",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Cancel");
        try {
            if (decision < 0) return;
            String action = options[decision].toString();
            if (action.equals("Edit")) {
                var draft = candidate.draft();
                String title = ask("Deadline title", draft.title());
                String type = ask("Type", draft.type());
                LocalDate due = date("Due date", draft.dueDate() == null ? "" : draft.dueDate().toString(), true);
                String weightText = ask("Weight percent (blank if unknown)",
                        draft.weight() == null ? "" : draft.weight().toString());
                Double weight = weightText.isBlank() ? null : Double.valueOf(weightText);
                String scope = ask("Scope", draft.scope());
                CommandInvoker.Proposal revised = controller.reviseSyllabusCandidate(proposal.id(),
                        new ResponseParser.DeadlineDraft(title, type, due, scope, draft.evidence(), weight));
                reviewProposal(revised);
            } else if (action.equals("Approve") && candidate != null) {
                if (candidate.warnings().isEmpty() || JOptionPane.showConfirmDialog(this,
                        "This candidate has validation warnings. Approve anyway?\n"
                                + String.join("\n", candidate.warnings()), "Confirm warnings",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    controller.approveProposal(proposal.id());
            } else if (action.equals("Approve")) controller.approveProposal(proposal.id());
            else if (action.equals("Merge duplicate")) {
                controller.mergeSyllabusCandidate(proposal.id());
                controller.approveProposal(proposal.id());
            } else if (action.equals("Reject")) controller.rejectProposal(proposal.id());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showHistory() {
        try {
            List<InteractionHistory.Entry> entries = controller.history();
            if (entries.isEmpty()) { showText("Agent history", "No interactions yet."); return; }
            InteractionHistory.Entry selected = choose("Select a history record", entries);
            int action = JOptionPane.showOptionDialog(this,
                    selected.time() + "\n" + selected.event() + "\n\n" + selected.detail(),
                    "History record", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                    new Object[] {"Delete this record", "Close"}, "Close");
            if (action == 0 && JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm deletion",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) controller.deleteHistory(selected);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void selfAssess() {
        Course course = chooseCourse();
        String topic = ask("Topic to assess", "");
        int confidence = Integer.parseInt(ask("Confidence from 1 to 5", "3"));
        try {
            controller.saveSelfAssessment(course.id(), topic, confidence);
            JOptionPane.showMessageDialog(this, "Saved. Use Study → Weak topics to view guidance.");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private Deadline selectedDeadline() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) throw new IllegalStateException("Select a deadline in the table first.");
        return controller.deadline(rowIds.get(table.convertRowIndexToModel(selectedRow)));
    }

    private void editDeadline() {
        editDeadline(selectedDeadline());
    }

    private void editDeadline(Deadline old) {
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
        deleteDeadline(selectedDeadline());
    }

    private void deleteDeadline(Deadline old) {
        int choice = JOptionPane.showConfirmDialog(this, "Delete " + old.title() + "?", "Confirm deletion",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            controller.deleteDeadline(old.id());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showDeadlineDetails(Deadline deadline) {
        int choice = JOptionPane.showOptionDialog(this,
                deadline.title() + "\n" + controller.course(deadline.courseId()).name()
                        + " · " + deadline.type() + "\nDue: "
                        + (deadline.dueDate() == null ? "TBD" : deadline.dueDate())
                        + "\nWeight: " + (deadline.weight() == null ? "Unknown" : deadline.weight() + "%")
                        + "\nScope: " + deadline.scope()
                        + (deadline.sourceText().isBlank() ? "" : "\nSource: " + deadline.sourceText()),
                "Deadline", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[] {"Edit", "Toggle completed", "Delete", "Close"}, "Close");
        if (choice == 0) perform(() -> editDeadline(deadline));
        else if (choice == 1) perform(() -> {
            try { controller.setCompleted(deadline.id(), !deadline.completed()); }
            catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        });
        else if (choice == 2) perform(() -> deleteDeadline(deadline));
    }

    private void openUndatedDeadline() {
        List<Deadline> undated = controller.schedule(AgentController.Period.ALL, selectedCourseId, "",
                LocalDate.now()).stream().filter(d -> d.dueDate() == null).toList();
        if (undated.isEmpty()) return;
        Deadline selected = choose("Undated deadline", undated);
        selectedCourseId = selected.courseId();
        refresh();
        showDeadlineDetails(selected);
    }

    private void selectionChanged(TreeSelectionEvent event) {
        Object value = selectedTreeValue();
        if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.COURSE) {
            selectedCourseId = branch.id();
            refreshSchedule();
            refreshCalendar();
            showCourseFiles(controller.course(branch.id()));
            coursePage.showCourse(controller.course(branch.id()));
            contentTabs.setSelectedIndex(3);
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.CATEGORY) {
            selectedCourseId = branch.id();
            refreshSchedule();
            refreshCalendar();
            showCategoryFiles(controller.course(branch.id()), categoryOf(branch));
            contentTabs.setSelectedIndex(2);
        } else if (value instanceof WorkspaceLeaf leaf) {
            selectedCourseId = leaf.file().courseId();
            refreshSchedule();
            refreshCalendar();
            showMaterial(leaf.file());
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.SEMESTER) {
            selectedCourseId = null;
            refreshSchedule();
            refreshCalendar();
        }
    }

    private void showCourseFiles(Course course) {
        List<MaterialFile> files = controller.materials().stream()
                .filter(m -> m.courseId().equals(course.id())).toList();
        showFileList(course.name(), "Course files", files);
    }

    private void showCategoryFiles(Course course, Category category) {
        List<MaterialFile> files = controller.materials().stream()
                .filter(m -> m.courseId().equals(course.id()) && m.category() == category).toList();
        showFileList(course.name() + " / " + category, "Right-click this folder to upload files", files);
    }

    private void showFileList(String heading, String hint, List<MaterialFile> files) {
        fileHeading.setText(heading);
        fileMeta.setText(files.size() + " file" + (files.size() == 1 ? "" : "s") + " · " + hint);
        openFileButton.setEnabled(false);
        filePreview.setText(files.isEmpty() ? "No files here yet. Right-click the folder to upload one or more files."
                : files.stream().map(m -> m.originalName() + (m.textAvailable() ? " · text ready"
                        : " · no extracted text")).collect(java.util.stream.Collectors.joining("\n")));
        filePreview.setCaretPosition(0);
    }

    private void showMaterial(MaterialFile material) {
        Course course = controller.course(material.courseId());
        fileHeading.setText(material.originalName());
        fileMeta.setText(course.name() + " / " + material.category() + " · "
                + (material.textAvailable() ? "Text extracted locally" : "No extracted text"));
        openFileButton.setEnabled(true);
        String preview;
        if (!material.textAvailable()) {
            preview = material.extractionWarning().isBlank() ? "The original file is stored and can be opened."
                    : material.extractionWarning() + "\n\nThe original file is stored and can be opened.";
        } else {
            try {
                List<String> pages = controller.materialPages(material.id());
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < pages.size() && text.length() < 8000; i++) {
                    text.append("Page ").append(i + 1).append("\n").append(pages.get(i)).append("\n\n");
                }
                preview = text.length() > 8000 ? text.substring(0, 8000) + "\n… Preview shortened."
                        : text.toString();
            } catch (java.io.IOException ex) {
                preview = "Unable to read extracted text: " + ex.getMessage();
            }
        }
        filePreview.setText(preview);
        filePreview.setCaretPosition(0);
        contentTabs.setSelectedIndex(2);
        status.setText(material.originalName() + " stored locally · "
                + (material.textAvailable() ? "Text preview ready without AI" : "No extractable text"));
    }

    private void selectMaterial(UUID id) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        var nodes = root.breadthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof WorkspaceLeaf leaf && leaf.file().id().equals(id)) {
                TreePath path = new TreePath(node.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return;
            }
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
        Object before = selectedTreeValue();
        UUID selectedMaterial = before instanceof WorkspaceLeaf leaf ? leaf.file().id() : null;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Workspace");
        for (WorkspaceNode node : controller.workspace()) root.add(toSwingNode(node));
        tree.setModel(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        if (selectedMaterial != null) {
            selectMaterial(selectedMaterial);
        } else if (selectedCourseId != null) {
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
        refreshCalendar();
        if (selectedCourseId != null) coursePage.showCourse(controller.course(selectedCourseId));
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
        List<Deadline> undated = deadlines.stream().filter(d -> d.dueDate() == null).toList();
        Object selected = selectedTreeValue();
        if (selected instanceof WorkspaceLeaf leaf) {
            status.setText(leaf.file().originalName() + " stored locally · "
                    + (leaf.file().textAvailable() ? "Text preview ready without AI" : "No extractable text"));
        } else if (selectedCourseId == null) {
            status.setText(deadlines.isEmpty() ? "No deadlines yet. Use New deadline to add one."
                    : "All courses | " + deadlines.size() + " deadlines | " + undated.size()
                            + " without dates | Select a course to see progress");
        } else {
            Course course = controller.course(selectedCourseId);
            AgentController.Progress p = controller.progress(selectedCourseId, LocalDate.now());
            status.setText(course.name() + " | " + p.completed() + "/" + p.total()
                    + " completed | " + p.overdue() + " overdue"
                    + (undated.isEmpty() ? "" : " | Undated: " + undated.stream()
                            .map(Deadline::title).collect(java.util.stream.Collectors.joining(", ")))
                    + (deadlines.isEmpty() ? " | No deadlines for this course yet" : ""));
        }
    }
}
