package eecs3311.studyagent;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** A small, shared visual vocabulary for the desktop workspace. */
final class AppTheme {
    static final Color BACKGROUND = new Color(0x181818);
    static final Color SURFACE = new Color(0x242424);
    static final Color RAISED = new Color(0x2D2D2D);
    static final Color TEXT = new Color(0xECECEC);
    static final Color MUTED = new Color(0xA6A6A6);
    static final Color BORDER = new Color(0x3A3A3A);
    static final Color BLUE = new Color(0x4789DF);
    static final Color BLUE_TINT = new Color(0x263B57);
    static final Color GREEN = new Color(0x61BE8C);
    static final Color RED = new Color(0xF08585);
    static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font SMALL = BODY.deriveFont(12f);
    static final Font SECTION = BODY.deriveFont(Font.BOLD, 16f);
    static final Font TITLE = BODY.deriveFont(Font.BOLD, 22f);

    private AppTheme() { }

    static void install() {
        for (String key : new String[] {"Label.font", "Button.font", "Tree.font", "Table.font",
                "TableHeader.font", "TabbedPane.font", "Menu.font", "MenuItem.font",
                "TextField.font", "TextArea.font", "ComboBox.font", "OptionPane.font",
                "RadioButton.font", "CheckBox.font"}) UIManager.put(key, BODY);
        UIManager.put("OptionPane.messageForeground", TEXT);
        for (String key : new String[] {"Panel.background", "OptionPane.background",
                "ScrollPane.background", "Viewport.background", "Tree.background", "Table.background",
                "TabbedPane.background", "TextField.background", "TextArea.background",
                "ComboBox.background", "MenuBar.background", "Menu.background", "MenuItem.background",
                "PopupMenu.background"}) UIManager.put(key, SURFACE);
        for (String key : new String[] {"Label.foreground", "Tree.foreground", "Table.foreground",
                "TabbedPane.foreground", "TextField.foreground", "TextArea.foreground",
                "ComboBox.foreground", "Menu.foreground", "MenuItem.foreground"}) UIManager.put(key, TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", RAISED);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("Table.selectionBackground", BLUE_TINT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Tree.selectionBackground", BLUE_TINT);
        UIManager.put("Tree.selectionForeground", TEXT);
    }

    static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    static JButton button(String label, boolean primary) {
        JButton button = new JButton(label);
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setBackground(primary ? BLUE : RAISED);
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? BLUE : BORDER, 1, true), padding(8, 14, 8, 14)));
        return button;
    }

    static void surface(JComponent component) {
        component.setBackground(SURFACE);
        component.setForeground(TEXT);
    }
}
