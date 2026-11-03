package eecs3311.studyagent;

import java.io.IOException;
import java.util.UUID;

public record EditDeadlineCommand(DataStore store, UUID id, ResponseParser.DeadlineDraft draft)
        implements AgentCommand {
    @Override public void execute() throws IOException {
        store.editDeadline(id, draft.title(), draft.type(), draft.dueDate(), draft.scope());
    }
}
