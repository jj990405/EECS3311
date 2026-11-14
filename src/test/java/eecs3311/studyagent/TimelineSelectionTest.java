package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class TimelineSelectionTest {
    @Test void rightClickPreservesGroupAndUsesSortedModelIds() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(new Object[][]{{"Z"},{"A"},{"M"}},new String[]{"Title"}));
            var ids = List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
            table.setAutoCreateRowSorter(true);table.getRowSorter().toggleSortOrder(0);
            table.setRowSelectionInterval(0,1);
            TimelineSelection.selectPopupRow(table,1);
            assertEquals(List.of(ids.get(1),ids.get(2)),TimelineSelection.ids(table,ids));
            List<UUID> snapshot=TimelineSelection.ids(table,ids);
            table.clearSelection();assertEquals(2,snapshot.size());
            TimelineSelection.selectPopupRow(table,2);
            assertEquals(List.of(ids.get(0)),TimelineSelection.ids(table,ids));
            TimelineSelection.selectPopupRow(table,-1);
            assertTrue(TimelineSelection.ids(table,ids).isEmpty());
        });
    }
}
