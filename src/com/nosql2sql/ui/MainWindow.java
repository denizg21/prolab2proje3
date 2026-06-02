package com.nosql2sql.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nosql2sql.engine.DatabaseEngine;
import com.nosql2sql.model.TableSchema;
import com.nosql2sql.parser.JsonAnalyzer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MainWindow extends JFrame {

    private JsonTreePanel jsonPanel;
    private SqlTablesPanel sqlPanel;
    private JLabel statusLabel;

    private JButton convertBtn;
    private JButton resetBtn;

    private JsonElement loadedJson;
    private String fileName;

    private DatabaseEngine engine;

    public MainWindow() {
        super("NoSQL → SQL Dönüşüm Sistemi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        engine = new DatabaseEngine();
        try {
            engine.connect();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Veritabanına bağlanılamadı: " + e.getMessage(),
                    "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
        }

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);

        jsonPanel = new JsonTreePanel();
        sqlPanel  = new SqlTablesPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsonPanel, sqlPanel);
        splitPane.setDividerLocation(420);
        splitPane.setDividerSize(6);
        add(splitPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Hazır. Bir JSON dosyası yükleyin.");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        panel.setBorder(BorderFactory.createEtchedBorder());

        JButton loadBtn = new JButton("JSON Yükle");
        convertBtn = new JButton("Dönüştür");
        resetBtn   = new JButton("Sıfırla");

        convertBtn.setEnabled(false);
        resetBtn.setEnabled(false);

        loadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadJson();
            }
        });

        convertBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                convert();
            }
        });

        resetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        panel.add(loadBtn);
        panel.add(convertBtn);
        panel.add(resetBtn);

        return panel;
    }

    private void loadJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("JSON Dosyası Seç");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Dosyaları (*.json)", "json"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            loadedJson = JsonParser.parseString(content);
            fileName = file.getName().replace(".json", "").replaceAll("[^a-zA-Z0-9]", "_");

            jsonPanel.display(content);
            convertBtn.setEnabled(true);
            setStatus("Yüklendi: " + file.getName());

        } catch (IOException ex) {
            showError("Dosya okunamadı:\n" + ex.getMessage());
        } catch (JsonSyntaxException ex) {
            showError("Geçersiz JSON:\n" + ex.getMessage());
        }
    }

    private void convert() {
        if (loadedJson == null) return;

        if (!engine.isConnected()) {
            showError("Veritabanı bağlantısı yok.\nUygulamayı yeniden başlatın.");
            return;
        }

        try {
            engine.dropAllTables();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Eski tablolar temizlenirken hata:\n" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return;
        }

        setStatus("Dönüştürülüyor...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            ArrayList<TableSchema> foundTables;
            java.util.HashMap<String, ArrayList<LinkedHashMap<String, Object>>> tableData;

            protected Void doInBackground() throws Exception {
                JsonAnalyzer analyzer = new JsonAnalyzer();
                analyzer.analyze(loadedJson, fileName);

                foundTables = analyzer.getTables();
                tableData   = analyzer.getTableData();

                engine.applyConversion(foundTables, tableData);
                return null;
            }

            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    sqlPanel.display(engine);
                    resetBtn.setEnabled(true);

                    int totalTables = foundTables.size();
                    int totalRows = 0;
                    for (ArrayList<LinkedHashMap<String, Object>> rows : tableData.values()) {
                        totalRows += rows.size();
                    }
                    setStatus(totalTables + " tablo ve " + totalRows + " satır başarıyla oluşturuldu.");

                } catch (Throwable ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    cause.printStackTrace();
                    showError("Dönüştürme sırasında hata:\n"
                            + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    setStatus("Hata oluştu.");
                }
            }
        };

        worker.execute();
    }

    private void reset() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Tüm tablolar silinecek. Devam etmek istiyor musunuz?",
                "Sıfırla", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (answer != JOptionPane.YES_OPTION) return;

        try {
            engine.dropAllTables();
            sqlPanel.clear();
            jsonPanel.clear();
            loadedJson = null;
            convertBtn.setEnabled(false);
            resetBtn.setEnabled(false);
            setStatus("Sistem sıfırlandı. Yeni bir JSON dosyası yükleyin.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Sıfırlanırken hata:\n" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void setStatus(String message) { statusLabel.setText(message); }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
    }
}
