package com.nosql2sql.ui;

import com.nosql2sql.engine.DatabaseEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class SqlTablesPanel extends JPanel {

    private JTabbedPane tabs;

    public SqlTablesPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("SQL Tabloları"));
        tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);
    }

    public void display(DatabaseEngine engine) throws SQLException {
        tabs.removeAll();
        ArrayList<String> tableNames = engine.getTableNames();

        for (String tableName : tableNames) {
            String[][] data = engine.getTableRows(tableName);

            if (data.length == 0) continue;

            String[] headers = data[0];
            int rowCount = data.length - 1;
            String[][] rows = new String[rowCount][headers.length];

            for (int j = 0; j < rowCount; j++) {
                rows[j] = data[j + 1];
            }

            DefaultTableModel model = new DefaultTableModel(rows, headers) {
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

            JTable table = new JTable(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.getTableHeader().setReorderingAllowed(false);

            for (int k = 0; k < table.getColumnCount(); k++) {
                int width = 80;
                for (int r = 0; r < table.getRowCount(); r++) {
                    Object value = table.getValueAt(r, k);
                    if (value != null) {
                        int textWidth = table.getFontMetrics(table.getFont())
                                .stringWidth(value.toString()) + 24;
                        if (textWidth > width) width = textWidth;
                    }
                }
                table.getColumnModel().getColumn(k).setPreferredWidth(width);
            }

            JScrollPane scroll = new JScrollPane(table);
            String tabTitle = tableName + "  (" + rowCount + " satır)";
            tabs.addTab(tabTitle, scroll);
        }

        if (tabs.getTabCount() == 0) {
            tabs.addTab("—", new JLabel("Tablo bulunamadı.", SwingConstants.CENTER));
        }
    }

    public void clear() {
        tabs.removeAll();
    }
}
