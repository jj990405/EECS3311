package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.util.function.IntConsumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** GUI timer controls for the three durations in the Stage 1 design. */
public final class TimerPanel extends JPanel {
    private final JLabel clock = new JLabel("Ready to study", JLabel.CENTER);
    private final java.util.List<JButton> starts = new java.util.ArrayList<>();
    private JButton pauseControl, resumeControl, stopControl;
    private boolean paused;
    private long lastSeconds;

    public TimerPanel(IntConsumer start, Runnable pause, Runnable resume, Runnable stop,
                      Runnable records) {
        super(new BorderLayout(0, 16));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 24, 24));
        clock.setFont(clock.getFont().deriveFont(Font.BOLD, 32f));
        add(clock, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 3, 8, 8));
        for (int minutes : new int[] {15, 30, 60}) {
            JButton button = new JButton(minutes == 60 ? "1 hour" : minutes + " min");
            button.addActionListener(event -> start.accept(minutes));
            actions.add(button);
            starts.add(button);
        }
        pauseControl = button(actions, "Pause", pause);
        resumeControl = button(actions, "Resume", resume);
        stopControl = button(actions, "Stop", stop);
        button(actions, "Records", records);
        add(actions, BorderLayout.SOUTH);
        showIdle();
    }

    private JButton button(JPanel parent, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        parent.add(button);
        return button;
    }

    public void showRemaining(long seconds) {
        lastSeconds = seconds;
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        clock.setText(paused ? "<html><center>" + time + "<br><span style='font-size:16pt'>Paused</span></center></html>" : time);
        starts.forEach(button -> button.setEnabled(false));
        pauseControl.setEnabled(!paused);
        resumeControl.setEnabled(paused);
        stopControl.setEnabled(true);
    }

    public void setPaused(boolean value) { paused = value; showRemaining(lastSeconds); }

    public void showIdle() {
        paused = false;
        clock.setText("Ready to study");
        starts.forEach(button -> button.setEnabled(true));
        pauseControl.setEnabled(false); resumeControl.setEnabled(false); stopControl.setEnabled(false);
    }
}
