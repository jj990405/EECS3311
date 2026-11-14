package eecs3311.studyagent;

import java.awt.Dimension;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;

/** Modal host for the question workspace and its answer review. */
final class QuizDialog {
    private QuizDialog() { }
    static List<Integer> take(JFrame owner, StudyArtifacts.Quiz quiz) {
        JDialog dialog = window(owner, "Practice quiz");
        var result = new java.util.concurrent.atomic.AtomicReference<List<Integer>>();
        QuizSessionPanel[] panel = new QuizSessionPanel[1];
        panel[0] = new QuizSessionPanel(quiz, null, () -> {
            result.set(panel[0].answers()); dialog.dispose();
        }, dialog::dispose);
        dialog.setContentPane(panel[0]); dialog.setVisible(true); return result.get();
    }
    static void showResult(JFrame owner, StudyArtifacts.Quiz quiz, List<Integer> answers,
                           StudyArtifacts.Attempt attempt) {
        JDialog dialog = window(owner, "Quiz results");
        dialog.setContentPane(new QuizSessionPanel(quiz, answers, dialog::dispose, dialog::dispose));
        dialog.setVisible(true);
    }
    private static JDialog window(JFrame owner, String title) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(760, 560)); dialog.setSize(1080, 780);
        dialog.setLocationRelativeTo(owner); return dialog;
    }
}