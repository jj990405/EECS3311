package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.IntConsumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** GUI timer controls for the three durations in the Stage 1 design. */
public final class TimerPanel extends JPanel {
    private final JLabel clock = new JLabel("Choose 15, 30, or 60 minutes", JLabel.CENTER);

    public TimerPanel(IntConsumer start, Runnable pause, Runnable resume, Runnable stop,
                      Runnable records) {
        super(new BorderLayout(0, 16));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 24, 24));
        clock.setFont(clock.getFont().deriveFont(Font.BOLD, 32f));
        add(clock, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        for (int minutes : new int[] {15, 30, 60}) {
            JButton button = new JButton(minutes == 60 ? "1 hour" : minutes + " min");
            button.addActionListener(event -> start.accept(minutes));
            actions.add(button);
        }
        button(actions, "Pause", pause);
        button(actions, "Resume", resume);
        button(actions, "Stop", stop);
        button(actions, "Records", records);
        add(actions, BorderLayout.SOUTH);
    }

    private void button(JPanel parent, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        parent.add(button);
    }

    public void showRemaining(long seconds) {
        clock.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
    }

    public void showIdle() { clock.setText("Choose 15, 30, or 60 minutes"); }
}
