package eecs3311.studyagent;

import java.util.List;
import java.util.UUID;

public record WorkspaceBranch(String label, Kind kind, UUID id, List<WorkspaceNode> children)
        implements WorkspaceNode {
    public enum Kind { SEMESTER, COURSE, CATEGORY }

    public WorkspaceBranch {
        children = List.copyOf(children);
    }

    @Override
    public String toString() {
        return label;
    }
}
