package eecs3311.studyagent;

import java.awt.Color;
import java.awt.Component;
import java.util.function.IntFunction;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/** Neutral course shades; resolve model rows so sorting keeps shades consistent. */
final class CourseRowRenderer extends DefaultTableCellRenderer {
    private final IntFunction<Course> courseAtModelRow;

    CourseRowRenderer(IntFunction<Course> courseAtModelRow) { this.courseAtModelRow = courseAtModelRow; }

    @Override public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        Course course = courseAtModelRow.apply(table.convertRowIndexToModel(row));
        int shade = course == null ? 42 : 38 + Math.floorMod(course.name().hashCode(), 6) * 8;
        int background = shade + (selected ? 18 : 0);
        setBackground(new Color(background, background, background));
        setForeground(AppTheme.TEXT);
        Color edge = new Color(shade + 30, shade + 30, shade + 30);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, column == 0 ? 3 : 0, 1, 0,
                        column == 0 ? edge : AppTheme.BORDER), AppTheme.padding(4, 10, 4, 10)));
        return this;
    }
}
