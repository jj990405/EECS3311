package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** One readable question at a time, with a separate answer review. */
final class QuizDialog extends JDialog {
    private final StudyArtifacts.Quiz quiz;
    private final Integer[] answers;
    private final JLabel counter = new JLabel();
    private final JLabel topic = new JLabel();
    private final JTextArea questionText = textArea(18, true);
    private final JPanel choiceList = new JPanel(new GridLayout(0, 1, 0, 10));
    private JScrollPane questionScroll;
    private final JButton back = AppTheme.button("Back", false);
    private final JButton next = AppTheme.button("Next question", true);
    private int index;
    private boolean submitted;

    private QuizDialog(JFrame owner, StudyArtifacts.Quiz quiz) {
        super(owner, "Practice quiz", true);
        this.quiz = quiz;
        this.answers = new Integer[quiz.questions().size()];
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(620, 500));
        setSize(820, 650);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(AppTheme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBackground(AppTheme.SURFACE);
        header.setBorder(AppTheme.padding(20, 28, 18, 28));
        JLabel title = new JLabel("Practice quiz");
        title.setFont(AppTheme.TITLE);
        title.setForeground(AppTheme.TEXT);
        header.add(title, BorderLayout.NORTH);
        counter.setForeground(AppTheme.MUTED);
        header.add(counter, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel body = new WidthTrackingPanel(new BorderLayout(0, 18));
        body.setBackground(AppTheme.BACKGROUND);
        body.setBorder(AppTheme.padding(24, 28, 24, 28));
        JPanel prompt = new JPanel(new BorderLayout(0, 10));
        prompt.setOpaque(false);
        topic.setFont(AppTheme.SMALL.deriveFont(Font.BOLD));
        topic.setForeground(AppTheme.BLUE);
        prompt.add(topic, BorderLayout.NORTH);
        questionText.setBackground(AppTheme.BACKGROUND);
        prompt.add(questionText, BorderLayout.CENTER);
        body.add(prompt, BorderLayout.NORTH);
        choiceList.setBackground(AppTheme.BACKGROUND);
        body.add(choiceList, BorderLayout.CENTER);
        questionScroll = new JScrollPane(body);
        questionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        questionScroll.setBorder(null);
        questionScroll.getViewport().setBackground(AppTheme.BACKGROUND);
        questionScroll.getVerticalScrollBar().setUnitIncrement(22);
        add(questionScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(AppTheme.SURFACE);
        footer.setBorder(AppTheme.padding(14, 24, 14, 24));
        JLabel hint = new JLabel("Choose one answer to continue.");
        hint.setForeground(AppTheme.MUTED);
        footer.add(hint, BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        JButton cancel = AppTheme.button("Cancel quiz", false);
        cancel.addActionListener(e -> dispose());
        back.addActionListener(e -> { index--; showQuestion(); });
        next.addActionListener(e -> {
            if (index == answers.length - 1) { submitted = true; dispose(); }
            else { index++; showQuestion(); }
        });
        controls.add(cancel);
        controls.add(back);
        controls.add(next);
        footer.add(controls, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(next);
        showQuestion();
    }

    static List<Integer> take(JFrame owner, StudyArtifacts.Quiz quiz) {
        QuizDialog dialog = new QuizDialog(owner, quiz);
        dialog.setVisible(true);
        return dialog.submitted ? Arrays.asList(dialog.answers) : null;
    }

    private void showQuestion() {
        StudyArtifacts.Question question = quiz.questions().get(index);
        counter.setText("Question " + (index + 1) + " of " + answers.length);
        topic.setText(question.topic());
        questionText.setText(question.text());
        questionText.setRows(Math.max(2, (question.text().length() + 65) / 66));
        choiceList.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (int choice = 0; choice < question.choices().size(); choice++) {
            final int choiceIndex = choice;
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(AppTheme.SURFACE);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                    AppTheme.padding(12, 14, 12, 14)));
            JRadioButton radio = new JRadioButton();
            radio.setOpaque(false);
            radio.setSelected(answers[index] != null && answers[index] == choice);
            radio.addActionListener(e -> { answers[index] = choiceIndex; next.setEnabled(true); });
            group.add(radio);
            row.add(radio, BorderLayout.WEST);
            JTextArea label = textArea(14, false);
            label.setBackground(AppTheme.SURFACE);
            label.setText(question.choices().get(choice));
            label.setRows(Math.max(2, (label.getText().length() + 54) / 55));
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { radio.doClick(); }
            });
            row.add(label, BorderLayout.CENTER);
            choiceList.add(row);
        }
        back.setEnabled(index > 0);
        next.setText(index == answers.length - 1 ? "Submit quiz" : "Next question");
        next.setEnabled(answers[index] != null);
        choiceList.revalidate();
        choiceList.repaint();
        javax.swing.SwingUtilities.invokeLater(() -> {
            questionScroll.getVerticalScrollBar().setValue(0);
            if (choiceList.getComponentCount() > 0)
                ((JPanel) choiceList.getComponent(0)).getComponent(0).requestFocusInWindow();
        });
    }

