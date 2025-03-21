package com.outputfetcher;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextArea;

/**
 * Represents a tab in the log viewer application containing connection details,
 * display settings and content state.
 *
 * Each TabInfo instance maintains:
 * - SSH connection details (host, port, credentials)
 * - UI components (text area, status labels)
 * - Content tracking (raw log, seen entries)
 * - Display preferences (colors, font, filters)
 * - Connection state
 */
public class TabInfo {
    public String host;
    public int port;
    public String user;
    public String password;
    public String logFile;
    public String tabName;
    public JTextArea textArea = new JTextArea();
    public JLabel statusLabel;
    public JLabel lastUpdateLabel;
    public Thread monitoringThread;
    public long lastModified = 0;
    public StringBuilder rawLogContent;
    public Set<String> seenLogContents = new HashSet<>();
    public boolean filterDuplicates = false;
    public boolean wordWrap = false;
    public boolean showTimestamp = true;
    public AtomicBoolean isConnected = new AtomicBoolean(false);
    public Color textColor = Color.BLACK;
    public Color backgroundColor = Color.WHITE;
    public Font currentFont = new Font("Monospaced", Font.PLAIN, 12);
    public boolean autoConnect = false;

    /**
     * Creates a new TabInfo instance with default settings.
     * Initializes UI components and sets default values for connection and display properties.
     */
    TabInfo() {
        textArea = new JTextArea();
        lastModified = 0;
        isConnected = new AtomicBoolean(false);
        textColor = Color.BLACK;
        backgroundColor = new Color(250, 250, 250);
        currentFont = new Font("Monospaced", Font.PLAIN, 12);
        tabName = "";
        port = 22;
        autoConnect = false;
    }
}