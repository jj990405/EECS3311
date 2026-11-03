package eecs3311.studyagent;

import java.util.List;

/** Common component for the semester -> course -> category -> file tree. */
public sealed interface WorkspaceNode permits WorkspaceBranch, WorkspaceLeaf {
    String label();
    List<WorkspaceNode> children();
}
