package eecs3311.studyagent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PptxReader implements DocumentReader {
    @Override
    public List<String> extractText(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            List<? extends ZipEntry> slides = zip.stream()
                    .filter(entry -> entry.getName().matches("ppt/slides/slide[0-9]+\\.xml"))
                    .sorted(Comparator.comparingInt(PptxReader::slideNumber)).toList();
            if (slides.isEmpty()) throw new IOException("PPTX has no slides.");
            List<String> pages = new ArrayList<>();
            for (ZipEntry slide : slides) {
                try (var stream = zip.getInputStream(slide)) {
                    pages.add(OfficeXmlReader.paragraphs(stream));
                }
            }
            return pages;
        }
    }

    private static int slideNumber(ZipEntry entry) {
        String name = entry.getName();
        return Integer.parseInt(name.substring("ppt/slides/slide".length(), name.length() - 4));
    }
}
