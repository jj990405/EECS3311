package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialSelectionTest {
    @TempDir Path root;
    @Test void fileRowsHighlightAndOpenTheClickedFileRatherThanAnotherSource() throws Exception {
        var agent=new AgentController(root.resolve("data"),(s,u)->"{}");
        var sem=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course=agent.createCourse(sem.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);
        Path a=root.resolve("first.txt"),b=root.resolve("second.txt");Files.writeString(a,"first");Files.writeString(b,"second");
        var first=agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,a);
        var second=agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,b);
        AtomicReference<MaterialFile> selected=new AtomicReference<>(),opened=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()-> {
            var files=new MaterialList(selected::set,opened::set,file->new JPopupMenu());
            files.setListData(new MaterialFile[]{first,second});files.setSelectedIndex(1);
            files.getActionMap().get("openFile").actionPerformed(new java.awt.event.ActionEvent(files,0,"open"));
            assertEquals(second,selected.get());assertEquals(second,opened.get());
            var page=new CoursePage(agent,()->{},()->{},()->{},()->{},()->{});
            page.setFileActions(selected::set,opened::set,file->new JPopupMenu());page.showCourse(course);
            JTextArea text=find(page,"first.txt");assertNotNull(text);
            text.dispatchEvent(new java.awt.event.MouseEvent(text,java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),0,5,5,1,false,java.awt.event.MouseEvent.BUTTON1));
            assertEquals(first,selected.get());assertEquals(AppTheme.BLUE_TINT,text.getBackground());
        });
    }
    private JTextArea find(java.awt.Container parent,String value) {
        for(var child:parent.getComponents()) {
            if(child instanceof JTextArea text&&text.getText().equals(value))return text;
            if(child instanceof java.awt.Container inner){var result=find(inner,value);if(result!=null)return result;}
        }
        return null;
    }
}
