package eecs3311.studyagent;

import java.io.IOException;
import java.util.UUID;

public record AssignWeekCommand(DataStore store, UUID materialId, int week) implements AgentCommand {
    @Override public void execute() throws IOException { store.assignLectureWeek(materialId, week); }
}
