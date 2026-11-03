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
            if (topic == null || topic.isBlank() || text == null || text.isBlank()
                    || explanation == null || explanation.isBlank()
                    || choices.size() != 4 || choices.stream().anyMatch(String::isBlank)
                    || answerIndex < 0 || answerIndex > 3)
                throw new IllegalArgumentException("Quiz questions need four choices and a valid answer");
        }
    }
    public record Quiz(UUID id, UUID courseId, UUID materialId, List<Question> questions) {
        public Quiz { questions = List.copyOf(questions); if (questions.isEmpty()) throw new IllegalArgumentException("Empty quiz"); }
    }
    public record Attempt(UUID id, UUID quizId, List<Integer> answers, int correct, Instant createdAt) {
        public Attempt { answers = List.copyOf(answers); }
    }
    public record EssayGrade(UUID id, UUID materialId, String question, String answer,
                             int score, String feedback, Instant createdAt) { }
    public record SelfAssessment(UUID id, UUID courseId, String topic, int confidence, Instant createdAt) { }
    public record EssayPrompt(UUID id, UUID courseId, UUID materialId, String topic, String question,
                              List<UUID> materialIds) {
        public EssayPrompt { materialIds = List.copyOf(materialIds); }
        public EssayPrompt(UUID id, UUID courseId, UUID materialId, String topic, String question) {
            this(id, courseId, materialId, topic, question, List.of(materialId));
        }
    }

    private final Path root;
    public StudyArtifacts(Path dataRoot) throws IOException {
        root = dataRoot.toAbsolutePath().normalize().resolve("artifacts");
        for (String name : List.of("summaries", "quizzes", "attempts", "essays", "assessments", "essay-prompts"))
            Files.createDirectories(root.resolve(name));
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

    public EssayGrade saveEssayGrade(UUID materialId, String question, String answer,
                                    int score, String feedback) throws IOException {
        EssayGrade item = new EssayGrade(UUID.randomUUID(), materialId, question, answer, score, feedback, Instant.now());
        save("essays", item.id(), Map.of("id", item.id().toString(), "material", materialId.toString(),
                "question", question, "answer", answer, "score", score, "feedback", feedback,
                "created", item.createdAt().toString()));
        return item;
    }
    public List<EssayGrade> essayGrades(UUID materialId) throws IOException {
        List<EssayGrade> result = new ArrayList<>();
        for (var value : load("essays")) if (value.get("material").equals(materialId.toString()))
            result.add(new EssayGrade(UUID.fromString(Json.string(value, "id")), materialId,
                    Json.string(value, "question"), Json.string(value, "answer"),
                    ((Number) value.get("score")).intValue(), Json.string(value, "feedback"),
                    Instant.parse(Json.string(value, "created"))));
        return result;
    }

    public SelfAssessment saveAssessment(UUID courseId, String topic, int confidence) throws IOException {
        if (topic == null || topic.isBlank() || confidence < 1 || confidence > 5)
            throw new IllegalArgumentException("Topic is required and confidence must be 1-5.");
        SelfAssessment item = new SelfAssessment(UUID.randomUUID(), courseId, topic.trim(), confidence, Instant.now());
        save("assessments", item.id(), Map.of("id", item.id().toString(), "course", courseId.toString(),
                "topic", item.topic(), "confidence", confidence, "created", item.createdAt().toString()));
        return item;
    }
    public List<SelfAssessment> assessments(UUID courseId) throws IOException {
        List<SelfAssessment> result = new ArrayList<>();
        for (var value : load("assessments")) if (value.get("course").equals(courseId.toString()))
            result.add(new SelfAssessment(UUID.fromString(Json.string(value, "id")), courseId,
                    Json.string(value, "topic"), ((Number) value.get("confidence")).intValue(),
                    Instant.parse(Json.string(value, "created"))));
        return result;
    }
    public EssayPrompt saveEssayPrompt(UUID courseId, UUID materialId, String topic, String question) throws IOException {
        return saveEssayPrompt(courseId, List.of(materialId), topic, question);
    }
    public EssayPrompt saveEssayPrompt(UUID courseId, List<UUID> materialIds, String topic,
                                      String question) throws IOException {
        if (topic.isBlank() || question.isBlank()) throw new IllegalArgumentException("Essay prompt must have a topic and question.");
        if (materialIds.isEmpty()) throw new IllegalArgumentException("Essay prompt needs source files.");
        UUID materialId = materialIds.get(0);
        EssayPrompt item = new EssayPrompt(UUID.randomUUID(), courseId, materialId, topic, question, materialIds);
        save("essay-prompts", item.id(), Map.of("id", item.id().toString(), "course", courseId.toString(),
                "material", materialId.toString(), "materials", materialIds.stream().map(UUID::toString).toList(),
                "topic", topic, "question", question));
        return item;
    }
    public List<EssayPrompt> essayPrompts(UUID courseId) throws IOException {
        List<EssayPrompt> result = new ArrayList<>();
        for (var value : load("essay-prompts")) if (value.get("course").equals(courseId.toString()))
            result.add(new EssayPrompt(UUID.fromString(Json.string(value, "id")), courseId,
                    UUID.fromString(Json.string(value, "material")), Json.string(value, "topic"),
                    Json.string(value, "question"), value.containsKey("materials")
                            ? Json.array(value.get("materials")).stream().map(v -> UUID.fromString(v.toString())).toList()
                            : List.of(UUID.fromString(Json.string(value, "material")))));
        return result;
    }
}
