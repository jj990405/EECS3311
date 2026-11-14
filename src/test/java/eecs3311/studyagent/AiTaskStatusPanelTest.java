package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class AiTaskStatusPanelTest {
    @Test void busyControlsFitTheFooterAndCancelRemainsClickableWithLongActivityNames() throws Exception {
        var canceled = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            var panel = new AiTaskStatusPanel(canceled::incrementAndGet);
            assertFalse(panel.isVisible());
            panel.start("Reading and indexing a very long collection of uploaded lecture slides");
            try {
                panel.setSize(320, 38); panel.doLayout();
                assertTrue(panel.isVisible());
                var cancel = (JButton)panel.getComponent(1);
                assertTrue(cancel.isEnabled());
                assertTrue(new Rectangle(0,0,320,38).contains(cancel.getBounds()));
                cancel.doClick();
                assertEquals(1,canceled.get());
                assertTrue(((JLabel)panel.getComponent(0)).getText().contains("0s"));
                assertTrue(panel.activity().contains("long collection"));
            } finally { panel.finish(); }
        });
    }

    @Test void completionFailureAndCancellationCanAllClearTheBusyPresentationAndRestart() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new AiTaskStatusPanel(() -> {});
            for (String task : java.util.List.of("Reading uploads", "Generating quiz", "Answering sources")) {
                panel.start(task);
                assertEquals(task, panel.activity());
                panel.finish();
                assertFalse(panel.isVisible());
                assertEquals("", panel.activity());
            }
        });
    }
}
