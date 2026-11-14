package eecs3311.studyagent;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.text.View;

/** Question navigation and readable answers, with an explicit submission boundary. */
final class QuizSessionPanel extends JPanel {
    private final StudyArtifacts.Quiz quiz;
    private final Integer[] answers;
    private final boolean review;
    private final JLabel progress = new JLabel(), footerStatus = new JLabel();
    private final DefaultListModel<String> questionItems = new DefaultListModel<>();
    private final JList<String> navigator = new JList<>(questionItems);
    private final ReadingColumn sheet = new ReadingColumn();
    private final JScrollPane scroll = new JScrollPane(sheet);
    private final JButton previous = AppTheme.button("Previous", false);
    private final JButton next = AppTheme.button("Next question", true);
    private final java.util.List<AnswerOption> options = new java.util.ArrayList<>();
    private int index;
    private boolean updatingNavigator;

    QuizSessionPanel(StudyArtifacts.Quiz quiz, List<Integer> savedAnswers, Runnable finish, Runnable cancel) {
        super(new BorderLayout());
        this.quiz = quiz; review = savedAnswers != null;
        answers = review ? savedAnswers.toArray(Integer[]::new) : new Integer[quiz.questions().size()];
        setBackground(AppTheme.BACKGROUND);
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(AppTheme.SURFACE); header.setBorder(AppTheme.padding(20, 24, 18, 24));
        JLabel title = new JLabel(review ? "Quiz results" : "Practice quiz"); title.setFont(AppTheme.TITLE);
        header.add(title, BorderLayout.NORTH);
        progress.setForeground(AppTheme.MUTED); header.add(progress, BorderLayout.CENTER); add(header, BorderLayout.NORTH);
        JPanel navigation = new JPanel(new BorderLayout(0, 12));
        navigation.setBackground(AppTheme.SURFACE); navigation.setBorder(AppTheme.padding(18, 14, 18, 14));
        JLabel label = new JLabel("Questions"); label.setFont(AppTheme.BODY.deriveFont(Font.BOLD));
        navigation.add(label, BorderLayout.NORTH);
        navigator.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); navigator.setFixedCellHeight(42);
        navigator.setBackground(AppTheme.SURFACE); navigator.setForeground(AppTheme.TEXT);
        navigator.setSelectionBackground(AppTheme.BLUE_TINT); navigator.setSelectionForeground(AppTheme.TEXT);
        navigator.getAccessibleContext().setAccessibleName("Question navigator");
        navigator.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int row,
                    boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, row, selected, focused);
                setBorder(AppTheme.padding(8, 10, 8, 10));
                if (review && !selected) setForeground(isCorrect(row) ? AppTheme.GREEN : AppTheme.RED);
                return this;
            }
        });
        navigator.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingNavigator && navigator.getSelectedIndex() >= 0) {
                index = navigator.getSelectedIndex(); showQuestion();
            }
        });
        JScrollPane navigationScroll = new JScrollPane(navigator); navigationScroll.setBorder(null);
        navigation.add(navigationScroll, BorderLayout.CENTER);
        navigation.setPreferredSize(new Dimension(174, 300)); navigation.setMinimumSize(new Dimension(145, 100));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(24);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigation, scroll);
        split.setDividerLocation(174); split.setResizeWeight(0); split.setBorder(null); split.setDividerSize(1);
        add(split, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(AppTheme.SURFACE); footer.setBorder(AppTheme.padding(14, 20, 14, 20));
        footerStatus.setForeground(AppTheme.MUTED); footer.add(footerStatus, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        JButton close = AppTheme.button(review ? "Close review" : "Cancel quiz", false);
        close.addActionListener(event -> cancel.run());
        previous.addActionListener(event -> { index--; showQuestion(); });
        next.addActionListener(event -> {
            if (index == answers.length - 1) { if (review || allAnswered()) finish.run(); }
            else { index++; showQuestion(); }
        });
        actions.add(close); actions.add(previous); actions.add(next); footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        for (int answer = 0; answer < 4; answer++) {
            final int choice = answer;
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke((char)('1' + answer)), "answer" + answer);
            getActionMap().put("answer" + answer, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) { if (!review) selectAnswer(choice); }
            });
        }
        showQuestion();
    }

    List<Integer> answers() { return Arrays.asList(answers.clone()); }
    boolean allAnswered() { return Arrays.stream(answers).allMatch(java.util.Objects::nonNull); }
    private boolean isCorrect(int question) { return answers[question] != null && answers[question] == quiz.questions().get(question).answerIndex(); }
    private void updateNavigation() {
        updatingNavigator = true;
        try {
            questionItems.clear();
            for (int i = 0; i < answers.length; i++) questionItems.addElement((i + 1) + "   "
                    + (review ? isCorrect(i) ? "Correct" : "Review" : answers[i] == null ? "Not answered" : "Answered"));
            navigator.setSelectedIndex(index); navigator.ensureIndexIsVisible(index);
        } finally { updatingNavigator = false; }
        long answered = Arrays.stream(answers).filter(java.util.Objects::nonNull).count();
        long correct = java.util.stream.IntStream.range(0, answers.length).filter(this::isCorrect).count();
        progress.setText(review ? correct + " of " + answers.length + " correct · " + Math.round(100.0 * correct / answers.length) + "%"
                : answered + " of " + answers.length + " answered");
        previous.setEnabled(index > 0);
        next.setText(index == answers.length - 1 ? review ? "Done" : "Submit quiz" : "Next question");
        next.setEnabled(review || index < answers.length - 1 || allAnswered());
        footerStatus.setText(review ? "Answer review" : answered == answers.length ? "Ready to submit" : (answers.length - answered) + " unanswered");
    }
    private void selectAnswer(int answer) {
        answers[index] = answer;
        for (AnswerOption option : options) option.refresh();
        updateNavigation();
    }
    private void showQuestion() {
        sheet.removeAll(); options.clear(); var question = quiz.questions().get(index);
        JLabel position = new JLabel("Question " + (index + 1) + " of " + answers.length);
        position.setFont(AppTheme.SMALL.deriveFont(Font.BOLD)); position.setForeground(AppTheme.MUTED); addToSheet(position, 10);
        addToSheet(new WrappedText(question.topic(), AppTheme.BODY.deriveFont(Font.BOLD), AppTheme.BLUE), 12);
        addToSheet(new WrappedText(question.text(), AppTheme.BODY.deriveFont(Font.BOLD, 20f), AppTheme.TEXT), 24);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < question.choices().size(); i++) {
            AnswerOption option = new AnswerOption(i, question.choices().get(i));
            options.add(option); group.add(option.radio); addToSheet(option, 10);
        }
        if (review) {
            JLabel explanation = new JLabel("Explanation"); explanation.setFont(AppTheme.SECTION);
            explanation.setBorder(AppTheme.padding(16, 0, 0, 0)); addToSheet(explanation, 12);
            addToSheet(new WrappedText(question.explanation(), AppTheme.BODY.deriveFont(15f), AppTheme.TEXT), 12);
        }
        sheet.add(Box.createVerticalGlue()); updateNavigation(); sheet.revalidate(); sheet.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }
    private void addToSheet(JComponent component, int gap) {
        component.setAlignmentX(LEFT_ALIGNMENT); sheet.add(component); sheet.add(Box.createVerticalStrut(gap));
    }
    private final class AnswerOption extends JPanel {
        private final int choice;
        private final JRadioButton radio;
        private final WrappedText text;
        private final JLabel state = new JLabel();
        AnswerOption(int choice, String answer) {
            super(new BorderLayout(12, 6)); this.choice = choice;
            radio = new JRadioButton(String.valueOf((char)('A' + choice)));
            radio.setOpaque(false); radio.setVerticalAlignment(SwingConstants.TOP);
            radio.getAccessibleContext().setAccessibleName("Answer " + (char)('A' + choice) + ": " + answer);
            radio.addActionListener(event -> selectAnswer(choice)); radio.setEnabled(!review); add(radio, BorderLayout.WEST);
            text = new WrappedText(answer, AppTheme.BODY.deriveFont(15f), AppTheme.TEXT);
            add(text, BorderLayout.CENTER); state.setFont(AppTheme.SMALL.deriveFont(Font.BOLD)); add(state, BorderLayout.SOUTH);
            var click = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent event) { if (!review) radio.doClick(); }
            };
            addMouseListener(click); text.addMouseListener(click); state.addMouseListener(click);
            setCursor(Cursor.getPredefinedCursor(review ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR)); refresh();
        }
        void refresh() {
            boolean selected = answers[index] != null && answers[index] == choice;
            boolean correct = review && quiz.questions().get(index).answerIndex() == choice;
            radio.setSelected(selected);
            Color accent = correct ? AppTheme.GREEN : review && selected ? AppTheme.RED : selected ? AppTheme.BLUE : AppTheme.BORDER;
            setBackground(selected || correct ? mix(AppTheme.SURFACE, accent) : AppTheme.SURFACE); text.setBackground(getBackground());
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, selected || correct ? 2 : 1), AppTheme.padding(14, 14, 14, 16)));
            state.setText(review ? selected && correct ? "Your answer · Correct" : correct ? "Correct answer" : selected ? "Your answer" : "" : selected ? "Selected" : "");
            state.setForeground(accent); revalidate(); repaint();
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
    private static Color mix(Color base, Color accent) {
        return new Color((base.getRed() * 9 + accent.getRed()) / 10, (base.getGreen() * 9 + accent.getGreen()) / 10,
                (base.getBlue() * 9 + accent.getBlue()) / 10);
    }
    private static final class WrappedText extends JTextArea {
        WrappedText(String value, Font font, Color foreground) {
            super(value); setLineWrap(true); setWrapStyleWord(true); setEditable(false); setFocusable(false);
            setFont(font); setForeground(foreground); setOpaque(false); setBorder(null);
        }
        @Override public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : 560;
            View root = getUI().getRootView(this); root.setSize(Math.max(40, width), Float.MAX_VALUE);
            return new Dimension(width, (int)Math.ceil(root.getPreferredSpan(View.Y_AXIS)) + 4);
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
    private static final class ReadingColumn extends JPanel implements Scrollable {
        ReadingColumn() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(AppTheme.BACKGROUND); setBorder(AppTheme.padding(24, 28, 24, 28)); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(620, 550); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(24, r.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
