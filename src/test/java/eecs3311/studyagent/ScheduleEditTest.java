package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleEditTest {
    @TempDir Path root;
    @Test void dateOnlyEditPreservesExistingTitleAndTypeWhenAiLeavesThemBlank() throws Exception {
        AgentController seed = new AgentController(root, (s,u)->"{}");
        var semester=seed.createSemester("Fall", LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course=seed.createCourse(semester.id(),"EECS3311","#3366CC",Difficulty.HIGH);
        var original=seed.addDeadline(course.id(),"Design Review","Assignment",LocalDate.of(2026,11,20),"SOLID");
        AgentController agent=new AgentController(root,(system,user)-> {
            assertTrue(user.contains("type: Assignment"));
            assertTrue(user.contains("scope: SOLID"));
            return "{\"action\":\"edit\",\"deadlineId\":\""+original.id()+"\",\"title\":\"\",\"type\":\"\",\"dueDate\":\"2026-11-21\"}";
        });
        var proposal=agent.planScheduleChange(course.id(),"Move Design Review to Nov 21");
        assertEquals(LocalDate.of(2026,11,20),agent.deadline(original.id()).dueDate());
        agent.approveProposal(proposal.id());
        var edited=agent.deadline(original.id());
        assertEquals("Design Review",edited.title());
        assertEquals("Assignment",edited.type());
        assertEquals("SOLID",edited.scope());
        assertEquals(LocalDate.of(2026,11,21),edited.dueDate());
    }
}
