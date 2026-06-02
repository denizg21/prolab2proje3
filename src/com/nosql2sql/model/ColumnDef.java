package com.nosql2sql.model;

public class ColumnDef {

    private String columnName;
    private String sqlType;
    private boolean primaryKey;
    private boolean foreignKey;
    private String referencedTable;

    public ColumnDef(String columnName, String sqlType) {
        this.columnName = columnName;
        this.sqlType = sqlType;
        this.primaryKey = false;
        this.foreignKey = false;
    }

    public String getColumnName() { return columnName; }
    public String getSqlType() { return sqlType; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public boolean isForeignKey() { return foreignKey; }
    public void setForeignKey(boolean foreignKey) { this.foreignKey = foreignKey; }

    public String getReferencedTable() { return referencedTable; }
    public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }

    public String toSqlPart() {
        String part = "\"" + columnName + "\" " + sqlType;
        if (primaryKey) {
            part += " PRIMARY KEY AUTOINCREMENT";
        }
        return part;
    }

    @Override
    public String toString() {
        String text = columnName + " [" + sqlType + "]";
        if (primaryKey) text += " (PK)";
        if (foreignKey)  text += " (FK -> " + referencedTable + ")";
        return text;
    }
}
