package eecs3311.studyagent;

import java.io.IOException;
import java.util.UUID;

public record DeleteDeadlineCommand(DataStore store, UUID id) implements AgentCommand {
    @Override public void execute() throws IOException { store.deleteDeadline(id); }
}
