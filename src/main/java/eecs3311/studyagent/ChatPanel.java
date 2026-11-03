package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Course chat entry point for grounded questions and schedule change requests. */
public final class ChatPanel extends JPanel {
    private final JTextArea conversation = new JTextArea();
    private final JTextField input = new JTextField(42);

    public ChatPanel(Consumer<String> question, Consumer<String> scheduleChange) {
        super(new BorderLayout(0, 12));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14));
        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setText("Ask about uploaded course materials or describe a schedule change.");
        add(new JScrollPane(conversation), BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(input);
        button(controls, "Ask sources", question);
        button(controls, "Change schedule", scheduleChange);
        add(controls, BorderLayout.SOUTH);
    }

    private void button(JPanel parent, String label, Consumer<String> action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> {
            String message = input.getText().trim();
            if (message.isBlank()) return;
            append("You: " + message);
            input.setText("");
            action.accept(message);
        });
        parent.add(button);
    }

    public void append(String message) {
        conversation.append("\n\n" + message);
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }
}
