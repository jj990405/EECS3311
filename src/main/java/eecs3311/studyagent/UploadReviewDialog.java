package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Review every file destination; content inspection and optional AI never block the EDT. */
final class UploadReviewDialog extends JDialog {
    private final File[] files;
    private final AgentController controller;
    private final Category selectedFolder;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"File", "Folder", "Classification"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return column == 1; }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel("Reading document titles and page samples…");
    private final JButton upload = AppTheme.button("Upload files", true);
    private final JButton ai = AppTheme.button("AI sort files", false);
    private SwingWorker<?, ?> worker;
    private List<Category> result;

    private UploadReviewDialog(JFrame owner, AgentController controller, File[] files, Category selectedFolder) {
        super(owner, "Review file folders", true);
        this.files = files; this.controller = controller; this.selectedFolder = selectedFolder;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1060, 600); setMinimumSize(new Dimension(780, 440)); setLocationRelativeTo(owner);
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setBorder(AppTheme.padding(20, 24, 18, 24));
        JLabel title = new JLabel("Review " + files.length + " files"); title.setFont(AppTheme.TITLE);
        heading.add(title, BorderLayout.NORTH);
        heading.add(new JLabel("Upload reads and indexes all pages. Image slides use AI; originals stay unchanged."), BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);
        for (File file : files) model.addRow(new Object[]{file.getName(), Category.OTHER, "Reading…"});
        table.setRowHeight(36); table.setAutoCreateRowSorter(true); table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(370);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(Category.values())));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(AppTheme.padding(0, 24, 0, 24)); add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 12)); footer.setBorder(AppTheme.padding(16, 24, 18, 24));
        status.setForeground(AppTheme.MUTED); footer.add(status, BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancel = AppTheme.button("Cancel", false); cancel.addActionListener(e -> dispose());
        ai.addActionListener(e -> sortWithAi());
        upload.addActionListener(e -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            result = new ArrayList<>();
            for (int row = 0; row < model.getRowCount(); row++) result.add((Category)model.getValueAt(row, 1));
            dispose();
        });
        controls.add(ai); controls.add(cancel); controls.add(upload); footer.add(controls, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH); getRootPane().setDefaultButton(upload);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { if (worker != null) worker.cancel(true); }
        });
        inspectDocuments();
    }

    static List<Category> review(JFrame owner, AgentController controller, File[] files, Category folder) {
        UploadReviewDialog dialog = new UploadReviewDialog(owner, controller, files, folder);
        dialog.setVisible(true); return dialog.result;
    }

    private void busy(boolean busy) { table.setEnabled(!busy); upload.setEnabled(!busy); ai.setEnabled(!busy); }

    private void inspectDocuments() {
        busy(true);
        worker = new SwingWorker<List<DocumentClassification.Hint>, Void>() {
            @Override protected List<DocumentClassification.Hint> doInBackground() {
                List<DocumentClassification.Hint> hints = new ArrayList<>();
                for (File file : files) {
                    if (isCancelled()) return List.of();
                    try { hints.add(DocumentClassification.read(file.toPath()).hint()); }
                    catch (Exception ex) { hints.add(new DocumentClassification.Hint(Category.OTHER,
                            "Could not read text — choose a folder manually", false)); }
                }
                return hints;
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    var hints = get(); int uncertain = 0;
                    for (int row = 0; row < hints.size(); row++) {
                        var hint = hints.get(row);
                        Category destination = !hint.clear() && files.length == 1 && selectedFolder != null
                                ? selectedFolder : hint.category();
                        model.setValueAt(destination, row, 1); model.setValueAt(hint.reason(), row, 2);
                        if (!hint.clear()) uncertain++;
                    }
                    status.setText(uncertain == 0 ? "Document checks complete. Review the folders, then upload."
                            : uncertain + " uncertain files. Choose their folders or use AI sort files.");
                } catch (Exception ex) { status.setText("Inspection failed. Choose folders manually or retry AI sorting."); }
                busy(false);
            }
        }; worker.execute();
    }

    private void sortWithAi() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        busy(true); status.setText("AI is checking file purpose; uncertain files receive a full-document review…");
        worker = new SwingWorker<List<AgentController.UploadCategory>, Void>() {
            @Override protected List<AgentController.UploadCategory> doInBackground() throws Exception {
                return controller.suggestUploadCategories(java.util.Arrays.stream(files).map(File::toPath).toList());
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    var suggestions = get();
                    for (int row = 0; row < suggestions.size(); row++) {
                        model.setValueAt(suggestions.get(row).category(), row, 1);
                        model.setValueAt(suggestions.get(row).reason(), row, 2);
                    }
                    status.setText("AI checked all " + suggestions.size() + " files. Review and adjust any folder before uploading.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText("AI sorting failed. Existing folder choices are unchanged.");
                    JOptionPane.showMessageDialog(UploadReviewDialog.this, cause.getMessage(), "AI sorting unavailable", JOptionPane.WARNING_MESSAGE);
                }
                busy(false);
            }
        }; worker.execute();
    }
}
