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
    private final JTextArea conversation = new JTextArea();
    private final JTextField input = new JTextField();

    public ChatPanel(Consumer<String> question, Consumer<String> scheduleChange) {
        super(new BorderLayout(0, 12));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14));
        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setText("Ask about uploaded course materials or describe a schedule change.");
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
        conversation.append("\n\n" + message);
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }

    public void clear() {
        conversation.setText("Ask about uploaded course materials or describe a schedule change.");
        input.setText("");
    }
}
