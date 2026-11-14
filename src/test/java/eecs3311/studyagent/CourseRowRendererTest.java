package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.util.UUID;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class CourseRowRendererTest {
    @Test void thinMarkerFollowsCourseAfterSortingWithoutTintingBackground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var a=new Course(UUID.randomUUID(),UUID.randomUUID(),"EECS3311","#D35968",Difficulty.HIGH);
            var b=new Course(UUID.randomUUID(),UUID.randomUUID(),"EECS2030","#58B88A",Difficulty.LOW);
            JTable table=new JTable(new DefaultTableModel(new Object[][]{{"Z","EECS3311"},{"A","EECS2030"}},new String[]{"Title","Course"}));
            table.setBackground(AppTheme.SURFACE);
            table.setAutoCreateRowSorter(true); CourseRowRenderer renderer=new CourseRowRenderer(row->row==0?a:b);
            Color first=renderer.getTableCellRendererComponent(table,"Z",false,false,0,0).getBackground();
            Color second=renderer.getTableCellRendererComponent(table,"A",false,false,1,0).getBackground();
            assertEquals(first.getRed(),first.getGreen()); assertEquals(first.getGreen(),first.getBlue());
            assertEquals(second.getRed(),second.getGreen()); assertEquals(second.getGreen(),second.getBlue());
            assertEquals(first,second); assertEquals(AppTheme.SURFACE,first);
            assertEquals(new Color(0x94B2D1), marker(renderer,table,0,1));
            assertEquals(new Color(0xC8B68B), marker(renderer,table,1,1));
            assertEquals(AppTheme.SURFACE, marker(renderer,table,0,0));
            table.getRowSorter().toggleSortOrder(0);
            assertEquals(second,renderer.getTableCellRendererComponent(table,"A",false,false,0,0).getBackground());
            assertEquals(new Color(0xC8B68B), marker(renderer,table,0,1));
            Color selected=renderer.getTableCellRendererComponent(table,"A",true,false,0,0).getBackground();
            assertNotEquals(second,selected);
            assertEquals(selected.getRed(),selected.getGreen()); assertEquals(selected.getGreen(),selected.getBlue());
        });
    }
    private static Color marker(CourseRowRenderer renderer,JTable table,int row,int column) {
        var cell=renderer.getTableCellRendererComponent(table,"Course",false,false,row,column);
        cell.setSize(200,32);
        var image=new java.awt.image.BufferedImage(200,32,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();
        try { cell.paint(graphics); } finally { graphics.dispose(); }
        return new Color(image.getRGB(1,16));
    }
}
