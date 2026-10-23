package eecs3311.studyagent;

import java.util.ArrayList;
import java.util.List;

/** Builds the same Composite tree for the Swing viewer and the CLI. */
public final class WorkspaceTree {
    private WorkspaceTree() { }

    public static List<WorkspaceNode> build(DataStore store) {
        List<WorkspaceNode> semesters = new ArrayList<>();
        for (Semester semester : store.semesters()) {
            List<WorkspaceNode> courses = new ArrayList<>();
            for (Course course : store.courses()) {
                if (!course.semesterId().equals(semester.id())) continue;
                List<WorkspaceNode> categories = new ArrayList<>();
                for (Category category : Category.values()) {
                    List<WorkspaceNode> files = store.materials().stream()
                            .filter(m -> m.courseId().equals(course.id()) && m.category() == category)
                            .map(m -> (WorkspaceNode) new WorkspaceLeaf(m)).toList();
                    categories.add(new WorkspaceBranch(category.toString(), WorkspaceBranch.Kind.CATEGORY,
                            course.id(), files));
                }
                courses.add(new WorkspaceBranch(course.name(), WorkspaceBranch.Kind.COURSE,
                        course.id(), categories));
            }
            semesters.add(new WorkspaceBranch(semester.name(), WorkspaceBranch.Kind.SEMESTER,
                    semester.id(), courses));
        }
        return semesters;
    }
}
