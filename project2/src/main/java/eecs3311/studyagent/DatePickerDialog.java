package eecs3311.studyagent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CancellationException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Editable date field with a compact, mouse-operated month calendar. */
final class DatePickerDialog {
    private static final String[] WEEKDAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");

    private DatePickerDialog() { }

    static LocalDate show(JFrame owner, String label, String initial, boolean optional) {
        JDialog dialog = new JDialog(owner, label, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel title = new JLabel(label);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        body.add(title, BorderLayout.NORTH);

        JTextField field = new JTextField(initial, 15);
        field.setToolTipText("Type 20261231 or 2026-12-31");
        JButton calendar = new JButton(new CalendarIcon());
        calendar.setToolTipText("Choose a date from the calendar");
        calendar.getAccessibleContext().setAccessibleName("Open calendar");
        JPanel entry = new JPanel(new BorderLayout(6, 0));
        entry.add(field, BorderLayout.CENTER);
        entry.add(calendar, BorderLayout.EAST);
        JPanel middle = new JPanel(new BorderLayout(0, 5));
        middle.add(entry, BorderLayout.NORTH);
        middle.add(new JLabel(optional ? "Type a date or leave blank for TBD" : "Type a date or use the calendar"),
                BorderLayout.CENTER);
        JLabel error = new JLabel(" ");
        error.setForeground(new Color(170, 35, 35));
        middle.add(error, BorderLayout.SOUTH);
        body.add(middle, BorderLayout.CENTER);

        LocalDate[] result = new LocalDate[1];
        boolean[] accepted = {false};
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> dialog.dispose());
        JButton save = new JButton("OK");
        save.addActionListener(event -> {
            String value = field.getText().trim();
            if (optional && value.isEmpty()) {
                accepted[0] = true;
                dialog.dispose();
                return;
            }
            try {
                result[0] = FlexibleDate.parse(value);
                accepted[0] = true;
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                field.requestFocusInWindow();
                field.selectAll();
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(cancel);
        actions.add(save);
        body.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(save);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { dialog.dispose(); }
        });
        calendar.addActionListener(event -> showCalendar(calendar, field));
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(field::requestFocusInWindow);
        dialog.setVisible(true);
        if (!accepted[0]) throw new CancellationException();
        return result[0];
    }

    private static void showCalendar(JButton anchor, JTextField field) {
        LocalDate initial;
        try { initial = FlexibleDate.parse(field.getText()); }
        catch (IllegalArgumentException ex) { initial = LocalDate.now(); }
        YearMonth[] visible = {YearMonth.from(initial)};
        JPopupMenu popup = new JPopupMenu();
        JPanel month = new JPanel(new BorderLayout(0, 8));
        month.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel heading = new JLabel("", JLabel.CENTER);
        JButton previous = new JButton("‹");
        previous.setToolTipText("Previous month");
        JButton next = new JButton("›");
        next.setToolTipText("Next month");
        JPanel navigation = new JPanel(new BorderLayout(4, 0));
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(heading, BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST);
        month.add(navigation, BorderLayout.NORTH);
        JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
        month.add(days, BorderLayout.CENTER);
        Runnable render = () -> {
            heading.setText(visible[0].format(MONTH));
            days.removeAll();
            for (String weekday : WEEKDAYS) {
                JLabel cell = new JLabel(weekday, JLabel.CENTER);
                cell.setFont(cell.getFont().deriveFont(Font.BOLD, 11f));
                days.add(cell);
            }
            int offset = visible[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) days.add(new JLabel());
            for (int day = 1; day <= visible[0].lengthOfMonth(); day++) {
                LocalDate selected = visible[0].atDay(day);
                JButton button = new JButton(Integer.toString(day));
                button.setMargin(new Insets(4, 4, 4, 4));
                button.setToolTipText(selected.toString());
                button.getAccessibleContext().setAccessibleName(selected.toString());
                if (selected.equals(LocalDate.now())) button.setFont(button.getFont().deriveFont(Font.BOLD));
                button.addActionListener(event -> {
                    field.setText(selected.toString());
                    popup.setVisible(false);
                    field.requestFocusInWindow();
                });
                days.add(button);
            }
            days.revalidate();
            days.repaint();
            popup.pack();
        };
        previous.addActionListener(event -> { visible[0] = visible[0].minusMonths(1); render.run(); });
        next.addActionListener(event -> { visible[0] = visible[0].plusMonths(1); render.run(); });
        popup.add(month);
        render.run();
        popup.show(anchor, 0, anchor.getHeight());
    }

    private static final class CalendarIcon implements javax.swing.Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            graphics.setColor(component.getForeground());
            graphics.drawRoundRect(x + 1, y + 3, 16, 14, 2, 2);
            graphics.drawLine(x + 1, y + 7, x + 17, y + 7);
            graphics.drawLine(x + 5, y + 1, x + 5, y + 5);
            graphics.drawLine(x + 13, y + 1, x + 13, y + 5);
            graphics.fillRect(x + 5, y + 10, 2, 2);
            graphics.fillRect(x + 10, y + 10, 2, 2);
        }
    }
}
