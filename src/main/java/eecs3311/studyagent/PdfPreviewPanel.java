package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Renders the stored PDF itself; extracted text is reserved for search and AI. */
final class PdfPreviewPanel extends JPanel {
    private final Path path;
    private final int pages;
    private final JLabel pageLabel = new JLabel();
    private final JLabel zoomLabel = new JLabel();
    private final JLabel imageLabel = new JLabel("Loading PDF…", SwingConstants.CENTER);
    private final JButton previous = AppTheme.button("‹", false);
    private final JButton next = AppTheme.button("›", false);
    private final JPanel canvas = new JPanel(new GridBagLayout());
    private final JScrollPane scroll = new JScrollPane(canvas);
    private final double[] pageWidths;
    private final javax.swing.Timer resizeTimer;
    private int page = 1;
    private double zoom = 1.0;
    private boolean fitWidth = true;
    private int renderedViewportWidth;
    private int request;
    private SwingWorker<BufferedImage, Void> renderWorker;
    private final Runnable studyNotes;
    private final Runnable wholeFileQuiz;

    PdfPreviewPanel(Path path) throws java.io.IOException {
        this(path, null);
    }

    PdfPreviewPanel(Path path, java.util.function.IntConsumer quizSlides) throws java.io.IOException {
        this(path, quizSlides, null, null);
    }

