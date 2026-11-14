package eecs3311.studyagent;

final class AiFeedback {
    private AiFeedback() { }
    enum Action { QUIZ, SLIDE_QUIZ, NOTES, QUESTION }
    static Action action(String message) {
        String text = message.strip().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[!.?]+$", "").strip().replaceAll("\\s+", " ");
        if (java.util.Set.of("quiz", "practice quiz", "create quiz", "create a quiz", "make a quiz",
                "give me a quiz", "/quiz", "퀴즈", "퀴즈 내줘", "퀴즈 만들어줘").contains(text)) return Action.QUIZ;
        if (java.util.Set.of("slide quiz", "quiz slides", "/slide quiz", "슬라이드 퀴즈").contains(text))
            return Action.SLIDE_QUIZ;
        if (java.util.Set.of("study notes", "notes", "summary", "/notes", "공부 노트", "요약").contains(text))
            return Action.NOTES;
        return Action.QUESTION;
    }
    static String greeting(String question) {
        String normalized = question.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[!.?]+$", "");
        return java.util.Set.of("hi", "hello", "hey", "안녕", "안녕하세요").contains(normalized)
                ? "Hi! Select a course or lecture, then ask a question about its uploaded materials. "
                  + "Type quiz for an interactive practice quiz, slide quiz for selected pages, or notes for a study guide. "
                  + "I can also explain concepts with source citations. Use Change schedule for deadline requests." : null;
    }
    static String failure(Throwable error) {
        while (error.getCause() != null && (error instanceof java.util.concurrent.ExecutionException
                || error instanceof java.util.concurrent.CompletionException)) error = error.getCause();
        if (error instanceof java.net.http.HttpTimeoutException)
            return "The AI request timed out. Retry, or choose fewer pages for a slide quiz.";
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        return message.replaceAll("sk-[A-Za-z0-9_-]+", "[hidden key]");
    }
}
