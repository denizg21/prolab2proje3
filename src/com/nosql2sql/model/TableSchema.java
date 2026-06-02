package com.nosql2sql.model;

import java.util.ArrayList;

public class TableSchema {

    private String tableName;
    private ArrayList<ColumnDef> columns;
    private String parentTableName;

    public TableSchema(String tableName) {
        this.tableName = tableName;
        this.columns = new ArrayList<>();
        this.parentTableName = null;

        ColumnDef pk = new ColumnDef("id", "INTEGER");
        pk.setPrimaryKey(true);
        columns.add(pk);
    }

    public void addColumn(ColumnDef col) {
        for (ColumnDef existing : columns) {
            if (existing.getColumnName().equals(col.getColumnName())) {
                return;
            }
        }
        columns.add(col);
    }

    public void addForeignKey(String parentTable) {
        this.parentTableName = parentTable;
        String fkColumnName = parentTable + "_id";

        for (ColumnDef col : columns) {
            if (col.getColumnName().equals(fkColumnName)) {
                return;
            }
        }

        ColumnDef fk = new ColumnDef(fkColumnName, "INTEGER");
        fk.setForeignKey(true);
        fk.setReferencedTable(parentTable);
        columns.add(fk);
    }

    public boolean hasColumn(String name) {
        for (ColumnDef col : columns) {
            if (col.getColumnName().equals(name)) return true;
        }
        return false;
    }

    public String getTableName() { return tableName; }
    public ArrayList<ColumnDef> getColumns() { return columns; }
    public String getParentTableName() { return parentTableName; }

    public String buildCreateTableSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS \"").append(tableName).append("\" (\n");

        ArrayList<String> parts = new ArrayList<>();

        for (ColumnDef col : columns) {
            parts.add("  " + col.toSqlPart());
        }

        for (ColumnDef col : columns) {
            if (col.isForeignKey()) {
                String fkLine = "  FOREIGN KEY (\"" + col.getColumnName() + "\") REFERENCES \""
                        + col.getReferencedTable() + "\"(\"id\")";
                parts.add(fkLine);
            }
        }

        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return tableName + " -> " + columns.toString();
    }
}
