package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DocxReader implements DocumentReader {
    @Override
    public List<String> extractText(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("word/document.xml");
            if (entry == null) throw new IOException("DOCX has no word/document.xml.");
            try (var stream = zip.getInputStream(entry)) {
                return List.of(OfficeXmlReader.paragraphs(stream));
            }
        }
    }
}
