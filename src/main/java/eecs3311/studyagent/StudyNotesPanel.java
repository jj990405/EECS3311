package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

/** Readable, source-linked study notes displayed beside the workspace. */
final class StudyNotesPanel extends JPanel {
    StudyNotesPanel(String fileName, String notes, Runnable openSource) {
        this(fileName, notes, openSource, () -> { });
    }

    StudyNotesPanel(String fileName, String notes, Runnable openSource, Runnable regenerate) {
        super(new BorderLayout(0, 12));
        setBorder(AppTheme.padding(18, 22, 18, 22));
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        JLabel title = new JLabel("Study notes");
        title.setFont(AppTheme.TITLE);
        heading.add(title, BorderLayout.NORTH);
        JLabel file = new JLabel(fileName);
        file.setForeground(AppTheme.MUTED);
        file.setToolTipText(fileName);
        heading.add(file, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        if (!notes.stripLeading().startsWith("## ")) {
            JButton rewrite = AppTheme.button("Make a study guide", true);
            rewrite.setToolTipText("Create a new, shorter guide from the original file using AI");
            rewrite.addActionListener(event -> regenerate.run());
            actions.add(rewrite);
        }
        JButton source = AppTheme.button(fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                ? "Open source PDF" : "Open source file", false);
        source.addActionListener(event -> openSource.run());
        actions.add(source);
        heading.add(actions, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);

        JTextPane content = new JTextPane();
        content.setEditable(false);
        content.setBackground(AppTheme.SURFACE);
        content.setForeground(AppTheme.TEXT);
        content.setBorder(AppTheme.padding(14, 16, 14, 16));
        content.setDocument(styledNotes(notes));
        content.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));
        add(scroll, BorderLayout.CENTER);
    }

    private DefaultStyledDocument styledNotes(String notes) {
        DefaultStyledDocument document = new DefaultStyledDocument();
        Style body = document.addStyle("body", null);
        StyleConstants.setFontFamily(body, AppTheme.BODY.getFamily());
        StyleConstants.setFontSize(body, 14);
        StyleConstants.setForeground(body, AppTheme.TEXT);
        StyleConstants.setSpaceBelow(body, 8);
        Style page = document.addStyle("page", body);
        StyleConstants.setBold(page, true);
        StyleConstants.setFontSize(page, 17);
        StyleConstants.setSpaceAbove(page, 14);
        StyleConstants.setSpaceBelow(page, 8);
        Style bullet = document.addStyle("bullet", body);
        StyleConstants.setLeftIndent(bullet, 22);
        StyleConstants.setFirstLineIndent(bullet, -12);
        StyleConstants.setSpaceBelow(bullet, 9);
        Style sources = document.addStyle("sources", body);
        StyleConstants.setForeground(sources, AppTheme.MUTED);
        StyleConstants.setFontSize(sources, 12);
        for (String raw : notes.split("\\R", -1)) {
            String line = raw.strip();
            if (line.isEmpty()) continue;
            if (line.matches("Page \\d+.*") || line.startsWith("## ")) {
                append(document, line.startsWith("## ") ? line.substring(3) : line, page);
            } else if (line.startsWith("Sources:")) {
                append(document, line, sources);
            } else if (line.startsWith("- ") || line.startsWith("• ")) {
                append(document, "• " + line.substring(2).strip(), bullet);
            } else {
                // Saved notes from older versions were paragraphs. Keep their content,
                // but break them into points so they are usable while studying.
                for (String sentence : line.split("(?<=[.!?])\\s+(?=[A-Z])"))
                    append(document, "• " + sentence.strip(), bullet);
            }
        }
        return document;
    }

    private void append(DefaultStyledDocument document, String line, Style style) {
        try { document.insertString(document.getLength(), line + "\n", style); }
        catch (BadLocationException ex) { throw new IllegalStateException(ex); }
    }
}
