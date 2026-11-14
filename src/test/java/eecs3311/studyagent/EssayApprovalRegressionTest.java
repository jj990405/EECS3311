package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EssayApprovalRegressionTest {
    @TempDir Path root;

    @Test void irrelevantTestAnswerCanReceiveZeroAndBeApprovedThenReopened() throws Exception {
        var agent = new AgentController(root.resolve("data"), (s,u) -> {
            assertTrue(u.contains("Student answer: test"));
            return "{\"score\":0,\"feedback\":\"The answer does not address the question. Explain requires and ensures.\"}";
        });
        var semester=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course=agent.createCourse(semester.id(),"EECS3311","#7799AA",Difficulty.MEDIUM);
        Path source=root.resolve("contracts.txt");
        Files.writeString(source,"A precondition is the caller's obligation. A postcondition is the method's guarantee.");
        var file=agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,source);
        var draft=agent.suggestEssayGrade(file.id(),"Explain preconditions and postconditions.","test");
        assertEquals(0,draft.score());
        var proposal=agent.proposeEssayGrade(draft,draft.score());
        assertTrue(agent.essayGrades(file.id()).isEmpty());
        agent.approveProposal(proposal.id());
        assertTrue(agent.pendingProposals().isEmpty());
        var restarted=new AgentController(root.resolve("data"),(s,u)->{throw new AssertionError("Reopening must not call AI.");});
        var grades=restarted.essayGrades(file.id());
        assertEquals(1,grades.size());
        assertEquals("test",grades.get(0).answer());
        assertEquals(0,grades.get(0).score());
        assertTrue(grades.get(0).feedback().contains("does not address"));
    }
}
