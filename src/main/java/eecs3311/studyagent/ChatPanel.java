package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Course chat entry point for grounded questions and schedule change requests. */
public final class ChatPanel extends JPanel {
    private final ReadableText conversation = new ReadableText();
    private final JTextField input = new JTextField();
    private final java.util.List<JButton> buttons = new java.util.ArrayList<>();
    private final javax.swing.JLabel context = new javax.swing.JLabel("Select a course or lecture for source questions.");
    private final javax.swing.JComboBox<String> scope = new javax.swing.JComboBox<>(new String[]{"Course materials", "Selected file only"});

    public ChatPanel(Consumer<String> question, Consumer<String> scheduleChange) {
        super(new BorderLayout(0, 12));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel heading = new JPanel(new BorderLayout(0,8));
        context.setForeground(AppTheme.MUTED); heading.add(context,BorderLayout.NORTH);
        scope.setEnabled(false); heading.add(scope,BorderLayout.SOUTH); add(heading,BorderLayout.NORTH);
        conversation.setMarkdown("Ask about uploaded course materials or describe a schedule change.");
        JScrollPane history = new JScrollPane(conversation);
        history.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(history, BorderLayout.CENTER);
        JPanel composer = new JPanel(new BorderLayout(0, 8));
        composer.add(input, BorderLayout.NORTH);
        JPanel controls = new JPanel(new GridLayout(1, 2, 8, 0));
        button(controls, "Ask sources", question);
        button(controls, "Change schedule", scheduleChange);
        composer.add(controls, BorderLayout.SOUTH);
        add(composer, BorderLayout.SOUTH);
        input.addActionListener(event -> submit(question));
    }

    private void button(JPanel parent, String label, Consumer<String> action) {
        JButton button = new JButton(label);
        buttons.add(button);
        button.addActionListener(event -> submit(action));
        parent.add(button);
    }

    private void submit(Consumer<String> action) {
        String message = input.getText().trim();
        if (message.isBlank()) return;
        append("You: " + message);
        input.setText("");
        action.accept(message);
    }

    public void append(String message) {
        conversation.appendMarkdown(message);
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }

    void appendAnswer(GroundedAnswer answer, Consumer<GroundedAnswer.Citation> openCitation) {
        int start = conversation.getDocument().getLength();
        conversation.appendMarkdown("Agent: " + answer.answer());
        conversation.onCitation(openCitation);
        conversation.appendCitations(answer.citations());
        conversation.setCaretPosition(start);
    }

    void setContext(String text) {
        context.setText(text); context.setToolTipText(text);
        scope.setSelectedIndex(0); scope.setEnabled(false);
    }
    void setFileContext(String course, String file) {
        context.setText(course + " · " + file); context.setToolTipText(file); scope.setEnabled(true);
    }
    void useSelectedFile() { scope.setSelectedIndex(1); }
    boolean fileOnly() { return scope.isEnabled() && scope.getSelectedIndex() == 1; }
    void setBusy(boolean busy) {
        input.setEnabled(!busy); buttons.forEach(button -> button.setEnabled(!busy));
    }

    public void clear() {
        conversation.setMarkdown("Ask about uploaded course materials or describe a schedule change.");
        scope.setSelectedIndex(0); scope.setEnabled(false);
        input.setText("");
    }
}
