package eecs3311.studyagent;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NarrowPanelTest {
    @TempDir Path root;
    @Test void courseAndTimerActionsStayInsideNarrowPanel() throws Exception {
        AgentController agent=new AgentController(root,(s,u)->"{}");
        SwingUtilities.invokeAndWait(()-> {
            CoursePage course=new CoursePage(agent,()->{},()->{},()->{},()->{},()->{});
            TimerPanel timer=new TimerPanel(m->{},()->{},()->{},()->{},()->{});
            for(JPanel panel:new JPanel[]{course,timer}) {
                panel.setSize(310,600);layout(panel);
                java.util.List<JButton> buttons=new java.util.ArrayList<>();collect(panel,buttons);
                buttons.removeIf(button->button.getText()==null||button.getText().isBlank());
                assertEquals(panel==course?2:7,buttons.size());
                for(JButton button:buttons) {
                    Rectangle bounds=SwingUtilities.convertRectangle(button.getParent(),button.getBounds(),panel);
                    assertTrue(bounds.width>0&&bounds.height>0,button.getText());
                    assertTrue(bounds.y>=0&&bounds.y+bounds.height<=panel.getHeight(),button.getText()+" clipped below panel");
                    assertTrue(bounds.x>=0&&bounds.x+bounds.width<=panel.getWidth(),button.getText()+" clipped horizontally");
                }
            }
        });
    }
    private void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container inner)layout(inner);}
    private void collect(Container c,java.util.List<JButton> buttons){for(Component child:c.getComponents()){if(child instanceof JButton b)buttons.add(b);else if(child instanceof Container inner)collect(inner,buttons);}}
}