    PdfPreviewPanel(Path path, java.util.function.IntConsumer quizSlides, Runnable notes, Runnable quizFile) throws java.io.IOException {
        super(new BorderLayout());
        studyNotes = notes; wholeFileQuiz = quizFile;
        this.path = path;
        try (var pdf = Loader.loadPDF(path.toFile())) {
            pages = pdf.getNumberOfPages();
            pageWidths = new double[pages];
            for (int i = 0; i < pages; i++) {
                var sheet = pdf.getPage(i);
                pageWidths[i] = sheet.getRotation() % 180 == 0
                        ? sheet.getCropBox().getWidth() : sheet.getCropBox().getHeight();
            }
        }
        if (pages == 0) throw new java.io.IOException("This PDF has no pages.");
        setBackground(AppTheme.BACKGROUND);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(AppTheme.SURFACE);
        toolbar.setBorder(AppTheme.padding(8, 12, 8, 12));
        JPanel navigation = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        navigation.setOpaque(false);
        previous.addActionListener(e -> showPage(page - 1));
        next.addActionListener(e -> showPage(page + 1));
        navigation.add(previous);
        navigation.add(pageLabel);
        navigation.add(next);
        if (quizSlides != null) {
            JButton ai = AppTheme.dropdown("AI", false);
            ai.addActionListener(e -> slideMenu(quizSlides).show(ai, 0, ai.getHeight()));
            navigation.add(ai);
            java.awt.event.MouseAdapter popup = new java.awt.event.MouseAdapter() {
                private void show(java.awt.event.MouseEvent e) {
                    if (e.isPopupTrigger()) slideMenu(quizSlides).show(e.getComponent(), e.getX(), e.getY());
                }
                @Override public void mousePressed(java.awt.event.MouseEvent e) { show(e); }
                @Override public void mouseReleased(java.awt.event.MouseEvent e) { show(e); }
            };
            imageLabel.addMouseListener(popup); canvas.addMouseListener(popup);
        }
        toolbar.add(navigation, BorderLayout.WEST);
        JPanel scaling = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        scaling.setOpaque(false);
        JButton zoomOut = AppTheme.button("−", false);
        JButton zoomIn = AppTheme.button("+", false);
        JButton fit = AppTheme.button("Fit width", false);
        zoomOut.addActionListener(e -> setZoom(Math.max(0.5, zoom - 0.25)));
        zoomIn.addActionListener(e -> setZoom(Math.min(2.0, zoom + 0.25)));
        fit.addActionListener(e -> { fitWidth = true; showPage(page); });
        scaling.add(zoomOut);
        scaling.add(zoomLabel);
        scaling.add(zoomIn);
        scaling.add(fit);
        toolbar.add(scaling, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
        canvas.setBackground(AppTheme.BACKGROUND);
        canvas.setBorder(AppTheme.padding(12, 12, 12, 12));
        imageLabel.setForeground(AppTheme.MUTED);
        imageLabel.setVerticalAlignment(SwingConstants.TOP);
        canvas.add(imageLabel);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
        resizeTimer = new javax.swing.Timer(180, e -> {
            if (fitWidth && Math.abs(scroll.getViewport().getWidth() - renderedViewportWidth) > 16)
                showPage(page);
        });
        resizeTimer.setRepeats(false);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                if (fitWidth) resizeTimer.restart();
            }
        });
        showPage(1);
    }

    private javax.swing.JPopupMenu slideMenu(java.util.function.IntConsumer action) {
        int selectedPage = page;
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem current = new javax.swing.JMenuItem("Quiz this slide (page " + selectedPage + ")...");
        current.addActionListener(e -> action.accept(selectedPage)); menu.add(current);
        javax.swing.JMenuItem range = new javax.swing.JMenuItem("Quiz pages / slides...");
        range.addActionListener(e -> action.accept(0)); menu.add(range);
        if (studyNotes != null) {
            menu.addSeparator();
            javax.swing.JMenuItem notes = new javax.swing.JMenuItem("Study notes for this file");
            notes.addActionListener(e -> studyNotes.run()); menu.add(notes);
        }
        if (wholeFileQuiz != null) {
            javax.swing.JMenuItem quiz = new javax.swing.JMenuItem("Quiz whole file...");
            quiz.addActionListener(e -> wholeFileQuiz.run()); menu.add(quiz);
        }
        return menu;
    }

    private void setZoom(double value) {
        fitWidth = false;
        zoom = value;
        showPage(page);
    }

    void showPage(int requestedPage) {
        if (requestedPage < 1 || requestedPage > pages) return;
        boolean pageChanged = page != requestedPage;
        page = requestedPage;
        int currentRequest = ++request;
        if (renderWorker != null) renderWorker.cancel(true);
        int viewportWidth = scroll.getViewport().getWidth();
        renderedViewportWidth = viewportWidth;
        double scale = fitWidth && viewportWidth > 100
                ? Math.max(0.2, Math.min(2.5, (viewportWidth - 32.0)
                    / (pageWidths[page - 1] * 96.0 / 72.0))) : zoom;
        if (fitWidth) zoom = scale;
        var graphics = getGraphicsConfiguration();
        double deviceScale = graphics == null ? 1.0 : graphics.getDefaultTransform().getScaleX();
        double rasterScale = Math.min(3.0, Math.max(2.0, deviceScale * 1.5));
        pageLabel.setText("Page " + page + " of " + pages);
        zoomLabel.setText((fitWidth ? "Fit · " : "") + Math.round(scale * 100) + "%");
        previous.setEnabled(page > 1);
        next.setEnabled(page < pages);
        imageLabel.setIcon(null);
        imageLabel.setText("Rendering page " + page + "…");
        renderWorker = new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() throws Exception {
                try (var pdf = Loader.loadPDF(path.toFile())) {
                    PDFRenderer renderer = new PDFRenderer(pdf);
                    var hints = new java.awt.RenderingHints(java.awt.RenderingHints.KEY_RENDERING,
                            java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                    hints.put(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    hints.put(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    hints.put(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    hints.put(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);
                    renderer.setRenderingHints(hints);
                    renderer.setSubsamplingAllowed(false);
                    return renderer.renderImageWithDPI(requestedPage - 1,
                            (float) (96 * scale * rasterScale), ImageType.RGB);
                }
            }
            @Override protected void done() {
                if (currentRequest != request) return;
                try {
                    BufferedImage image = get();
                    imageLabel.setText("");
                    PdfPageIcon icon = new PdfPageIcon(image, rasterScale);
                    imageLabel.setIcon(icon);
                    imageLabel.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
                    canvas.revalidate();
                    if (pageChanged) scroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
                } catch (java.util.concurrent.CancellationException ex) {
                    // A newer page/zoom request owns the preview now.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    imageLabel.setText("Rendering interrupted.");
                } catch (ExecutionException ex) {
                    imageLabel.setText("Unable to render PDF: " + ex.getCause().getMessage());
                }
            }
        };
        renderWorker.execute();
    }

    /** Retains high-resolution pixels while Swing lays out the page in logical screen units. */
    static final class PdfPageIcon implements javax.swing.Icon {
        private final BufferedImage image;
        private final int width, height;
        PdfPageIcon(BufferedImage image, double rasterScale) {
            this.image = image;
            width = Math.max(1, (int)Math.round(image.getWidth() / rasterScale));
            height = Math.max(1, (int)Math.round(image.getHeight() / rasterScale));
        }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D copy = (java.awt.Graphics2D)g.create();
            try {
                copy.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                copy.drawImage(image, x, y, width, height, null);
            } finally { copy.dispose(); }
        }
    }
}