    static void showResult(JFrame owner, StudyArtifacts.Quiz quiz, List<Integer> answers,
                           StudyArtifacts.Attempt attempt) {
        JDialog dialog = new JDialog(owner, "Quiz review", true);
        dialog.setSize(840, 680);
        dialog.setMinimumSize(new Dimension(620, 500));
        dialog.setLocationRelativeTo(owner);
        dialog.getContentPane().setBackground(AppTheme.BACKGROUND);
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setBackground(AppTheme.SURFACE);
        heading.setBorder(AppTheme.padding(20, 28, 18, 28));
        JLabel score = new JLabel(attempt.correct() + " of " + quiz.questions().size() + " correct");
        score.setFont(AppTheme.TITLE);
        score.setForeground(AppTheme.TEXT);
        heading.add(score, BorderLayout.NORTH);
        JLabel subtitle = new JLabel("Review each answer and its explanation");
        subtitle.setForeground(AppTheme.MUTED);
        heading.add(subtitle, BorderLayout.SOUTH);
        dialog.add(heading, BorderLayout.NORTH);
        JPanel review = new WidthTrackingPanel();
        review.setLayout(new javax.swing.BoxLayout(review, javax.swing.BoxLayout.Y_AXIS));
        review.setBackground(AppTheme.BACKGROUND);
        review.setBorder(AppTheme.padding(18, 24, 18, 24));
        for (int i = 0; i < quiz.questions().size(); i++) {
            StudyArtifacts.Question q = quiz.questions().get(i);
            boolean correct = answers.get(i) == q.answerIndex();
            JPanel item = new JPanel(new BorderLayout(0, 10));
            item.setBackground(AppTheme.SURFACE);
            item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER), AppTheme.padding(16, 18, 16, 18)));
            JLabel label = new JLabel("Question " + (i + 1) + "  ·  " + q.topic()
                    + "  ·  " + (correct ? "Correct" : "Review"));
            label.setForeground(correct ? AppTheme.GREEN : AppTheme.RED);
            label.setFont(AppTheme.BODY.deriveFont(Font.BOLD));
            item.add(label, BorderLayout.NORTH);
            String details = q.text() + "\n\nYour answer: " + q.choices().get(answers.get(i))
                    + (correct ? "" : "\nCorrect answer: " + q.choices().get(q.answerIndex()))
                    + "\n\n" + q.explanation();
            JTextArea description = textArea(14, false);
            description.setBackground(AppTheme.SURFACE);
            description.setText(details);
            int lines = Arrays.stream(details.split("\n", -1))
                    .mapToInt(line -> Math.max(1, (line.length() + 84) / 85)).sum();
            description.setRows(Math.max(5, lines));
            item.add(description, BorderLayout.CENTER);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, item.getPreferredSize().height));
            review.add(item);
            review.add(javax.swing.Box.createVerticalStrut(12));
        }
        JScrollPane scroll = new JScrollPane(review);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        dialog.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(AppTheme.SURFACE);
        footer.setBorder(AppTheme.padding(8, 20, 8, 20));
        JButton close = AppTheme.button("Done", true);
        close.addActionListener(e -> dialog.dispose());
        footer.add(close);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        dialog.setVisible(true);
    }

    private static JTextArea textArea(int size, boolean question) {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setForeground(AppTheme.TEXT);
        text.setFont(AppTheme.BODY.deriveFont(question ? Font.BOLD : Font.PLAIN, (float) size));
        text.setBorder(null);
        return text;
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        private WidthTrackingPanel() { super(); }
        private WidthTrackingPanel(LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, visible.height - 24);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
