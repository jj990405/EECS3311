package eecs3311.studyagent;

import java.util.List;

public record WorkspaceLeaf(MaterialFile file) implements WorkspaceNode {
    @Override
    public String label() {
        return file.originalName();
    }

    @Override
    public List<WorkspaceNode> children() {
        return List.of();
    }

    @Override
    public String toString() {
        return label();
    }
}
