package eecs3311.studyagent;

import java.io.IOException;
import java.util.UUID;

public record AddDeadlineCommand(DataStore store, UUID courseId, ResponseParser.DeadlineDraft draft)
        implements AgentCommand {
    @Override public void execute() throws IOException {
        store.createDeadline(courseId, draft.title(), draft.type(), draft.dueDate(), draft.scope());
    }
}
