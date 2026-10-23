package eecs3311.studyagent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Extracts visible paragraph text from Office Open XML without loading external entities. */
final class OfficeXmlReader {
    private OfficeXmlReader() { }

    static String paragraphs(InputStream source) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(source);
            NodeList paragraphs = document.getElementsByTagNameNS("*", "p");
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < paragraphs.getLength(); i++) {
                StringBuilder line = new StringBuilder();
                collectText(paragraphs.item(i), line);
                if (!line.toString().isBlank()) lines.add(line.toString());
            }
            return String.join("\n", lines);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Cannot read Office XML text.", ex);
        }
    }

    private static void collectText(Node node, StringBuilder result) {
        if (node instanceof Element element && "t".equals(element.getLocalName())) {
            result.append(element.getTextContent());
            return;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) collectText(children.item(i), result);
    }
}
