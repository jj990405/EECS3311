package eecs3311.studyagent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TxtReader implements DocumentReader {
    @Override
    public List<String> extractText(Path file) throws IOException {
        return List.of(Files.readString(file, StandardCharsets.UTF_8));
    }
}
