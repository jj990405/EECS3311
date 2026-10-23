package eecs3311.studyagent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Validates model JSON before any proposed action reaches the user. */
public final class ResponseParser {
    public record DeadlineDraft(String title, String type, LocalDate dueDate, String scope, String evidence) { }

    public Map<String, Object> parse(String response) {
        String value = response.trim();
        if (value.startsWith("```")) {
            int newline = value.indexOf('\n');
            value = value.substring(newline + 1, value.lastIndexOf("```")).trim();
        }
        return Json.object(Json.parse(value));
    }

    public List<DeadlineDraft> deadlines(String response) {
        List<DeadlineDraft> result = new ArrayList<>();
        for (Object item : Json.array(parse(response).get("deadlines"))) {
            Map<String, Object> object = Json.object(item);
            Object rawDate = object.get("dueDate");
            LocalDate date = rawDate == null || rawDate.toString().isBlank() ? null : LocalDate.parse(rawDate.toString());
            result.add(new DeadlineDraft(Json.string(object, "title"), Json.string(object, "type"),
                    date, Json.string(object, "scope"), Json.string(object, "evidence")));
        }
        return result;
    }

    public Integer week(String response) {
        Object value = parse(response).get("week");
        if (value == null) return null;
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Week must be a number");
        int week = number.intValue();
        if (week < 1 || week > 18 || number.doubleValue() != week) throw new IllegalArgumentException("Week must be 1-18");
        return week;
    }
}
