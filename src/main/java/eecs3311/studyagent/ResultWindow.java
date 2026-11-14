package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Screen-bounded, resizable result frame with the operating system's maximize control. */
final class ResultWindow extends JFrame {
    ResultWindow(JFrame owner, String title, String content, Runnable openSource) {
        this(owner, title, content, openSource, java.util.List.of(), null);
    }
    ResultWindow(JFrame owner, String title, GroundedAnswer answer,
                 java.util.function.Consumer<GroundedAnswer.Citation> openCitation) {
        this(owner, title, answer.answer(), null, answer.citations(), openCitation);
    }
    private ResultWindow(JFrame owner, String title, String content, Runnable openSource,
                         java.util.List<GroundedAnswer.Citation> citations,
                         java.util.function.Consumer<GroundedAnswer.Citation> openCitation) {
        super(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(AppTheme.padding(18, 22, 18, 22));
        JLabel heading = new JLabel(title);
        heading.setFont(AppTheme.SECTION);
        root.add(heading, BorderLayout.NORTH);
        ReadableText text = new ReadableText();
        text.setMarkdown(content);
        text.onCitation(openCitation);
        text.appendCitations(citations);
        text.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        root.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (openSource != null) {
            JButton source = AppTheme.button("Open cited source", false);
            source.addActionListener(event -> openSource.run());
            actions.add(source);
        }
        JButton close = AppTheme.button("Close", true);
        close.addActionListener(event -> dispose());
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
        Rectangle usable = new Rectangle(owner.getGraphicsConfiguration().getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(owner.getGraphicsConfiguration());
        usable.x += insets.left; usable.y += insets.top;
        usable.width -= insets.left + insets.right;
        usable.height -= insets.top + insets.bottom;
        Rectangle bounds = initialBounds(usable);
        setMinimumSize(new Dimension(Math.min(640, bounds.width), Math.min(360, bounds.height)));
        setBounds(bounds);
        WindowAdapter ownerListener = new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { dispose(); }
        };
        owner.addWindowListener(ownerListener);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { owner.removeWindowListener(ownerListener); }
        });
        getRootPane().registerKeyboardAction(event -> dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    static Rectangle initialBounds(Rectangle usable) {
        int width = Math.min(1200, Math.max(1, usable.width - 32));
        int height = Math.min(700, Math.min(Math.max(1, usable.height - 32), Math.max(1, width * 5 / 8)));
        return new Rectangle(usable.x + (usable.width - width) / 2,
                usable.y + (usable.height - height) / 2, width, height);
    }
}
