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
    /** Embedded slide pictures, in physical slide order, without extracting ZIP paths to disk. */
    List<List<String>> images(Path file) throws IOException {
        try(ZipFile zip=new ZipFile(file.toFile())) {
            List<? extends ZipEntry> slides=zip.stream().filter(e->e.getName().matches("ppt/slides/slide[0-9]+\\.xml"))
                    .sorted(Comparator.comparingInt(PptxReader::slideNumber)).toList();
            List<List<String>> all=new ArrayList<>();
            for(ZipEntry slide:slides) {
                List<String> urls=new ArrayList<>();
                var rels=zip.getEntry("ppt/slides/_rels/slide"+slideNumber(slide)+".xml.rels");
                if(rels!=null) try(var input=zip.getInputStream(rels)) {
                    var nodes=OfficeXmlReader.document(input).getElementsByTagNameNS("*","Relationship");
                    for(int i=0;i<nodes.getLength();i++) {
                        var node=(org.w3c.dom.Element)nodes.item(i);
                        if(!node.getAttribute("Type").endsWith("/image") || "External".equals(node.getAttribute("TargetMode"))) continue;
                        String target=Path.of("ppt/slides").resolve(node.getAttribute("Target")).normalize().toString().replace('\\','/');
                        if(!target.startsWith("ppt/media/")) throw new IOException("Invalid embedded image path.");
                        ZipEntry image=zip.getEntry(target); if(image==null) throw new IOException("A slide image is missing: "+target);
                        try(var bytes=zip.getInputStream(image)) {
                            var bitmap=javax.imageio.ImageIO.read(bytes);
                            if(bitmap==null) throw new IOException("An embedded slide image cannot be decoded. Export the slides to PDF and upload that file.");
                            var encoded=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(bitmap,"png",encoded); bitmap.flush();
                            urls.add("data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(encoded.toByteArray()));
                        }
                    }
                }
                all.add(List.copyOf(urls));
            }
            return List.copyOf(all);
        }
    }
}
