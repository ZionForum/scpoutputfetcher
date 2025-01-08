package com.outputfetcher;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JTextArea;

public class TabInfo {
    JTextArea textArea;
    String host;
    String user;
    String password;
    String logFile;
    String tabName;
    long lastModified;
    AtomicBoolean isConnected;
    String lastContent;
    Thread monitoringThread;
    JLabel statusLabel;
    JLabel lastUpdateLabel;
    Color textColor;
    Color backgroundColor;
    Font currentFont;
    public StringBuilder rawLogContent;
    public boolean filterDuplicates = true;  // Default to filtering duplicates

    TabInfo() {
        textArea = new JTextArea();
        lastModified = 0;
        isConnected = new AtomicBoolean(false);
        lastContent = "";
        textColor = Color.BLACK;
        backgroundColor = new Color(250, 250, 250);
        currentFont = new Font("Monospaced", Font.PLAIN, 12);
        tabName = "";
    }
}