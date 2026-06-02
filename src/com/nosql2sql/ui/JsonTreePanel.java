package com.nosql2sql.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Map;

public class JsonTreePanel extends JPanel {

    private JTree tree;
    private DefaultTreeModel treeModel;

    public JsonTreePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("JSON Yapısı"));

        DefaultMutableTreeNode empty = new DefaultMutableTreeNode("(henüz dosya yüklenmedi)");
        treeModel = new DefaultTreeModel(empty);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void display(String jsonText) {
        try {
            JsonElement root = JsonParser.parseString(jsonText);
            DefaultMutableTreeNode rootNode = buildNode("kök", root);
            treeModel.setRoot(rootNode);
            expandAll();
        } catch (JsonSyntaxException e) {
            DefaultMutableTreeNode error = new DefaultMutableTreeNode("HATA: Geçersiz JSON formatı");
            treeModel.setRoot(error);
        }
    }

    public void clear() {
        DefaultMutableTreeNode empty = new DefaultMutableTreeNode("(henüz dosya yüklenmedi)");
        treeModel.setRoot(empty);
    }

    private DefaultMutableTreeNode buildNode(String label, JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("{ " + label + " }");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                node.add(buildNode(entry.getKey(), entry.getValue()));
            }
            return node;

        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                    "[ " + label + " ]  —  " + array.size() + " öğe");
            for (int i = 0; i < array.size(); i++) {
                node.add(buildNode("[" + i + "]", array.get(i)));
            }
            return node;

        } else if (element.isJsonNull()) {
            return new DefaultMutableTreeNode(label + " : null");

        } else {
            return new DefaultMutableTreeNode(label + " : " + element.getAsString());
        }
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }
}
