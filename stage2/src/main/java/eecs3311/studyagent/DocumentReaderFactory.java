package eecs3311.studyagent;

import java.util.Locale;
import java.util.Optional;

/** Selects a document reader by extension without format checks in the uploader. */
public final class DocumentReaderFactory {
    public Optional<DocumentReader> getReader(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return Optional.of(new PdfReader());
        if (lower.endsWith(".pptx")) return Optional.of(new PptxReader());
        if (lower.endsWith(".docx")) return Optional.of(new DocxReader());
        if (lower.endsWith(".txt")) return Optional.of(new TxtReader());
        return Optional.empty();
    }
}
