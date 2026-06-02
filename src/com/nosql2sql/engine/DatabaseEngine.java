package com.nosql2sql.engine;

import com.nosql2sql.model.ColumnDef;
import com.nosql2sql.model.TableSchema;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DatabaseEngine {

    private static final String DB_PATH = "nosql2sql.db";
    private Connection connection;

    public void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC surucu bulunamadi: " + e.getMessage());
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        Statement st = connection.createStatement();
        st.execute("PRAGMA foreign_keys = ON");
        st.close();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void applyConversion(ArrayList<TableSchema> tables,
                                java.util.HashMap<String, ArrayList<LinkedHashMap<String, Object>>> tableData)
            throws SQLException {

        connection.setAutoCommit(false);

        try {
            for (int i = 0; i < tables.size(); i++) {
                createTable(tables.get(i));
            }

            for (int i = 0; i < tables.size(); i++) {
                TableSchema schema = tables.get(i);
                ArrayList<LinkedHashMap<String, Object>> rows = tableData.get(schema.getTableName());
                if (rows != null) {
                    for (int j = 0; j < rows.size(); j++) {
                        insertRow(schema, rows.get(j));
                    }
                }
            }

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void createTable(TableSchema schema) throws SQLException {
        String sql = schema.buildCreateTableSql();
        Statement st = connection.createStatement();
        st.execute(sql);
        st.close();
    }

    private void insertRow(TableSchema schema, LinkedHashMap<String, Object> row) throws SQLException {
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();

        for (ColumnDef col : schema.getColumns()) {
            if (col.isPrimaryKey()) continue;
            String name = col.getColumnName();
            if (row.containsKey(name)) {
                columns.add("\"" + name + "\"");
                values.add(row.get(name));
            }
        }

        if (columns.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO \"").append(schema.getTableName()).append("\" (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i));
            if (i < columns.size() - 1) sb.append(", ");
        }
        sb.append(") VALUES (");
        for (int i = 0; i < values.size(); i++) {
            sb.append("?");
            if (i < values.size() - 1) sb.append(", ");
        }
        sb.append(")");

        PreparedStatement ps = connection.prepareStatement(sb.toString());
        for (int i = 0; i < values.size(); i++) {
            ps.setObject(i + 1, values.get(i));
        }
        ps.executeUpdate();
        ps.close();
    }

    public void dropAllTables() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("PRAGMA foreign_keys = OFF");
        st.close();

        ArrayList<String> names = getTableNames();
        for (int i = 0; i < names.size(); i++) {
            Statement drop = connection.createStatement();
            drop.execute("DROP TABLE IF EXISTS \"" + names.get(i) + "\"");
            drop.close();
        }

        Statement st2 = connection.createStatement();
        st2.execute("PRAGMA foreign_keys = ON");
        st2.close();
    }

    public ArrayList<String> getTableNames() throws SQLException {
        ArrayList<String> names = new ArrayList<>();
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
        while (rs.next()) {
            names.add(rs.getString("name"));
        }
        rs.close();
        st.close();
        return names;
    }

    public String[][] getTableRows(String tableName) throws SQLException {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM \"" + tableName + "\"");

        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        String[] headers = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            headers[i - 1] = meta.getColumnName(i);
        }

        ArrayList<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                Object val = rs.getObject(i);
                row[i - 1] = (val == null) ? "NULL" : val.toString();
            }
            rows.add(row);
        }
        rs.close();
        st.close();

        String[][] result = new String[rows.size() + 1][columnCount];
        result[0] = headers;
        for (int i = 0; i < rows.size(); i++) {
            result[i + 1] = rows.get(i);
        }
        return result;
    }
}
