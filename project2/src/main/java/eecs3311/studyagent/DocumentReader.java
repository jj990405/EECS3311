package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** A page, slide, or text document becomes one element in the returned list. */
public interface DocumentReader {
    List<String> extractText(Path file) throws IOException;
}
