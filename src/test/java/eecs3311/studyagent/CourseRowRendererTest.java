package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.util.UUID;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class CourseRowRendererTest {
    @Test void neutralShadeFollowsCourseAfterSortingAndSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var a=new Course(UUID.randomUUID(),UUID.randomUUID(),"EECS3311","#D35968",Difficulty.HIGH);
            var b=new Course(UUID.randomUUID(),UUID.randomUUID(),"EECS2030","#58B88A",Difficulty.LOW);
            JTable table=new JTable(new DefaultTableModel(new Object[][]{{"Z"},{"A"}},new String[]{"Title"}));
            table.setAutoCreateRowSorter(true); CourseRowRenderer renderer=new CourseRowRenderer(row->row==0?a:b);
            Color first=renderer.getTableCellRendererComponent(table,"Z",false,false,0,0).getBackground();
            Color second=renderer.getTableCellRendererComponent(table,"A",false,false,1,0).getBackground();
            assertEquals(first.getRed(),first.getGreen()); assertEquals(first.getGreen(),first.getBlue());
            assertEquals(second.getRed(),second.getGreen()); assertEquals(second.getGreen(),second.getBlue());
            assertNotEquals(first,second); table.getRowSorter().toggleSortOrder(0);
            assertEquals(second,renderer.getTableCellRendererComponent(table,"A",false,false,0,0).getBackground());
            Color selected=renderer.getTableCellRendererComponent(table,"A",true,false,0,0).getBackground();
            assertNotEquals(second,selected);
            assertEquals(selected.getRed(),selected.getGreen()); assertEquals(selected.getGreen(),selected.getBlue());
        });
    }
}
