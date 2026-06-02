package com.nosql2sql.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nosql2sql.model.ColumnDef;
import com.nosql2sql.model.TableSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonAnalyzer {

    private ArrayList<TableSchema> tables = new ArrayList<>();
    private HashMap<String, ArrayList<LinkedHashMap<String, Object>>> tableData = new HashMap<>();
    private HashMap<String, Integer> idCounters = new HashMap<>();

    // Her tablo icin satir cache: tablo_adi -> (satirAnahtari -> id)
    // Ayni veri tekrar gelirse yeni satir eklemeyip mevcut id'yi dondurmek icin
    private HashMap<String, HashMap<String, Integer>> rowCache = new HashMap<>();

    public void analyze(JsonElement root, String rootTableName) {
        if (root.isJsonArray()) {
            processArray(root.getAsJsonArray(), rootTableName, null, -1);
        } else if (root.isJsonObject()) {
            processObject(root.getAsJsonObject(), rootTableName, null, -1);
        }
    }

    private int processObject(JsonObject obj, String tableName, String parentTable, int parentId) {
        TableSchema schema = getOrCreateSchema(tableName);

        if (parentTable != null) {
            schema.addForeignKey(parentTable);
        }

        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        int internalId = nextId(tableName);
        row.put("id", internalId);

        if (parentTable != null) {
            row.put(parentTable + "_id", parentId);
        }

        flattenFields(obj, tableName, schema, row, "", internalId);

        // Satiri cache anahtarina donustur (id ve fk sutunlari haric)
        String cacheKey = buildCacheKey(row, parentTable);

        if (!rowCache.containsKey(tableName)) {
            rowCache.put(tableName, new HashMap<>());
        }

        HashMap<String, Integer> tableCache = rowCache.get(tableName);

        if (tableCache.containsKey(cacheKey)) {
            // Ayni veri zaten var - sayaci geri al, mevcut id'yi don
            idCounters.put(tableName, idCounters.get(tableName) - 1);
            return tableCache.get(cacheKey);
        }

        tableCache.put(cacheKey, internalId);
        addRow(tableName, row);
        return internalId;
    }

    // Satirin benzersiz anahtarini olusturur (id ve fk sutunlari haric)
    private String buildCacheKey(LinkedHashMap<String, Object> row, String parentTable) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String col = entry.getKey();
            if (col.equals("id")) continue;
            if (parentTable != null && col.equals(parentTable + "_id")) continue;
            sb.append(col).append("=").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }

    private void flattenFields(JsonObject obj, String tableName,
                               TableSchema schema, LinkedHashMap<String, Object> row,
                               String prefix, int internalId) {

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            String columnName = prefix.isEmpty() ? key : prefix + "_" + key;

            if (columnName.equals("id")) {
                columnName = "json_id";
                if (schema.hasColumn(columnName)) {
                    columnName = resolveColumnName(columnName, schema);
                }
            }

            if (value.isJsonNull()) {
                schema.addColumn(new ColumnDef(columnName, "TEXT"));
                row.put(columnName, null);

            } else if (value.isJsonPrimitive()) {
                String type = inferType(value.getAsJsonPrimitive());
                schema.addColumn(new ColumnDef(columnName, type));
                row.put(columnName, getValue(value.getAsJsonPrimitive()));

            } else if (value.isJsonObject()) {
                flattenFields(value.getAsJsonObject(), tableName, schema, row, columnName, internalId);

            } else if (value.isJsonArray()) {
                String childTableName = tableName + "_" + key;
                processArray(value.getAsJsonArray(), childTableName, tableName, internalId);
            }
        }
    }

    private void processArray(JsonArray array, String childTableName, String parentTable, int parentId) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);

            if (element.isJsonObject()) {
                processObject(element.getAsJsonObject(), childTableName, parentTable, parentId);

            } else if (element.isJsonPrimitive() || element.isJsonNull()) {
                TableSchema schema = getOrCreateSchema(childTableName);
                if (parentTable != null) schema.addForeignKey(parentTable);
                schema.addColumn(new ColumnDef("value", "TEXT"));

                String val = element.isJsonNull() ? null : element.getAsString();
                String cacheKey = "value=" + val + ";";

                if (!rowCache.containsKey(childTableName)) {
                    rowCache.put(childTableName, new HashMap<>());
                }

                if (!rowCache.get(childTableName).containsKey(cacheKey)) {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    int newId = nextId(childTableName);
                    row.put("id", newId);
                    if (parentTable != null) row.put(parentTable + "_id", parentId);
                    row.put("value", val);
                    addRow(childTableName, row);
                    rowCache.get(childTableName).put(cacheKey, newId);
                }

            } else if (element.isJsonArray()) {
                int newId = nextId(childTableName);
                processArray(element.getAsJsonArray(), childTableName + "_item", childTableName, newId);
            }
        }
    }

    private String resolveColumnName(String name, TableSchema schema) {
        if (!schema.hasColumn(name)) {
            return name;
        }
        int i = 1;
        while (schema.hasColumn(name + "_" + i)) {
            i++;
        }
        return name + "_" + i;
    }

    private String inferType(JsonPrimitive p) {
        if (p.isBoolean()) return "INTEGER";
        if (p.isNumber()) {
            if (p.getAsString().contains(".")) return "REAL";
            return "INTEGER";
        }
        return "TEXT";
    }

    private Object getValue(JsonPrimitive p) {
        if (p.isBoolean()) return p.getAsBoolean() ? 1 : 0;
        if (p.isNumber()) {
            if (p.getAsString().contains(".")) return p.getAsDouble();
            return p.getAsLong();
        }
        return p.getAsString();
    }

    private TableSchema getOrCreateSchema(String name) {
        for (TableSchema t : tables) {
            if (t.getTableName().equals(name)) return t;
        }
        TableSchema newSchema = new TableSchema(name);
        tables.add(newSchema);
        return newSchema;
    }

    private int nextId(String tableName) {
        int current = 0;
        if (idCounters.containsKey(tableName)) {
            current = idCounters.get(tableName);
        }
        int next = current + 1;
        idCounters.put(tableName, next);
        return next;
    }

    private void addRow(String tableName, LinkedHashMap<String, Object> row) {
        if (!tableData.containsKey(tableName)) {
            tableData.put(tableName, new ArrayList<>());
        }
        tableData.get(tableName).add(row);
    }

    public ArrayList<TableSchema> getTables() { return tables; }

    public HashMap<String, ArrayList<LinkedHashMap<String, Object>>> getTableData() { return tableData; }

    public void reset() {
        tables.clear();
        tableData.clear();
        idCounters.clear();
        rowCache.clear();
    }
}
