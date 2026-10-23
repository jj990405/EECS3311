package eecs3311.studyagent;

import java.io.IOException;

/** Command pattern: an approved action is the only path from proposal to mutation. */
@FunctionalInterface
public interface AgentCommand {
    void execute() throws IOException;
}
