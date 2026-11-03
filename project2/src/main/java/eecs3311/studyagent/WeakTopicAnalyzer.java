package eecs3311.studyagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Computes per-topic quiz accuracy and combines it with optional self-assessment. */
public final class WeakTopicAnalyzer {
    public record TopicStats(String topic, int correct, int total, Integer confidence) {
        public Integer accuracyPercent() {
            return total == 0 ? null : (int) Math.round(100.0 * correct / total);
        }

        public int weaknessScore() {
            int quizWeakness = total == 0 ? 0 : 100 - accuracyPercent();
            int confidenceWeakness = confidence == null ? 0 : (5 - confidence) * 30;
            return quizWeakness + confidenceWeakness;
        }
    }

    private static final class Counts {
        int correct;
        int total;
        Integer confidence;
    }

    public List<TopicStats> statistics(StudyArtifacts artifacts, UUID courseId) throws IOException {
        Map<String, Counts> counts = new HashMap<>();
        for (StudyArtifacts.Quiz quiz : artifacts.quizzes(courseId)) {
            for (StudyArtifacts.Attempt attempt : artifacts.attempts(quiz.id())) {
                for (int i = 0; i < quiz.questions().size(); i++) {
                    var question = quiz.questions().get(i);
                    Counts count = counts.computeIfAbsent(question.topic(), ignored -> new Counts());
                    count.total++;
                    if (attempt.answers().get(i).equals(question.answerIndex())) count.correct++;
                }
            }
        }
        List<StudyArtifacts.SelfAssessment> assessments = new ArrayList<>(artifacts.assessments(courseId));
        assessments.sort(java.util.Comparator.comparing(StudyArtifacts.SelfAssessment::createdAt));
        for (StudyArtifacts.SelfAssessment assessment : assessments)
            counts.computeIfAbsent(assessment.topic(), ignored -> new Counts()).confidence = assessment.confidence();
        return counts.entrySet().stream().map(entry -> new TopicStats(entry.getKey(),
                entry.getValue().correct, entry.getValue().total, entry.getValue().confidence))
                .sorted(java.util.Comparator.comparing(TopicStats::topic, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<Map.Entry<String, Integer>> analyze(StudyArtifacts artifacts, UUID courseId) throws IOException {
        return statistics(artifacts, courseId).stream().filter(stats -> stats.weaknessScore() > 0)
                .map(stats -> Map.entry(stats.topic(), stats.weaknessScore()))
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).toList();
    }
}
