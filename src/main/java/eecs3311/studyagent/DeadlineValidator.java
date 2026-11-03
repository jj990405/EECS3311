package eecs3311.studyagent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Rejects model dates outside the course semester and empty or unsupported suggestions. */
public final class DeadlineValidator {
    private static final Pattern WEEK = Pattern.compile("\\bweek\\s+(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUE_IN_24_HOURS = Pattern.compile("\\b(?:in|within)\\s+24\\s+hours\\b",
            Pattern.CASE_INSENSITIVE);

    public ResponseParser.DeadlineDraft resolveRelativeDate(ResponseParser.DeadlineDraft draft,
                                                              Semester semester) {
        if (draft.dueDate() != null) {
            if (DUE_IN_24_HOURS.matcher(draft.evidence()).find()
                    && mentionsDate(draft.dueDate(), draft.evidence()))
                return new ResponseParser.DeadlineDraft(draft.title(), draft.type(),
                        draft.dueDate().plusDays(1), draft.scope(), draft.evidence(), draft.weight());
            return draft;
        }
        var match = WEEK.matcher(draft.evidence());
        if (!match.find()) return draft;
        int week = Integer.parseInt(match.group(1));
        if (week < 1 || week > 18) return draft;
        return new ResponseParser.DeadlineDraft(draft.title(), draft.type(),
                semester.startDate().plusWeeks(week - 1L), draft.scope(), draft.evidence(), draft.weight());
    }

    public List<String> warnings(ResponseParser.DeadlineDraft draft, Semester semester, String sourceText) {
        List<String> warnings = new ArrayList<>();
        if (draft.evidence().isBlank() || !sourceText.toLowerCase(Locale.ROOT)
                .contains(draft.evidence().toLowerCase(Locale.ROOT)))
            warnings.add("The quoted evidence was not found verbatim in the source.");
        if (draft.dueDate() != null) {
            if (!semester.contains(draft.dueDate())) warnings.add("Date is outside the semester.");
            boolean nextDayOfRelease = DUE_IN_24_HOURS.matcher(draft.evidence()).find()
                    && mentionsDate(draft.dueDate().minusDays(1), draft.evidence());
            if (!mentionsDate(draft.dueDate(), sourceText) && !nextDayOfRelease
                    && !WEEK.matcher(draft.evidence()).find())
                warnings.add("The proposed date could not be matched to the source text.");
            if (nextDayOfRelease)
                warnings.add("Due 24 hours after release; confirm the exact cutoff time.");
        } else warnings.add("No specific date: this item will appear as TBD.");
        return warnings;
    }

    private boolean mentionsDate(LocalDate date, String text) {
        String source = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(\\d{1,2})(?:st|nd|rd|th)\\b", "$1")
                .replaceAll("\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\\.", "$1");
        String fullMonth = date.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
                .toLowerCase(Locale.ROOT);
        String shortMonth = date.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
                .toLowerCase(Locale.ROOT);
        String monthForms = fullMonth + "|" + shortMonth + (date.getMonthValue() == 9 ? "|sept" : "");
        String day = Integer.toString(date.getDayOfMonth());
        Pattern namedDate = Pattern.compile("\\b(?:" + monthForms + ")\\s+0?" + day
                + "\\b|\\b0?" + day + "\\s+(?:" + monthForms + ")\\b");
        return source.contains(date.toString()) || namedDate.matcher(source).find();
    }

    public void validate(ResponseParser.DeadlineDraft draft, Semester semester) {
        if (draft.title().isBlank() || draft.type().isBlank() || draft.evidence().isBlank())
            throw new IllegalArgumentException("AI deadline is missing title, type, or source evidence.");
        LocalDate date = draft.dueDate();
        if (date != null && !semester.contains(date))
            throw new IllegalArgumentException("Suggested date is outside the semester: " + date);
    }
}
