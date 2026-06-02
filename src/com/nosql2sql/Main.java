package com.nosql2sql;

import com.nosql2sql.ui.MainWindow;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {
    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                final String detay = "Thread: " + t.getName() + "\n\n" + sw.toString();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JTextArea textArea = new JTextArea(detay);
                        textArea.setEditable(false);
                        textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
                        JScrollPane scroll = new JScrollPane(textArea);
                        scroll.setPreferredSize(new java.awt.Dimension(650, 300));
                        JOptionPane.showMessageDialog(null, scroll,
                                "Beklenmeyen Hata", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}
                new MainWindow().setVisible(true);
            }
        });
    }
}
