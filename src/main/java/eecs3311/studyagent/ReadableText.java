package eecs3311.studyagent;

import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;

/** Styled Markdown and app-owned local citation actions; never executes HTML or remote links. */
final class ReadableText extends JTextPane {
    private static final String SOURCE = "study-source";
    private record Link(GroundedAnswer.Citation citation, java.util.function.Consumer<GroundedAnswer.Citation> action) { }
    private java.util.function.Consumer<GroundedAnswer.Citation> openCitation;
    ReadableText() {
        setEditable(false); setBackground(AppTheme.SURFACE); setForeground(AppTheme.TEXT);
        setBorder(AppTheme.padding(12,14,12,14));
        var mouse = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getButton() == java.awt.event.MouseEvent.BUTTON1)
                    activateCitation(viewToModel2D(event.getPoint()));
            }
            @Override public void mouseMoved(java.awt.event.MouseEvent event) {
                var citation = citationAt(viewToModel2D(event.getPoint()));
                setCursor(java.awt.Cursor.getPredefinedCursor(citation == null
                        ? java.awt.Cursor.TEXT_CURSOR : java.awt.Cursor.HAND_CURSOR));
                setToolTipText(citation == null ? null : "Open " + citation.fileName() + " at page " + citation.page());
            }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "open-source");
        getActionMap().put("open-source", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { activateCitation(getCaretPosition()); }
        });
    }
    void onCitation(java.util.function.Consumer<GroundedAnswer.Citation> action) { openCitation = action; }
    GroundedAnswer.Citation citationAt(int position) {
        if (position < 0 || position >= getDocument().getLength()) return null;
        Object value = getStyledDocument().getCharacterElement(position).getAttributes().getAttribute(SOURCE);
        return value instanceof Link link ? link.citation() : null;
    }
    void activateCitation(int position) {
        if (position < 0 || position >= getDocument().getLength()) return;
        Object value = getStyledDocument().getCharacterElement(position).getAttributes().getAttribute(SOURCE);
        if (value instanceof Link link && link.action() != null) link.action().accept(link.citation());
    }
    void appendCitations(java.util.List<GroundedAnswer.Citation> citations) {
        if (citations.isEmpty()) return;
        appendMarkdown("\nSources:");
        try {
            int number = 0;
            for (var citation : citations) {
                var style = new SimpleAttributeSet();
                StyleConstants.setForeground(style, AppTheme.BLUE);
                StyleConstants.setUnderline(style, true);
                StyleConstants.setFontFamily(style, AppTheme.BODY.getFamily());
                StyleConstants.setFontSize(style, 15);
                style.addAttribute(SOURCE, new Link(citation, openCitation));
                getStyledDocument().insertString(getDocument().getLength(),
                        "[" + (++number) + "] " + citation.fileName() + " p." + citation.page(), style);
                getStyledDocument().insertString(getDocument().getLength(), "\n", null);
            }
        } catch (BadLocationException ex) { throw new IllegalStateException(ex); }
    }
    void setMarkdown(String value) { setText(""); appendMarkdown(value); setCaretPosition(0); }
    void appendMarkdown(String value) {
        var doc = getStyledDocument();
        try {
            if (doc.getLength() > 0) doc.insertString(doc.getLength(), "\n", null);
            boolean code = false;
            for (String raw : value.split("\\R", -1)) {
                if (raw.strip().startsWith("```")) { code = !code; continue; }
                String line = raw;
                var attributes = new SimpleAttributeSet();
                StyleConstants.setFontFamily(attributes, code ? "Consolas" : AppTheme.BODY.getFamily());
                StyleConstants.setFontSize(attributes, 15);
                StyleConstants.setForeground(attributes, AppTheme.TEXT);
                StyleConstants.setSpaceBelow(attributes, 8);
                StyleConstants.setLineSpacing(attributes, 0.12f);
                if (!code && line.matches("^#{1,6} .*")) {
                    line = line.replaceFirst("^#{1,6} +", "");
                    StyleConstants.setBold(attributes,true); StyleConstants.setFontSize(attributes,18);
                }
                if (!code && line.startsWith("- ")) line = "• " + line.substring(2);
                int start = doc.getLength();
                if (code) doc.insertString(doc.getLength(),line,attributes);
                else {
                    var matcher = java.util.regex.Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`").matcher(line);
                    int previous = 0;
                    while (matcher.find()) {
                        doc.insertString(doc.getLength(),line.substring(previous,matcher.start()),attributes);
                        var span = new SimpleAttributeSet(attributes);
                        if (matcher.group(1) != null) StyleConstants.setBold(span,true);
                        else StyleConstants.setFontFamily(span,"Consolas");
                        doc.insertString(doc.getLength(),matcher.group(1) != null ? matcher.group(1) : matcher.group(2),span);
                        previous = matcher.end();
                    }
                    doc.insertString(doc.getLength(),line.substring(previous),attributes);
                }
                doc.insertString(doc.getLength(),"\n",attributes);
                doc.setParagraphAttributes(start,doc.getLength()-start,attributes,false);
            }
        } catch (BadLocationException ex) { throw new IllegalStateException(ex); }
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
}
