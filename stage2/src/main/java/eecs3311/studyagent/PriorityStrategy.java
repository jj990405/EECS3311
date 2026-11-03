package eecs3311.studyagent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Strategy boundary for deadline ordering policies. */
public interface PriorityStrategy {
    List<Deadline> order(List<Deadline> deadlines, Map<UUID, Course> courses);
}
