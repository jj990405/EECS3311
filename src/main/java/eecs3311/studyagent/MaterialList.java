package eecs3311.studyagent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;

/** Real selectable file rows; context actions capture the clicked material. */
final class MaterialList extends JList<MaterialFile> {
    MaterialList(Consumer<MaterialFile> selected, Consumer<MaterialFile> open,
                 Function<MaterialFile, JPopupMenu> menu) {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setBackground(AppTheme.SURFACE); setForeground(AppTheme.TEXT);
        setSelectionBackground(AppTheme.BLUE_TINT); setSelectionForeground(AppTheme.TEXT);
        setFixedCellHeight(42);
        setFont(AppTheme.BODY);
        setToolTipText("Select a file; double-click to open. Right-click for notes and file/slide quizzes.");
        setCellRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selection, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selection, focus);
                MaterialFile file = (MaterialFile) value;
                setFont(AppTheme.BODY);
                setText(file.originalName()); setToolTipText(file.originalName()
                        + (file.textAvailable() ? " · text ready" : " · no extracted text"));
                setBorder(AppTheme.padding(10, 12, 10, 12)); return this;
            }
        });
        addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && getSelectedValue() != null) selected.accept(getSelectedValue()); });
        addMouseListener(new MouseAdapter() {
            private void popup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = locationToIndex(e.getPoint());
                if (row < 0 || !getCellBounds(row, row).contains(e.getPoint())) return;
                setSelectedIndex(row);
                menu.apply(getSelectedValue()).show(MaterialList.this, e.getX(), e.getY());
            }
            @Override public void mousePressed(MouseEvent e) { popup(e); }
            @Override public void mouseReleased(MouseEvent e) { popup(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && getSelectedValue() != null)
                    open.accept(getSelectedValue());
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openFile");
        getActionMap().put("openFile", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (getSelectedValue() != null) open.accept(getSelectedValue());
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("shift F10"), "fileMenu");
        getActionMap().put("fileMenu", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (getSelectedValue() != null) menu.apply(getSelectedValue()).show(MaterialList.this, 12,
                        getCellBounds(getSelectedIndex(), getSelectedIndex()).y + getFixedCellHeight());
            }
        });
    }
}
