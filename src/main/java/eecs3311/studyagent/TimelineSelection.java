package eecs3311.studyagent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.swing.JTable;

/** Resolves visible selection before a store refresh changes the table's rows. */
final class TimelineSelection {
    private TimelineSelection() { }
    static void selectPopupRow(JTable table, int row) {
        if (row < 0) table.clearSelection();
        else if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
    }
    static List<UUID> ids(JTable table, List<UUID> modelIds) {
        return Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel)
                .filter(row -> row >= 0 && row < modelIds.size()).mapToObj(modelIds::get).toList();
    }
}
