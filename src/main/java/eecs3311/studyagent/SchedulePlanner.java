package eecs3311.studyagent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Converts a natural-language edit into one reviewable Command proposal. */
public final class SchedulePlanner {
    public CommandInvoker.Proposal plan(DataStore store, LlmClient llm, PromptBuilder prompts,
                                        ResponseParser parser, CommandInvoker invoker,
                                        UUID courseId, String request) throws IOException {
        Course course = store.course(courseId);
        StringBuilder context = new StringBuilder("Course: " + course.name() + "\nExisting deadlines:\n");
        for (Deadline deadline : store.deadlines()) if (deadline.courseId().equals(courseId))
            context.append(deadline.id()).append(" | ").append(deadline.title()).append(" | ")
                    .append(deadline.dueDate()).append(" | type: ").append(deadline.type())
                    .append(" | scope: ").append(deadline.scope()).append('\n');
        Map<String, Object> plan = parser.parse(llm.complete(prompts.system(PromptBuilder.Task.SCHEDULE_PLAN),
                prompts.user(PromptBuilder.Task.SCHEDULE_PLAN, context.toString(), request)));
        String action = Json.string(plan, "action");
        if (action.equals("clarify"))
            throw new IllegalArgumentException("Please clarify the schedule request: "
                    + Json.string(plan, "clarification"));
        Object idValue = plan.get("deadlineId");
        UUID deadlineId = idValue == null ? null : UUID.fromString(idValue.toString());
        if (!action.equals("add")) {
            if (deadlineId == null || !store.deadline(deadlineId).courseId().equals(courseId))
                throw new IllegalArgumentException("AI selected a deadline outside this course.");
        }
        if (action.equals("delete"))
            return invoker.propose("Before: " + describe(store.deadline(deadlineId)) + "\nAfter: deleted",
                    new DeleteDeadlineCommand(store, deadlineId));
        if (!action.equals("add") && !action.equals("edit")) throw new IllegalArgumentException("Unsupported schedule action.");
        Object rawDate = plan.get("dueDate");
        LocalDate date = rawDate == null ? null : LocalDate.parse(rawDate.toString());
        Deadline existing = action.equals("edit") ? store.deadline(deadlineId) : null;
        ResponseParser.DeadlineDraft draft = new ResponseParser.DeadlineDraft(
                textOrExisting(plan, "title", existing == null ? null : existing.title()),
                textOrExisting(plan, "type", existing == null ? null : existing.type()), date,
                textOrExisting(plan, "scope", existing == null ? "" : existing.scope()),
                "User request: " + request);
        new DeadlineValidator().validate(draft, store.semester(course.semesterId()));
        AgentCommand command = action.equals("add") ? new AddDeadlineCommand(store, courseId, draft)
                : new EditDeadlineCommand(store, deadlineId, draft);
        String before = action.equals("add") ? "none" : describe(store.deadline(deadlineId));
        return invoker.propose("Before: " + before + "\nAfter: " + draft.title() + " / "
                + (date == null ? "TBD" : date) + " / " + draft.scope(), command);
    }

    private String describe(Deadline deadline) {
        return deadline.title() + " / " + (deadline.dueDate() == null ? "TBD" : deadline.dueDate())
                + " / " + deadline.scope();
    }

    private String textOrExisting(Map<String, Object> plan, String field, String existing) {
        Object value = plan.get(field);
        if (value instanceof String text && (!text.isBlank() || field.equals("scope"))) return text;
        if (existing != null) return existing;
        throw new IllegalArgumentException("Please specify the deadline " + field + ".");
    }
}
