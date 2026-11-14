package eecs3311.studyagent;

import java.awt.Color;
import java.awt.Component;
import java.util.function.IntFunction;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/** A thin course marker beside the course name; row backgrounds stay neutral. */
final class CourseRowRenderer extends DefaultTableCellRenderer {
    private static final Color[] MARKERS = {
        new Color(0x94B2D1), new Color(0xB2A1C8), new Color(0xA5ADCC), new Color(0xC8B68B)
    };
    private final IntFunction<Course> courseAtModelRow;

    CourseRowRenderer(IntFunction<Course> courseAtModelRow) { this.courseAtModelRow = courseAtModelRow; }

    @Override public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        Course course = courseAtModelRow.apply(table.convertRowIndexToModel(row));
        setBackground(selected ? AppTheme.RAISED : table.getBackground());
        setForeground(AppTheme.TEXT);
        boolean courseColumn = table.convertColumnIndexToModel(column) == 1 && course != null;
        var marker = courseColumn
                ? javax.swing.BorderFactory.createMatteBorder(0, 3, 0, 0,
                        MARKERS[Math.floorMod(course.name().hashCode(), MARKERS.length)])
                : javax.swing.BorderFactory.createEmptyBorder(0, 3, 0, 0);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                marker, javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                        AppTheme.padding(4, 10, 4, 10))));
        return this;
    }
}
