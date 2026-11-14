package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Always-visible cancellation beside the footer, independent of toolbar width. */
final class AiTaskStatusPanel extends JPanel {
    private final JLabel activityLabel = new JLabel();
    private final JButton cancel = AppTheme.button("Cancel AI", false);
    private final Timer elapsed = new Timer(1000, event -> updateLabel());
    private String activity = "";
    private long started;

    AiTaskStatusPanel(Runnable cancelTask) {
        super(new BorderLayout(10, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(370, 36));
        activityLabel.setForeground(AppTheme.TEXT);
        cancel.addActionListener(event -> cancelTask.run());
        add(activityLabel, BorderLayout.CENTER);
        add(cancel, BorderLayout.EAST);
        setVisible(false);
    }

    void start(String name) {
        activity = name;
        started = System.nanoTime();
        activityLabel.setToolTipText(name);
        updateLabel();
        setVisible(true);
        elapsed.start();
        revalidate();
    }

    void finish() {
        elapsed.stop();
        activity = "";
        activityLabel.setText("");
        setVisible(false);
        revalidate();
    }

    String activity() { return activity; }

    private void updateLabel() {
        long seconds = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started);
        String label = activity.length() > 28 ? activity.substring(0, 25) + "..." : activity;
        activityLabel.setText(label + " · " + seconds + "s");
        activityLabel.getAccessibleContext().setAccessibleName(activity + ", running for " + seconds + " seconds");
    }
}
