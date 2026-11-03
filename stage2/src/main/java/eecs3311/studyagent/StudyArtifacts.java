package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persisted AI summaries, multiple-choice quizzes, and deterministic attempts. */
public final class StudyArtifacts {
    public record Summary(UUID id, UUID materialId, String text, Instant createdAt) { }
    public record Question(String topic, String text, List<String> choices, int answerIndex, String explanation) {
        public Question {
            choices = List.copyOf(choices);
            if (choices.size() != 4 || answerIndex < 0 || answerIndex > 3)
                throw new IllegalArgumentException("Quiz questions need four choices and a valid answer");
        }
    }
    public record Quiz(UUID id, UUID courseId, UUID materialId, List<Question> questions) {
        public Quiz { questions = List.copyOf(questions); if (questions.isEmpty()) throw new IllegalArgumentException("Empty quiz"); }
    }
    public record Attempt(UUID id, UUID quizId, List<Integer> answers, int correct, Instant createdAt) {
        public Attempt { answers = List.copyOf(answers); }
    }

    private final Path root;
    public StudyArtifacts(Path dataRoot) throws IOException {
        root = dataRoot.toAbsolutePath().normalize().resolve("artifacts");
        for (String name : List.of("summaries", "quizzes", "attempts")) Files.createDirectories(root.resolve(name));
    }

    private void save(String folder, UUID id, Object value) throws IOException {
        Files.writeString(root.resolve(folder).resolve(id + ".json"), Json.write(value));
    }
    private List<Map<String, Object>> load(String folder) throws IOException {
        try (var files = Files.list(root.resolve(folder))) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList())
                result.add(Json.object(Json.parse(Files.readString(file))));
            return result;
        }
    }

    public Summary saveSummary(UUID materialId, String text) throws IOException {
        Summary item = new Summary(UUID.randomUUID(), materialId, text, Instant.now());
        save("summaries", item.id(), Map.of("id", item.id().toString(), "material", materialId.toString(),
                "text", text, "created", item.createdAt().toString()));
        return item;
    }
    public List<Summary> summaries(UUID materialId) throws IOException {
        List<Summary> result = new ArrayList<>();
        for (var value : load("summaries")) if (value.get("material").equals(materialId.toString()))
            result.add(new Summary(UUID.fromString(Json.string(value, "id")), materialId,
                    Json.string(value, "text"), Instant.parse(Json.string(value, "created"))));
        return result;
    }

    public Quiz saveQuiz(UUID courseId, UUID materialId, List<Question> questions) throws IOException {
        Quiz quiz = new Quiz(UUID.randomUUID(), courseId, materialId, questions);
        save("quizzes", quiz.id(), Map.of("id", quiz.id().toString(), "course", courseId.toString(),
                "material", materialId.toString(), "questions", questions.stream().map(q -> Map.of(
                        "topic", q.topic(), "text", q.text(), "choices", q.choices(),
                        "answerIndex", q.answerIndex(), "explanation", q.explanation())).toList()));
        return quiz;
    }
    public List<Quiz> quizzes(UUID courseId) throws IOException {
        List<Quiz> result = new ArrayList<>();
        for (var value : load("quizzes")) if (value.get("course").equals(courseId.toString())) {
            List<Question> questions = new ArrayList<>();
            for (Object raw : Json.array(value.get("questions"))) {
                var q = Json.object(raw);
                questions.add(new Question(Json.string(q, "topic"), Json.string(q, "text"),
                        Json.array(q.get("choices")).stream().map(Object::toString).toList(),
                        ((Number) q.get("answerIndex")).intValue(), Json.string(q, "explanation")));
            }
            result.add(new Quiz(UUID.fromString(Json.string(value, "id")), courseId,
                    UUID.fromString(Json.string(value, "material")), questions));
        }
        return result;
    }
    public Attempt grade(Quiz quiz, List<Integer> answers) throws IOException {
        if (answers.size() != quiz.questions().size()) throw new IllegalArgumentException("Answer every question.");
        for (int answer : answers) if (answer < 0 || answer > 3) throw new IllegalArgumentException("Choice must be 0-3.");
        int correct = 0;
        for (int i = 0; i < answers.size(); i++) if (answers.get(i) == quiz.questions().get(i).answerIndex()) correct++;
        Attempt result = new Attempt(UUID.randomUUID(), quiz.id(), answers, correct, Instant.now());
        save("attempts", result.id(), Map.of("id", result.id().toString(), "quiz", quiz.id().toString(),
                "answers", answers, "correct", correct, "created", result.createdAt().toString()));
        return result;
    }
    public List<Attempt> attempts(UUID quizId) throws IOException {
        List<Attempt> result = new ArrayList<>();
        for (var value : load("attempts")) if (value.get("quiz").equals(quizId.toString()))
            result.add(new Attempt(UUID.fromString(Json.string(value, "id")), quizId,
                    Json.array(value.get("answers")).stream().map(n -> ((Number) n).intValue()).toList(),
                    ((Number) value.get("correct")).intValue(), Instant.parse(Json.string(value, "created"))));
        return result;
    }
}
