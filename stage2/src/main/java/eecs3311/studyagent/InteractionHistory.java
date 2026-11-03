package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Local audit trail of agent proposals, approvals, and study actions; excludes API credentials. */
public final class InteractionHistory {
    public record Entry(Instant time, String event, String detail) { }
    private final Path file;
    public InteractionHistory(Path dataRoot) throws IOException {
        file = dataRoot.toAbsolutePath().normalize().resolve("interaction-history.jsonl");
        Files.createDirectories(file.getParent());
    }
    public synchronized void add(String event, String detail) throws IOException {
        String safe = detail.length() > 1000 ? detail.substring(0, 1000) : detail;
        Files.writeString(file, Json.write(Map.of("time", Instant.now().toString(), "event", event,
                "detail", safe)) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public synchronized List<Entry> list() throws IOException {
        if (!Files.exists(file)) return List.of();
        List<Entry> result = new ArrayList<>();
        for (String line : Files.readAllLines(file)) if (!line.isBlank()) {
            Map<String, Object> value = Json.object(Json.parse(line));
            result.add(new Entry(Instant.parse(Json.string(value, "time")),
                    Json.string(value, "event"), Json.string(value, "detail")));
        }
        return result;
    }
    public synchronized List<Entry> recent(int count) throws IOException {
        List<Entry> all = list();
        return all.subList(Math.max(0, all.size() - count), all.size());
    }
    public synchronized void deleteAll() throws IOException { Files.deleteIfExists(file); }
}
