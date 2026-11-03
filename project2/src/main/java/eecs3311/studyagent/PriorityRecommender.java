package eecs3311.studyagent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Earlier due dates come first; same-day ties favour the more difficult course. */
public final class PriorityRecommender implements PriorityStrategy {
    public List<Deadline> order(List<Deadline> deadlines, Map<UUID, Course> courses) {
        List<Deadline> result = new ArrayList<>(deadlines);
        result.sort(Comparator
                .comparing(Deadline::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(d -> courses.get(d.courseId()).difficulty().priority())
                .thenComparing(Deadline::title, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
}
