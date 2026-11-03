package eecs3311.studyagent;

import org.junit.jupiter.api.Test;

/** Runs the integration checks through Maven's standard test lifecycle. */
final class AgentControllerMavenTest {
    @Test
    void integrationWorkflow() throws Exception {
        AgentControllerTest.main(new String[0]);
    }
}
