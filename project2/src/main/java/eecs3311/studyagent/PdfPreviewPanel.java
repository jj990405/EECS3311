package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.ImageIcon;
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
    private int page = 1;
    private double zoom = 1.0;
    private int request;

    PdfPreviewPanel(Path path) throws java.io.IOException {
        super(new BorderLayout());
        this.path = path;
        try (var pdf = Loader.loadPDF(path.toFile())) { pages = pdf.getNumberOfPages(); }
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
        toolbar.add(navigation, BorderLayout.WEST);
        JPanel scaling = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        scaling.setOpaque(false);
        JButton zoomOut = AppTheme.button("−", false);
        JButton zoomIn = AppTheme.button("+", false);
        zoomOut.addActionListener(e -> setZoom(Math.max(0.5, zoom - 0.25)));
        zoomIn.addActionListener(e -> setZoom(Math.min(2.0, zoom + 0.25)));
        scaling.add(zoomOut);
        scaling.add(zoomLabel);
        scaling.add(zoomIn);
        toolbar.add(scaling, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
        canvas.setBackground(AppTheme.BACKGROUND);
        canvas.setBorder(AppTheme.padding(24, 24, 24, 24));
        imageLabel.setForeground(AppTheme.MUTED);
        imageLabel.setVerticalAlignment(SwingConstants.TOP);
        canvas.add(imageLabel);
        JScrollPane scroll = new JScrollPane(canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
        showPage(1);
    }

    private void setZoom(double value) {
        zoom = value;
        showPage(page);
    }

    private void showPage(int requestedPage) {
        if (requestedPage < 1 || requestedPage > pages) return;
        page = requestedPage;
        int currentRequest = ++request;
        double scale = zoom;
        pageLabel.setText("Page " + page + " of " + pages);
        zoomLabel.setText(Math.round(zoom * 100) + "%");
        previous.setEnabled(page > 1);
        next.setEnabled(page < pages);
        imageLabel.setIcon(null);
        imageLabel.setText("Rendering page " + page + "…");
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() throws Exception {
                try (var pdf = Loader.loadPDF(path.toFile())) {
                    return new PDFRenderer(pdf).renderImageWithDPI(requestedPage - 1,
                            (float) (96 * scale), ImageType.RGB);
                }
            }
            @Override protected void done() {
                if (currentRequest != request) return;
                try {
                    BufferedImage image = get();
                    imageLabel.setText("");
                    imageLabel.setIcon(new ImageIcon(image));
                    imageLabel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                    canvas.revalidate();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    imageLabel.setText("Rendering interrupted.");
                } catch (ExecutionException ex) {
                    imageLabel.setText("Unable to render PDF: " + ex.getCause().getMessage());
                }
            }
        }.execute();
    }
}
