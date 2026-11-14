package eecs3311.studyagent;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.*;
import java.util.UUID;
import java.awt.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudyRecordsTest {
    @TempDir Path root;
    @Test void recordsCombineSameDayAndBriefIncludesUnlinkedStudyTime() throws Exception {
        DataStore store=new DataStore(root);
        LocalDate day=LocalDate.now();
        store.recordStudySession(new StudySession(UUID.randomUUID(),null,"Study",day.atTime(9,0),300));
        store.recordStudySession(new StudySession(UUID.randomUUID(),null,"Study",day.atTime(10,0),600));
        AgentController agent=new AgentController(root,(system,user)-> {
            assertTrue(user.contains("All study seconds this week (timer is not course-linked): 900"));
            return "{\"briefing\":\"You studied 15 minutes this week.\"}";
        });
        var semester=agent.createSemester("Term",day.minusDays(30),day.plusDays(90));
        var course=agent.createCourse(semester.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);
        assertEquals(900,agent.weeklyStudySeconds(null,day));
        assertTrue(agent.weeklyBriefing(course.id(),day).contains("15 minutes"));
        SwingUtilities.invokeAndWait(()-> {
            TimerRecordPanel panel=new TimerRecordPanel(agent);
            String text=findText(panel);
            assertTrue(text.contains(day+" · 15 min 0 sec"));
            assertEquals(1,text.split(day.toString(),-1).length-1);
            assertTrue(text.contains("Weekly total: 15 min 0 sec"));
        });
    }
    private String findText(Component component) {
        if(component instanceof JTextArea area)return area.getText();
        if(component instanceof Container container)
            for(Component child:container.getComponents()){String text=findText(child);if(!text.isEmpty())return text;}
        return "";
    }
    @Test void timerReachesZeroAndStopsWithoutOvercountingPause() throws Exception {
        StudyTimer timer=new StudyTimer();timer.start(null,"Study",1);
        Thread.sleep(1100);
        assertEquals(0,timer.remainingSeconds());
        assertTrue(timer.stop().seconds()>=1);
        assertFalse(timer.active());
    }
}
