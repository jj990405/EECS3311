package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.Icon;
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
import javax.swing.tree.DefaultTreeCellRenderer;
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
    private final JTabbedPane contentTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final JTabbedPane viewerTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final JSplitPane centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private final JPanel mainArea = new JPanel(new BorderLayout());
    private JPanel calendarTab;
    private JPanel filePanel;
    private boolean viewerOpen;
    private int restoredDivider = 480;
    private boolean viewerExpanded;
    private final CoursePage coursePage;
    private final TimerPanel timerPanel;
    private final TimerRecordPanel timerRecords;
    private final ChatPanel chatPanel;
    private final JPanel calendarHost = new JPanel(new BorderLayout());
    private final JLabel fileHeading = new JLabel("Select a file in the workspace");
    private final JLabel fileMeta = new JLabel("Uploaded files are stored locally.");
    private final JTextArea filePreview = new JTextArea();
    private final Map<UUID, Component> openDocuments = new java.util.HashMap<>();
    private final Map<UUID, Component> openNotes = new java.util.HashMap<>();
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
        setSize(1460, 900);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(AppTheme.BACKGROUND);

        setJMenuBar(createMenuBar());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.SURFACE);
        header.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                AppTheme.padding(3, 12, 3, 12)));
        JLabel appName = new JLabel("Study Agent");
        appName.setFont(AppTheme.SECTION);
        appName.setForeground(AppTheme.TEXT);
        header.add(appName, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        button(actions, "New semester", this::addSemester);
        button(actions, "New course", this::addCourse);
        button(actions, "Add files", this::uploadFiles);
        button(actions, "New deadline", this::addDeadline);
        button(actions, "Calendar", this::showCalendar);
        JButton sync = AppTheme.button("Analyze files for calendar", true);
        sync.addActionListener(e -> perform(this::analyzeCalendarFiles));
        actions.add(sync);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree target, Object value,
                    boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
                super.getTreeCellRendererComponent(target, value, selected, expanded, leaf, row, focused);
                if (value instanceof DefaultMutableTreeNode node
                        && node.getUserObject() instanceof WorkspaceBranch branch
                        && branch.kind() == WorkspaceBranch.Kind.COURSE) {
                    setIcon(courseIcon(getIcon(), Color.decode(controller.course(branch.id()).colorHex())));
                }
                return this;
            }
        });
        tree.setBackground(AppTheme.SURFACE);
        tree.setForeground(AppTheme.TEXT);
        tree.setRowHeight(26);
        tree.addTreeSelectionListener(this::selectionChanged);
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent event) { showTreeMenu(event); }
            @Override public void mouseReleased(java.awt.event.MouseEvent event) { showTreeMenu(event); }
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                TreePath path = tree.getPathForLocation(event.getX(), event.getY());
                if (path == null) return;
                Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                if (event.getClickCount() == 2 && value instanceof WorkspaceLeaf) {
                    openSelectedFile();
                } else if (value instanceof WorkspaceLeaf leaf) {
                    perform(() -> showMaterial(leaf.file()));
                } else if (value instanceof WorkspaceBranch branch
                        && branch.kind() == WorkspaceBranch.Kind.CATEGORY) {
                    perform(() -> showCategoryFiles(controller.course(branch.id()), categoryOf(branch)));
                } else if (value instanceof WorkspaceBranch branch
                        && branch.kind() == WorkspaceBranch.Kind.COURSE) {
                    perform(() -> showCourseFiles(controller.course(branch.id())));
                }
            }
        });
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(32);
        table.setBackground(AppTheme.SURFACE);
        table.setForeground(AppTheme.TEXT);
        table.setSelectionBackground(AppTheme.BLUE_TINT);
        table.setSelectionForeground(AppTheme.TEXT);
        table.setGridColor(AppTheme.BORDER);
        table.getTableHeader().setBackground(AppTheme.RAISED);
        table.getTableHeader().setForeground(AppTheme.TEXT);
        contentTabs.addTab("Timeline", new JScrollPane(table));
        calendarTab = new JPanel(new BorderLayout(0, 8));
        JPanel monthActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(monthActions, "Previous month", () -> changeMonth(-1));
        button(monthActions, "Today", () -> { visibleMonth = YearMonth.now(); refreshCalendar(); });
        button(monthActions, "Next month", () -> changeMonth(1));
        button(monthActions, "Expand / restore", this::toggleViewer);
        calendarTab.add(monthActions, BorderLayout.NORTH);
        calendarTab.add(calendarHost, BorderLayout.CENTER);
        filePanel = createFilePanel();
        contentTabs.addTab("Course", coursePage);
        contentTabs.addTab("Study timer", timerPanel);
        contentTabs.addTab("Study records", timerRecords);
        contentTabs.addTab("Chat", chatPanel);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(AppTheme.SURFACE);
        JLabel workspaceHeading = new JLabel("WORKSPACE");
        workspaceHeading.setFont(AppTheme.SMALL.deriveFont(java.awt.Font.BOLD));
        workspaceHeading.setForeground(AppTheme.MUTED);
        workspaceHeading.setBorder(AppTheme.padding(18, 18, 12, 18));
        workspace.add(workspaceHeading, BorderLayout.NORTH);
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(null);
        treeScroll.getViewport().setBackground(AppTheme.SURFACE);
        workspace.add(treeScroll, BorderLayout.CENTER);
        JButton clear = AppTheme.button("Delete all local data...", false);
        clear.setBackground(new Color(0x5A2929));
        clear.setForeground(Color.WHITE);
        clear.setToolTipText("Delete courses, files, deadlines, study records and history from this device");
        clear.addActionListener(e -> perform(this::clearAllData));
        JPanel danger = new JPanel(new BorderLayout());
        danger.setBackground(AppTheme.SURFACE);
        danger.setBorder(AppTheme.padding(10, 12, 10, 12));
        danger.add(clear, BorderLayout.CENTER);
        workspace.add(danger, BorderLayout.SOUTH);
        contentTabs.setBackground(AppTheme.SURFACE);
        contentTabs.setForeground(AppTheme.TEXT);
        contentTabs.setMinimumSize(new Dimension(300, 0));
        viewerTabs.setBackground(AppTheme.SURFACE);
        viewerTabs.setForeground(AppTheme.TEXT);
        viewerTabs.setMinimumSize(new Dimension(320, 0));
        centerRightSplit.setRightComponent(viewerTabs);
        centerRightSplit.setBorder(null);
        centerRightSplit.setDividerSize(12);
        centerRightSplit.setContinuousLayout(true);
        centerRightSplit.setResizeWeight(0.38);
        mainArea.add(contentTabs, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, workspace, mainArea);
        split.setBorder(null);
        split.setDividerSize(10);
        split.setContinuousLayout(true);
        split.setDividerLocation(245);
        add(split, BorderLayout.CENTER);
        status.setForeground(AppTheme.MUTED);
        status.setOpaque(true);
        status.setBackground(AppTheme.SURFACE);
        status.setBorder(AppTheme.padding(8, 16, 8, 16));
        status.setToolTipText("Click to inspect undated deadlines");
        status.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                perform(MainWindow.this::openUndatedDeadline);
            }
        });
        add(status, BorderLayout.SOUTH);

        controller.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));
        refresh();
        javax.swing.Timer notificationTimer = new javax.swing.Timer(60_000, event -> {
            showDueNotifications();
            perform(this::showUndatedReminders);
        });
        notificationTimer.start();
        SwingUtilities.invokeLater(() -> {
            showDueNotifications();
            perform(this::showUndatedReminders);
        });
        SwingUtilities.invokeLater(this::showWeeklyBriefIfNeeded);
    }

    private void button(JPanel parent, String title, Runnable action) {
        JButton button = AppTheme.button(title, false);
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
        workspace.addSeparator();
        menuItem(workspace, "Delete all local data...", this::clearAllData);
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
        menuItem(ai, "Create study notes", this::summarizeFile);
        menuItem(ai, "Saved study notes", this::showSummaries);
        menuItem(ai, "Generate quiz", this::generateQuiz);
        menuItem(ai, "Take saved quiz", this::takeQuiz);
        menuItem(ai, "Generate essay question", this::generateEssayQuestion);
        menuItem(ai, "Answer essay question", this::answerEssayQuestion);
        menuItem(ai, "Extract syllabus deadlines", this::aiSyllabus);
        menuItem(ai, "Analyze uploaded files for calendar", this::analyzeCalendarFiles);
        menuItem(ai, "Classify uploaded files", this::classifyFilesWithAi);
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
                popupItem(menu, "Upload and sort files...", () -> uploadFiles(course, null));
                popupItem(menu, "New deadline...", () -> addDeadline(course));
                popupItem(menu, "Change course color...", () -> changeCourseColor(course));
                popupItem(menu, "View calendar", this::showCalendar);
            } else {
                Course course = controller.course(branch.id());
                Category category = categoryOf(branch);
                popupItem(menu, "Upload files to " + category + "...", () -> uploadFiles(course, category));
            }
        } else if (value instanceof WorkspaceLeaf leaf) {
            popupItem(menu, "Show file details", () -> showMaterial(leaf.file()));
            popupItem(menu, "Open original", this::openSelectedFile);
            popupItem(menu, "Move to another folder...", () -> moveMaterial(leaf.file()));
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
        JButton classify = AppTheme.button("Classify files with AI", false);
        classify.addActionListener(e -> perform(this::classifyFilesWithAi));
        controls.add(classify);
        button(controls, "Expand / restore", this::toggleViewer);
        heading.add(controls, BorderLayout.SOUTH);
        panel.add(heading, BorderLayout.NORTH);
        filePreview.setEditable(false);
        filePreview.setLineWrap(true);
        filePreview.setWrapStyleWord(true);
        filePreview.setText("Choose a file on the left. PDFs open here as original pages.\n\n"
                + "Text is extracted separately for search and AI, without changing the stored original.");
        filePreview.setBorder(AppTheme.padding(20, 20, 20, 20));
        panel.add(new JScrollPane(filePreview), BorderLayout.CENTER);
        return panel;
    }

    private void showViewerTab(String title, Component component, Runnable onClose) {
        if (viewerTabs.indexOfComponent(component) < 0) {
            viewerTabs.addTab(title, component);
            int index = viewerTabs.indexOfComponent(component);
            JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            tab.setOpaque(false);
            JButton select = new JButton(title.length() > 30 ? title.substring(0, 27) + "…" : title);
            select.setToolTipText("Show " + title);
            select.setContentAreaFilled(false);
            select.setBorder(AppTheme.padding(2, 4, 2, 4));
            select.addActionListener(e -> viewerTabs.setSelectedComponent(component));
            tab.add(select);
            JButton close = new JButton("×");
            close.setToolTipText("Close " + title);
            close.setBorder(AppTheme.padding(2, 4, 2, 4));
            close.addActionListener(e -> {
                viewerTabs.remove(component);
                onClose.run();
                if (viewerTabs.getTabCount() == 0) hideViewer();
            });
            tab.add(close);
            viewerTabs.setTabComponentAt(index, tab);
        }
        if (!viewerOpen) {
            mainArea.remove(contentTabs);
            centerRightSplit.setLeftComponent(contentTabs);
            mainArea.add(centerRightSplit, BorderLayout.CENTER);
            viewerOpen = true;
            mainArea.revalidate();
            mainArea.repaint();
            SwingUtilities.invokeLater(() -> centerRightSplit.setDividerLocation(0.38));
        }
        viewerTabs.setSelectedComponent(component);
    }

    private void hideViewer() {
        if (!viewerOpen) return;
        mainArea.remove(centerRightSplit);
        centerRightSplit.setLeftComponent(null);
        mainArea.add(contentTabs, BorderLayout.CENTER);
        viewerOpen = false;
        viewerExpanded = false;
        mainArea.revalidate();
        mainArea.repaint();
    }

    private void changeMonth(int offset) {
        visibleMonth = visibleMonth.plusMonths(offset);
        refreshCalendar();
    }

    private void toggleViewer() {
        if (viewerExpanded) {
            centerRightSplit.setDividerLocation(restoredDivider);
        } else {
            restoredDivider = Math.max(250, centerRightSplit.getDividerLocation());
            centerRightSplit.setDividerLocation(0);
        }
        viewerExpanded = !viewerExpanded;
    }

    private void refreshCalendar() {
        Map<UUID, Course> courses = controller.courses().stream()
                .collect(java.util.stream.Collectors.toMap(Course::id, c -> c));
        calendarHost.removeAll();
        calendarHost.add(new CalendarPanel(visibleMonth, LocalDate.now(),
                controller.schedule(AgentController.Period.ALL, null, "", LocalDate.now()), courses,
                this::showDeadlineDetails, date -> perform(() -> showCalendarDay(date))),
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
        return DatePickerDialog.show(this, label, initial, optional);
    }

    private Icon courseIcon(Icon folder, Color color) {
        return new Icon() {
            @Override public int getIconWidth() { return 12 + (folder == null ? 0 : folder.getIconWidth()); }
            @Override public int getIconHeight() { return Math.max(12, folder == null ? 0 : folder.getIconHeight()); }
            @Override public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
                graphics.setColor(color);
                graphics.fillOval(x + 1, y + (getIconHeight() - 9) / 2, 9, 9);
                graphics.setColor(Color.DARK_GRAY);
                graphics.drawOval(x + 1, y + (getIconHeight() - 9) / 2, 9, 9);
                if (folder != null) folder.paintIcon(component, graphics, x + 12, y);
            }
        };
    }

    private String chooseCourseColor(String current) {
        List<CourseColors.Choice> choices = new ArrayList<>(List.of(CourseColors.PALETTE));
        if (choices.stream().noneMatch(choice -> choice.hex().equalsIgnoreCase(current)))
            choices.add(new CourseColors.Choice("Current", current));
        JPanel palette = new JPanel(new GridLayout(0, 4, 10, 10));
        ButtonGroup group = new ButtonGroup();
        JRadioButton[] options = new JRadioButton[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            CourseColors.Choice choice = choices.get(i);
            JRadioButton option = new JRadioButton(choice.name());
            Color swatch = Color.decode(choice.hex());
            option.setIcon(swatchIcon(swatch, false));
            option.setSelectedIcon(swatchIcon(swatch, true));
            option.setIconTextGap(6);
            group.add(option);
            palette.add(option);
            options[i] = option;
            if (choice.hex().equalsIgnoreCase(current)) option.setSelected(true);
        }
        int result = JOptionPane.showConfirmDialog(this, palette, "Course color",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) throw new CancellationException();
        for (int i = 0; i < options.length; i++)
            if (options[i].isSelected()) return choices.get(i).hex();
        throw new IllegalStateException("Choose a color.");
    }

    private javax.swing.Icon swatchIcon(Color color, boolean selected) {
        return new javax.swing.Icon() {
            @Override public int getIconWidth() { return 22; }
            @Override public int getIconHeight() { return 22; }
            @Override public void paintIcon(java.awt.Component component, java.awt.Graphics graphics,
                                            int x, int y) {
                graphics.setColor(color);
                graphics.fillRoundRect(x + 2, y + 2, 18, 18, 5, 5);
                graphics.setColor(Color.DARK_GRAY);
                graphics.drawRoundRect(x + 2, y + 2, 18, 18, 5, 5);
                if (selected) {
                    graphics.setColor(Color.WHITE);
                    graphics.drawLine(x + 6, y + 11, x + 10, y + 15);
                    graphics.drawLine(x + 10, y + 15, x + 17, y + 7);
                }
            }
        };
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
        int longest = choices.stream().mapToInt(choice -> displayName(choice).length()).max().orElse(0);
        box.setPreferredSize(new Dimension(Math.min(480, Math.max(190, 14 + longest * 7)), 30));
        int answer = JOptionPane.showConfirmDialog(this, box, label, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) throw new CancellationException();
        return (T) box.getSelectedItem();
    }

    private String displayName(Object value) {
        if (value instanceof MaterialFile material)
            return controller.course(material.courseId()).name() + " / " + material.originalName();
        if (value instanceof StudyArtifacts.Quiz quiz)
            return quiz.questions().size() + " questions · " + quiz.questions().get(0).topic();
        if (value instanceof Semester semester) return semester.name();
        if (value instanceof Course course) return course.name();
        if (value instanceof CommandInvoker.Proposal proposal) return proposal.description();
        return String.valueOf(value);
    }

    private Semester chooseSemester() {
        List<Semester> semesters = controller.semesters();
        return semesters.size() == 1 ? semesters.get(0) : choose("Choose semester", semesters);
    }

    private Course chooseCourse() {
        List<Course> courses = controller.courses();
        return courses.size() == 1 ? courses.get(0) : choose("Choose course", courses);
    }

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
        String color = chooseCourseColor(CourseColors.suggested(controller.courses().size()).hex());
        JComboBox<String> difficultyBox = new JComboBox<>(new String[] {"Easy", "Medium", "Hard"});
        difficultyBox.setSelectedItem("Medium");
        difficultyBox.setPreferredSize(new Dimension(160, 28));
        int answer = JOptionPane.showConfirmDialog(this, difficultyBox, "Course difficulty",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) throw new CancellationException();
        String level = (String) difficultyBox.getSelectedItem();
        Difficulty difficulty = switch (level) {
            case "Easy" -> Difficulty.LOW;
            case "Hard" -> Difficulty.HIGH;
            default -> Difficulty.MEDIUM;
        };
        try {
            controller.createCourse(semester.id(), name, color, difficulty);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void changeCourseColor(Course course) {
        String color = chooseCourseColor(course.colorHex());
        try { controller.changeCourseColor(course.id(), color); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void uploadFiles() {
        Object value = selectedTreeValue();
        if (value instanceof WorkspaceLeaf leaf) {
            uploadFiles(controller.course(leaf.file().courseId()), leaf.file().category());
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.CATEGORY) {
            uploadFiles(controller.course(branch.id()), categoryOf(branch));
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.COURSE) {
            uploadFiles(controller.course(branch.id()), null);
        } else {
            uploadFiles(chooseCourse(), null);
        }
    }

    private Object selectedTreeValue() {
        Object selected = tree.getLastSelectedPathComponent();
        return selected instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
    }

    private void uploadFiles(Course course, Category category) {
        JFileChooser chooser = new JFileChooser(lastUploadDirectory);
        chooser.setDialogTitle("Choose files for " + course.name());
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        if (files.length == 0) return;
        lastUploadDirectory = chooser.getCurrentDirectory().getAbsolutePath();
        List<Category> destinations = reviewUploadDestinations(files, category);
        if (destinations == null) return;
        int added = 0;
        MaterialFile last = null;
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            Category destination = destinations.get(i);
            try {
                boolean duplicate = controller.materials().stream().anyMatch(m -> m.courseId().equals(course.id())
                        && m.category() == destination && m.originalName().equalsIgnoreCase(file.getName()));
                boolean replace = false;
                if (duplicate) {
                    int choice = JOptionPane.showOptionDialog(this,
                            file.getName() + " already exists in " + destination + ". Replace it?",
                            "Duplicate file", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            new Object[] {"Replace", "Skip", "Stop upload"}, "Skip");
                    if (choice == 2 || choice < 0) break;
                    if (choice == 1) continue;
                    replace = true;
                }
                MaterialFile material = controller.uploadMaterial(course.id(), destination, file.toPath(), replace);
                added++;
                last = material;
                if (!material.extractionWarning().isBlank())
                    problems.add(file.getName() + ": " + material.extractionWarning());
            } catch (Exception ex) {
                problems.add(file.getName() + ": " + ex.getMessage());
            }
        }
        refresh();
        if (last != null) selectMaterial(last.id());
        status.setText(added + " file" + (added == 1 ? "" : "s") + " added to " + course.name()
                + (problems.isEmpty() ? "" : " | " + problems.size() + " need attention"));
        if (!problems.isEmpty()) JOptionPane.showMessageDialog(this, String.join("\n", problems),
                "Upload details", JOptionPane.WARNING_MESSAGE);
    }

    private List<Category> reviewUploadDestinations(File[] files, Category selectedFolder) {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"File", "Destination folder", "Basis"}, 0) {
            @Override public Class<?> getColumnClass(int column) {
                return column == 1 ? Category.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int column) { return column == 1; }
        };
        for (File file : files) {
            var hint = FileCategorySuggester.suggest(file.getName());
            model.addRow(new Object[] {file.getName(), FileCategorySuggester.defaultDestination(
                    file.getName(), selectedFolder),
                    hint.isPresent() ? "File-name suggestion" : selectedFolder == null
                            ? "No clear hint; review this row" : "Selected folder; review this row"});
        }
        JTable destinations = new JTable(model);
        destinations.setRowHeight(32);
        destinations.getColumnModel().getColumn(0).setPreferredWidth(390);
        destinations.getColumnModel().getColumn(1).setPreferredWidth(190);
        destinations.getColumnModel().getColumn(2).setPreferredWidth(240);
        destinations.getColumnModel().getColumn(1).setCellEditor(
                new javax.swing.DefaultCellEditor(new JComboBox<>(Category.values())));
        JPanel review = new JPanel(new BorderLayout(0, 10));
        review.setBorder(AppTheme.padding(16, 16, 16, 16));
        review.add(new JLabel("Check each destination. Click a folder cell to change it before upload."),
                BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(destinations);
        scroll.setPreferredSize(new Dimension(850, Math.min(460, 65 + files.length * 32)));
        review.add(scroll, BorderLayout.CENTER);
        int choice = JOptionPane.showOptionDialog(this, review, "Sort files before upload",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[] {"Upload to shown folders", "Cancel"}, "Upload to shown folders");
        if (choice != 0) return null;
        if (destinations.isEditing()) destinations.getCellEditor().stopCellEditing();
        List<Category> result = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) result.add((Category) model.getValueAt(row, 1));
        return result;
    }

    private void moveMaterial(MaterialFile file) {
        Category target = choose("Move " + file.originalName() + " to", List.of(Category.values()));
        if (target == file.category()) return;
        try {
            controller.moveMaterial(file.id(), target);
            closeMaterialTab(file.id());
            refresh();
            selectMaterial(file.id());
            status.setText("Moved " + file.originalName() + " to " + target + ".");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void classifyFilesWithAi() {
        runAi("Classifying uploaded files", controller::suggestFileCategories, suggestions -> {
            if (suggestions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No folder changes were suggested.",
                        "File classification", JOptionPane.INFORMATION_MESSAGE);
            } else reviewCategorySuggestions(suggestions, "Review AI folder suggestions");
        });
    }

    private void reviewCategorySuggestions(List<AgentController.CategorySuggestion> hints, String title) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Move", "File", "Current folder", "Suggested folder", "Why"}, 0) {
            @Override public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        };
        List<AgentController.CategorySuggestion> displayed = new ArrayList<>();
        for (var hint : hints) {
            MaterialFile file = controller.materials().stream()
                    .filter(m -> m.id().equals(hint.materialId())).findFirst().orElse(null);
            if (file != null) {
                displayed.add(hint);
                model.addRow(new Object[] {true, file.originalName(), file.category().toString(),
                        hint.category().toString(), hint.reason()});
            }
        }
        JTable table = new JTable(model);
        table.setRowHeight(31);
        table.getColumnModel().getColumn(0).setMaxWidth(54);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        JScrollPane rows = new JScrollPane(table);
        rows.setPreferredSize(new Dimension(950, Math.min(480, 74 + model.getRowCount() * 31)));
        JPanel view = new JPanel(new BorderLayout(0, 10));
        view.setBorder(AppTheme.padding(16, 16, 16, 16));
        view.add(new JLabel("Review the suggestions. Only checked files will move; originals stay unchanged."),
                BorderLayout.NORTH);
        view.add(rows, BorderLayout.CENTER);
        int choice = JOptionPane.showOptionDialog(this, view, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Move checked files", "Keep current folders"},
                "Keep current folders");
        if (choice != 0) return;
        int moved = 0;
        List<String> errors = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (!Boolean.TRUE.equals(model.getValueAt(row, 0))) continue;
            var hint = displayed.get(row);
            try {
                controller.moveMaterial(hint.materialId(), hint.category());
                closeMaterialTab(hint.materialId());
                moved++;
            }
            catch (Exception ex) { errors.add(model.getValueAt(row, 1) + ": " + ex.getMessage()); }
        }
        refresh();
        status.setText(moved + " file" + (moved == 1 ? "" : "s") + " moved to suggested folders.");
        if (!errors.isEmpty()) JOptionPane.showMessageDialog(this, String.join("\n", errors),
                "Some files were not moved", JOptionPane.WARNING_MESSAGE);
    }

    private void clearAllData() {
        if (aiBusy) {
            JOptionPane.showMessageDialog(this, "Wait for the current AI request to finish first.",
                    "Data deletion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String message = "Delete all local study data from this app?\n\n"
                + controller.semesters().size() + " semesters, " + controller.courses().size()
                + " courses, and " + controller.materials().size() + " uploaded files, plus deadlines,"
                + " quizzes, study records and history will be removed.\n\nThis cannot be undone.";
        int choice = JOptionPane.showOptionDialog(this, message, "Delete all local data?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[] {"Delete all data", "Cancel"}, "Cancel");
        if (choice != 0) return;
        try {
            selectedCourseId = null;
            tree.clearSelection();
            controller.clearAllData();
            if (studyDisplayTimer != null) studyDisplayTimer.stop();
            timerPanel.showIdle();
            timerRecords.refresh();
            coursePage.clear();
            chatPanel.clear();
            viewerTabs.removeAll();
            openDocuments.clear();
            openNotes.clear();
            hideViewer();
            fileHeading.setText("Select a file in the workspace");
            fileMeta.setText("Uploaded files are stored locally.");
            filePreview.setText("Create a semester and course to start again.");
            openFileButton.setEnabled(false);
            contentTabs.setSelectedIndex(0);
            refresh();
            status.setText("All local study data deleted. Start with New semester.");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
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
        showViewerTab("Calendar", calendarTab, () -> { });
    }

    private void showCalendarDay(LocalDate day) {
        List<Deadline> items = controller.schedule(AgentController.Period.ALL, null, "",
                LocalDate.now()).stream().filter(d -> day.equals(d.dueDate())).toList();
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel(items.isEmpty() ? "No deadlines on " + day : "Deadlines on " + day),
                BorderLayout.NORTH);
        javax.swing.JList<Deadline> list = new javax.swing.JList<>(items.toArray(Deadline[]::new));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(javax.swing.JList<?> target, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(target, value, index, selected, focused);
                if (value instanceof Deadline deadline) setText("[" + controller.course(deadline.courseId()).name()
                        + "] " + deadline.title());
                return this;
            }
        });
        list.setVisibleRowCount(Math.min(8, Math.max(1, items.size())));
        if (!items.isEmpty()) list.setSelectedIndex(0);
        if (!items.isEmpty()) panel.add(new JScrollPane(list), BorderLayout.CENTER);
        Object[] choices = items.isEmpty() ? new Object[] {"Add deadline", "Close"}
                : new Object[] {"Add deadline", "Edit selected", "Close"};
        int action = JOptionPane.showOptionDialog(this, panel, day.toString(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (action == 0) {
            Course course = chooseCourse();
            try {
                controller.addDeadline(course.id(), ask("Deadline title", ""),
                        ask("Type (e.g. Assignment)", ""), day, ask("Covered topics / scope", ""));
            } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
            refreshCalendar();
        } else if (action == 1 && !items.isEmpty()) {
            Deadline selected = list.getSelectedValue();
            if (selected != null) editDeadline(selected);
        }
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

    private void showUndatedReminders() {
        try {
            LocalDate today = LocalDate.now();
            List<Deadline> remaining = new ArrayList<>(controller.unseenUndatedReminders(today));
            if (remaining.isEmpty()) return;
            controller.markUndatedRemindersShown(remaining, today);
            while (!remaining.isEmpty()) {
                javax.swing.JList<Deadline> list = new javax.swing.JList<>(remaining.toArray(Deadline[]::new));
                list.setCellRenderer(new DefaultListCellRenderer() {
                    @Override public Component getListCellRendererComponent(javax.swing.JList<?> target,
                            Object value, int index, boolean selected, boolean focused) {
                        super.getListCellRendererComponent(target, value, index, selected, focused);
                        if (value instanceof Deadline deadline)
                            setText("[" + controller.course(deadline.courseId()).name() + "] " + deadline.title());
                        return this;
                    }
                });
                list.setVisibleRowCount(Math.min(8, remaining.size()));
                list.setSelectedIndex(0);
                JPanel panel = new JPanel(new BorderLayout(0, 10));
                panel.add(new JLabel("These assessments have no due date. You will be reminded tomorrow."),
                        BorderLayout.NORTH);
                panel.add(new JScrollPane(list), BorderLayout.CENTER);
                int action = JOptionPane.showOptionDialog(this, panel, "Dates still needed",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                        new Object[] {"Set selected date", "Remind tomorrow"}, "Remind tomorrow");
                if (action != 0) return;
                Deadline selected = list.getSelectedValue();
                if (selected == null) return;
                LocalDate due = date("Due date for " + selected.title(), "", false);
                controller.editDeadline(selected.id(), selected.title(), selected.type(), due, selected.scope());
                remaining.remove(selected);
                refresh();
            }
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
                summary -> showStudyNotes(material, summary));
    }

    private void showSummaries() {
        MaterialFile material = chooseReadableMaterial();
        try {
            List<StudyArtifacts.Summary> saved = controller.summaries(material.id());
            if (saved.isEmpty()) { showText("Saved study notes", "No saved study notes for this file yet."); return; }
            StudyArtifacts.Summary selected;
            if (saved.size() == 1) selected = saved.get(0);
            else {
                List<String> labels = new ArrayList<>();
                for (int index = 0; index < saved.size(); index++)
                    labels.add("Study notes " + (index + 1) + " · "
                            + java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")
                            .withZone(java.time.ZoneId.systemDefault()).format(saved.get(index).createdAt()));
                selected = saved.get(labels.indexOf(choose("Choose study notes", labels)));
            }
            showStudyNotes(material, selected);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private void showStudyNotes(MaterialFile material, StudyArtifacts.Summary summary) {
        Component previous = openNotes.remove(material.id());
        if (previous != null) viewerTabs.remove(previous);
        Runnable openSource = material.originalName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                ? () -> showMaterial(material) : () -> openOriginal(material);
        Component notes = new StudyNotesPanel(material.originalName(), summary.text(), openSource,
                () -> runAi("Creating study guide from " + material.originalName(),
                        () -> controller.generateSummary(material.id()),
                        refreshed -> showStudyNotes(material, refreshed)));
        openNotes.put(material.id(), notes);
        showViewerTab("Notes · " + material.originalName(), notes,
                () -> openNotes.remove(material.id()));
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
        List<Integer> answers = QuizDialog.take(this, quiz);
        if (answers == null) return;
        try {
            StudyArtifacts.Attempt result = controller.gradeQuiz(quiz, answers);
            QuizDialog.showResult(this, quiz, answers, result);
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

    private void analyzeCalendarFiles() {
        runAi("Analyzing uploaded syllabus and assignment files",
                controller::suggestCalendarFromFiles, result -> {
                    if (result.proposals().isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "No new deadlines were found in " + result.filesAnalyzed() + " files.",
                                "Calendar analysis", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    reviewCalendarSuggestions(result);
                });
    }

    private void reviewCalendarSuggestions(AgentController.CalendarSuggestions result) {
        Map<UUID, CommandInvoker.Proposal> pending = controller.pendingProposals().stream()
                .collect(java.util.stream.Collectors.toMap(CommandInvoker.Proposal::id, p -> p));
        List<CommandInvoker.Proposal> proposals = result.proposals().stream()
                .map(p -> pending.get(p.id())).filter(java.util.Objects::nonNull).toList();
        if (proposals.isEmpty()) {
            status.setText("All calendar suggestions have been reviewed.");
            return;
        }
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Add", "Course", "Due", "Deadline", "Source", "Review"}, 0) {
            @Override public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        };
        for (CommandInvoker.Proposal proposal : proposals) {
            var candidate = controller.syllabusCandidate(proposal.id());
            var file = controller.materials().stream()
                    .filter(m -> m.id().equals(candidate.materialId())).findFirst().orElseThrow();
            boolean timeOnlyWarning = !candidate.warnings().isEmpty() && candidate.warnings().stream()
                    .allMatch(w -> w.startsWith("Due 24 hours after release;"));
            boolean selectedByDefault = candidate.draft().dueDate() != null
                    && (candidate.warnings().isEmpty() || timeOnlyWarning);
            model.addRow(new Object[] {selectedByDefault,
                    controller.course(candidate.courseId()).name(),
                    candidate.draft().dueDate() == null ? "TBD" : candidate.draft().dueDate().toString(),
                    candidate.draft().title(), file.originalName(),
                    candidate.warnings().isEmpty() ? "Ready"
                            : timeOnlyWarning ? "Confirm time" : "Check warning"});
        }
        JTable suggestions = new JTable(model);
        suggestions.setRowHeight(31);
        suggestions.setAutoCreateRowSorter(true);
        suggestions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        suggestions.getColumnModel().getColumn(0).setMaxWidth(50);
        suggestions.getColumnModel().getColumn(2).setPreferredWidth(105);
        suggestions.getColumnModel().getColumn(3).setPreferredWidth(260);
        suggestions.getColumnModel().getColumn(4).setPreferredWidth(200);
        JTextArea details = new JTextArea("Select a row to see its source wording and warnings.");
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setBorder(AppTheme.padding(12, 12, 12, 12));
        suggestions.getSelectionModel().addListSelectionListener(e -> {
            int row = suggestions.getSelectedRow();
            if (row < 0) return;
            CommandInvoker.Proposal proposal = proposals.get(suggestions.convertRowIndexToModel(row));
            details.setText(proposal.description());
            details.setCaretPosition(0);
        });
        suggestions.setRowSelectionInterval(0, 0);
        JPanel view = new JPanel(new BorderLayout(0, 12));
        view.setBorder(AppTheme.padding(18, 18, 18, 18));
        JLabel introduction = new JLabel(result.filesAnalyzed() + " files analyzed · " + proposals.size()
                + " suggestions. Check the dates and source before adding them to your calendar.");
        introduction.setForeground(AppTheme.TEXT);
        view.add(introduction, BorderLayout.NORTH);
        JScrollPane rows = new JScrollPane(suggestions);
        rows.setPreferredSize(new Dimension(930, 340));
        view.add(rows, BorderLayout.CENTER);
        JScrollPane source = new JScrollPane(details);
        source.setPreferredSize(new Dimension(930, 130));
        view.add(source, BorderLayout.SOUTH);
        int decision = JOptionPane.showOptionDialog(this, view, "Review calendar suggestions",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[] {"Add all", "Add checked", "Review selected", "Later"}, "Later");
        if (decision == 2) {
            int row = suggestions.getSelectedRow();
            if (row >= 0) {
                reviewProposal(proposals.get(suggestions.convertRowIndexToModel(row)));
                reviewCalendarSuggestions(result);
            }
            return;
        }
        if (decision == 3 || decision < 0) return;
        List<CommandInvoker.Proposal> selected = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++)
            if (decision == 0 || Boolean.TRUE.equals(model.getValueAt(row, 0))) selected.add(proposals.get(row));
        addCalendarSuggestions(selected);
    }

    private void addCalendarSuggestions(List<CommandInvoker.Proposal> selected) {
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one suggestion to add.",
                    "Calendar suggestions", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<CommandInvoker.Proposal> ready = new ArrayList<>();
        List<AgentController.SyllabusCandidate> prepared = new ArrayList<>();
        int skippedDuplicates = 0;
        int skippedByUser = 0;
        for (CommandInvoker.Proposal proposal : selected) {
            var candidate = controller.syllabusCandidate(proposal.id());
            var draft = candidate.draft();
            if (draft.dueDate() == null) {
                int choice = JOptionPane.showOptionDialog(this,
                        "[" + controller.course(candidate.courseId()).name() + "] " + draft.title()
                                + " has no confirmed due date.", "Date needed",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new Object[] {"Enter date", "Keep TBD · remind daily", "Skip this item"}, "Enter date");
                if (choice < 0) return;
                if (choice == 2) { skippedByUser++; continue; }
                if (choice == 0) {
                    LocalDate due = date("Due date for " + draft.title(), "", false);
                    Semester semester = controller.semester(controller.course(candidate.courseId()).semesterId());
                    if (!semester.contains(due)) throw new IllegalArgumentException(
                            "Date must be within " + semester.startDate() + " to " + semester.endDate() + ".");
                    try { controller.reviseSyllabusCandidate(proposal.id(), new ResponseParser.DeadlineDraft(
                            draft.title(), draft.type(), due, draft.scope(), draft.evidence(), draft.weight())); }
                    catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
                }
            }
            candidate = controller.syllabusCandidate(proposal.id());
            draft = candidate.draft();
            AgentController.SyllabusCandidate current = candidate;
            boolean repeatedInBatch = prepared.stream().anyMatch(previous ->
                    previous.courseId().equals(current.courseId())
                            && java.util.Objects.equals(previous.draft().dueDate(), current.draft().dueDate())
                            && DeadlineTitleMatcher.same(previous.draft().title(), current.draft().title()));
            if (controller.hasSyllabusDuplicate(proposal.id()) || repeatedInBatch) {
                skippedDuplicates++;
                continue;
            }
            prepared.add(candidate);
            ready.add(proposal);
        }
        if (ready.isEmpty()) {
            status.setText("No new calendar entries to add. " + skippedDuplicates + " duplicates skipped.");
            return;
        }
        List<String> selectedWarnings = new ArrayList<>();
        for (CommandInvoker.Proposal proposal : ready) {
            var candidate = controller.syllabusCandidate(proposal.id());
            List<String> warnings = candidate.warnings().stream()
                    .filter(w -> !w.startsWith("No specific date:"))
                    .filter(w -> !w.startsWith("Another pending proposal has the same title and date;"))
                    .toList();
            if (!warnings.isEmpty())
                selectedWarnings.add(candidate.draft().title() + ": "
                        + String.join("; ", warnings));
        }
        if (!selectedWarnings.isEmpty()) {
            JTextArea warnings = new JTextArea("Check these source details before adding:\n\n"
                    + String.join("\n\n", selectedWarnings), 12, 68);
            warnings.setEditable(false);
            warnings.setLineWrap(true);
            warnings.setWrapStyleWord(true);
            warnings.setCaretPosition(0);
            if (JOptionPane.showConfirmDialog(this, new JScrollPane(warnings),
                    "Confirm calendar details", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        }
        java.util.Set<UUID> before = controller.schedule(AgentController.Period.ALL, null, "",
                LocalDate.now()).stream().map(Deadline::id).collect(java.util.stream.Collectors.toSet());
        int applied = 0;
        List<String> errors = new ArrayList<>();
        for (CommandInvoker.Proposal proposal : ready) {
            try { controller.approveProposal(proposal.id()); applied++; }
            catch (java.io.IOException | RuntimeException ex) {
                errors.add(controller.syllabusCandidate(proposal.id()).draft().title() + ": " + ex.getMessage());
            }
        }
        List<Deadline> newlyUndated = controller.schedule(AgentController.Period.ALL, null, "",
                LocalDate.now()).stream().filter(d -> !before.contains(d.id()) && d.dueDate() == null).toList();
        try { controller.markUndatedRemindersShown(newlyUndated, LocalDate.now()); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        refresh();
        showViewerTab("Calendar", calendarTab, () -> { });
        status.setText(applied + " deadline" + (applied == 1 ? "" : "s") + " added to the calendar"
                + (skippedDuplicates == 0 ? "" : " · " + skippedDuplicates + " duplicates skipped")
                + (skippedByUser == 0 ? "" : " · " + skippedByUser + " skipped")
                + (errors.isEmpty() ? "" : " · " + errors.size() + " failed"));
        if (!errors.isEmpty()) JOptionPane.showMessageDialog(this, String.join("\n", errors),
                "Some deadlines were not added", JOptionPane.WARNING_MESSAGE);
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
            contentTabs.setSelectedComponent(coursePage);
        } else if (value instanceof WorkspaceBranch branch && branch.kind() == WorkspaceBranch.Kind.CATEGORY) {
            selectedCourseId = branch.id();
            refreshSchedule();
            refreshCalendar();
            showCategoryFiles(controller.course(branch.id()), categoryOf(branch));
            showViewerTab("Files", filePanel, () -> { });
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
        showViewerTab("Files", filePanel, () -> { });
    }

    private void showMaterial(MaterialFile material) {
        Course course = controller.course(material.courseId());
        Component opened = openDocuments.get(material.id());
        if (opened == null) {
            try {
                JPanel document = new JPanel(new BorderLayout(0, 8));
                document.setBorder(AppTheme.padding(4, 8, 8, 8));
                JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                JLabel meta = new JLabel(course.name() + " / " + material.category());
                meta.setForeground(AppTheme.MUTED);
                controls.add(meta);
                button(controls, "Open original", () -> openOriginal(material));
                button(controls, "Move to another folder...", () -> moveMaterial(material));
                button(controls, "Expand / restore", this::toggleViewer);
                document.add(controls, BorderLayout.NORTH);
                if (material.originalName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
                    document.add(new PdfPreviewPanel(controller.materialPath(material.id())), BorderLayout.CENTER);
                } else {
                    JTextArea details = new JTextArea("Original file: " + material.originalName()
                            + "\n\nUse Open original to view this file in its normal application."
                            + (material.extractionWarning().isBlank() ? ""
                                    : "\n\n" + material.extractionWarning()));
                    details.setEditable(false);
                    details.setLineWrap(true);
                    details.setWrapStyleWord(true);
                    details.setBorder(AppTheme.padding(24, 24, 24, 24));
                    document.add(new JScrollPane(details), BorderLayout.CENTER);
                }
                opened = document;
            } catch (java.io.IOException ex) { throw new IllegalStateException("Unable to open PDF: " + ex.getMessage(), ex); }
            openDocuments.put(material.id(), opened);
        }
        showViewerTab(material.originalName(), opened, () -> openDocuments.remove(material.id()));
        status.setText(material.originalName() + " · Original file preview");
    }

    private void closeMaterialTab(UUID id) {
        Component opened = openDocuments.remove(id);
        if (opened == null) return;
        viewerTabs.remove(opened);
        if (viewerTabs.getTabCount() == 0) hideViewer();
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
        perform(() -> openOriginal(leaf.file()));
    }

    private void openOriginal(MaterialFile material) {
        try {
            var path = controller.materialPath(material.id());
            if (!Files.isRegularFile(path)) throw new IllegalStateException("Stored file is missing: " + path);
            Desktop.getDesktop().open(path.toFile());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
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
                    + (leaf.file().textAvailable() ? "Text available to AI" : "No readable text for AI"));
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
