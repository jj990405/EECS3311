package eecs3311.studyagent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Dates entered by people may omit separators; stored dates remain ISO dates. */
final class FlexibleDate {
    private FlexibleDate() { }

    static LocalDate parse(String input) {
        String value = input.trim();
        try {
            if (value.matches("[0-9]{8}")) return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
            if (value.matches("[0-9]{4}[-/.][0-9]{1,2}[-/.][0-9]{1,2}")) {
                String[] parts = value.split("[-/.]");
                return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
            }
        } catch (java.time.DateTimeException ex) {
            throw new IllegalArgumentException("Enter a real date, for example 20261231 or 2026-12-31.");
        }
        throw new IllegalArgumentException("Enter a date like 20261231 or 2026-12-31.");
    }
}
