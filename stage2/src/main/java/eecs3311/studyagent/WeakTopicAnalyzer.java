package eecs3311.studyagent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Finds topics missed in saved attempts; repeats count toward weakness. */
public final class WeakTopicAnalyzer {
    public List<Map.Entry<String, Integer>> analyze(StudyArtifacts artifacts, UUID courseId) throws IOException {
        Map<String, Integer> misses = new HashMap<>();
        for (StudyArtifacts.Quiz quiz : artifacts.quizzes(courseId)) {
            for (StudyArtifacts.Attempt attempt : artifacts.attempts(quiz.id())) {
                for (int i = 0; i < quiz.questions().size(); i++)
                    if (!attempt.answers().get(i).equals(quiz.questions().get(i).answerIndex()))
                        misses.merge(quiz.questions().get(i).topic(), 1, Integer::sum);
            }
        }
        for (StudyArtifacts.SelfAssessment assessment : artifacts.assessments(courseId))
            misses.merge(assessment.topic(), 5 - assessment.confidence(), Integer::sum);
        return misses.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).toList();
    }
}
