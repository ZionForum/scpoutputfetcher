package com.outputfetcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.json.JSONException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import javax.swing.JFileChooser;
public class SCPLogViewer {
    private static JTabbedPane tabbedPane;
    private static List<TabInfo> tabs = new ArrayList<>();
    private static JTextField searchField;
    private static JCheckBox caseSensitiveBox;
    private static JCheckBox wrapAroundCheckBox;
    private static final int MAX_TABS = 100;
    private static JTextField hostField, userField, logFileField;
    private static JPasswordField passwordField;

    private static final Color DARK_BG = new Color(43, 43, 43);
    private static final Color DARK_TEXT = new Color(187, 187, 187);
    private static final Color DARK_BUTTON_BG = new Color(240, 240, 240); // Dirty white
    private static final Color DARK_BUTTON_TEXT = new Color(30, 30, 30); // Near black
    private static final Color LIGHT_BUTTON_BG = new Color(240, 240, 240);
    private static final Color LIGHT_BUTTON_TEXT = Color.BLACK;
    private static boolean isDarkMode = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private static void createAndShowGUI() throws JSONException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        isDarkMode = Configuration.isDarkMode();

        JFrame frame = new JFrame("SCP Log Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(1200, 800));

        tabbedPane = new JTabbedPane();

        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(tabbedPane, BorderLayout.CENTER);

        frame.add(tabPanel);

        JToolBar toolBar = createToolBar();
        frame.add(toolBar, BorderLayout.NORTH);

        // Load saved configurations
        List<Configuration.TabConfig> savedConfigs = Configuration.loadTabConfigs();
        if (savedConfigs.isEmpty()) {
            addNewTab();
        } else {
            for (Configuration.TabConfig config : savedConfigs) {
                addNewTab(config);
            }
        }

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        applyTheme(frame.getContentPane(), isDarkMode);

        setupHotkeys(frame);
    }

    private static void addNewTab() throws JSONException {
        addNewTab(new Configuration.TabConfig());
    }

    private static void addNewTab(Configuration.TabConfig config) throws JSONException {
        if (tabs.size() >= MAX_TABS) {
            JOptionPane.showMessageDialog(null, "Maximum number of tabs reached!");
            return;
        }

        TabInfo tab = new TabInfo();
        tab.host = config.host;
        tab.user = config.user;
        tab.logFile = config.logFile;
        tab.textColor = config.textColor;
        tab.backgroundColor = config.backgroundColor;
        tab.currentFont = config.font;

        // Set default tab name
        tab.tabName = "Log " + (tabs.size() + 1);

        // Apply saved settings to the text area
        tab.textArea.setFont(tab.currentFont);
        tab.textArea.setForeground(tab.textColor);
        tab.textArea.setBackground(tab.backgroundColor);

        tabs.add(tab);

        JPanel tabContent = createTabContent(tab);
        int index = tabbedPane.getTabCount();
        tabbedPane.addTab(tab.tabName, tabContent);
        createTabComponent(tab.tabName, index);
        tabbedPane.setSelectedIndex(index);

        saveConfiguration();
    }

    private static void toggleTheme() {
        isDarkMode = !isDarkMode;
        // add a set button theme too for darkmode
        Window window = SwingUtilities.getWindowAncestor(tabbedPane);
        if (window instanceof JFrame) {
            applyTheme(((JFrame) window).getContentPane(), isDarkMode);
            updateButtonStyles();
        }

        try {
            saveConfiguration();
        } catch (JSONException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving theme configuration: " + e.getMessage());
        }
    }

    private static void applyTheme(Container container, boolean dark) {
        Color bg = dark ? DARK_BG : Color.WHITE;
        Color fg = dark ? DARK_TEXT : Color.BLACK;
        Color buttonBg = dark ? DARK_BUTTON_BG : LIGHT_BUTTON_BG;
        Color buttonFg = dark ? DARK_BUTTON_TEXT : LIGHT_BUTTON_TEXT;

        container.setBackground(bg);
        container.setForeground(fg);

        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextArea) {
                comp.setBackground(bg);
                comp.setForeground(fg);
            } else if (comp instanceof JButton) {
                comp.setBackground(buttonBg);
                comp.setForeground(buttonFg);
            } else if (comp instanceof JTabbedPane) {
                comp.setBackground(bg);
                comp.setForeground(fg);
                JTabbedPane tp = (JTabbedPane) comp;
                for (int i = 0; i < tp.getTabCount(); i++) {
                    Component tabComp = tp.getComponentAt(i);
                    if (tabComp instanceof Container) {
                        applyTheme((Container) tabComp, dark);
                    }
                }
            } else if (comp instanceof Container) {
                applyTheme((Container) comp, dark);
            }
        }
    }

    private static void updateButtonStyles() {
        Window window = SwingUtilities.getWindowAncestor(tabbedPane);
        if (window instanceof JFrame) {
            Container contentPane = ((JFrame) window).getContentPane();
            updateButtonStylesInContainer(contentPane);
        }
    }

    private static void updateButtonStylesInContainer(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                styleButton((JButton) comp);
            } else if (comp instanceof Container) {
                updateButtonStylesInContainer((Container) comp);
            }
        }
    }

    private static JPanel createTabContent(TabInfo tabInfo) {
        JPanel tabPanel = new JPanel(new BorderLayout(5, 5));
        tabPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Settings panel
        JPanel settingsPanel = createSettingsPanel(tabInfo);

        // Status panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        tabInfo.statusLabel = new JLabel("Status: Disconnected");
        tabInfo.statusLabel.setForeground(Color.RED);
        tabInfo.lastUpdateLabel = new JLabel("Last Update: Never");
        statusPanel.add(tabInfo.statusLabel);
        statusPanel.add(tabInfo.lastUpdateLabel);

        // Text area setup
        setupTextArea(tabInfo);
        JScrollPane scrollPane = new JScrollPane(tabInfo.textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Button panel
        JPanel buttonPanel = createButtonPanel(tabInfo);

        // Combine panels
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(settingsPanel, BorderLayout.CENTER);
        topPanel.add(statusPanel, BorderLayout.SOUTH);

        tabPanel.add(topPanel, BorderLayout.NORTH);
        tabPanel.add(scrollPane, BorderLayout.CENTER);
        tabPanel.add(buttonPanel, BorderLayout.SOUTH);

        return tabPanel;
    }

    private static void setupTextArea(TabInfo tabInfo) {
        tabInfo.textArea.setEditable(false);
        tabInfo.textArea.setFont(tabInfo.currentFont);
        tabInfo.textArea.setBackground(tabInfo.backgroundColor);
        tabInfo.textArea.setForeground(tabInfo.textColor);

        // Add right-click menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem selectAllItem = new JMenuItem("Select All");

        copyItem.addActionListener(e -> tabInfo.textArea.copy());
        selectAllItem.addActionListener(e -> tabInfo.textArea.selectAll());

        popup.add(copyItem);
        popup.add(selectAllItem);

        tabInfo.textArea.setComponentPopupMenu(popup);
    }

    private static JPanel createSettingsPanel(TabInfo tabInfo) {
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Host field
        hostField = new JTextField("192.168.0.103");
        addSettingsField(settingsPanel, "Host:", hostField, gbc, 0);

        // User field
        userField = new JTextField("root");
        addSettingsField(settingsPanel, "User:", userField, gbc, 1);

        // Password field
        passwordField = new JPasswordField();
        addSettingsField(settingsPanel, "Password:", passwordField, gbc, 2);

        // Log file field
        logFileField = new JTextField("/home/user1/Desktop/output.log");
        addSettingsField(settingsPanel, "Log File:", logFileField, gbc, 3);

        // Connect button
        JButton connectBtn = new JButton("CONNECT");
        styleButton(connectBtn);
        gbc.gridx = 1;
        gbc.gridy = 4;
        settingsPanel.add(connectBtn, gbc);

        // Store references
        tabInfo.host = hostField.getText();
        tabInfo.user = userField.getText();
        tabInfo.password = new String(passwordField.getPassword());
        tabInfo.logFile = logFileField.getText();

        // Add listeners
        connectBtn.addActionListener(e -> {
            saveTabSettings(tabInfo, hostField.getText(),
                    userField.getText(), new String(passwordField.getPassword()), logFileField.getText());
            try {
                saveConfiguration();
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }
        });

        return settingsPanel;
    }

    private static void addSettingsField(JPanel panel, String label, JComponent field,
            GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private static JPanel createButtonPanel(TabInfo tabInfo) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear");

        styleButton(refreshButton);
        styleButton(clearButton);

        refreshButton.addActionListener(e -> refreshLog(tabInfo));
        clearButton.addActionListener(e -> clearLogFromLinux(tabInfo));

        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);

        return buttonPanel;
    }

    private static void showSearchDialog() {
        JDialog searchDialog = new JDialog((Frame) null, "Search", true);
        searchDialog.setLayout(new BorderLayout(5, 5));
        searchDialog.setSize(400, 250);

        // Search Input Panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField(30);
        searchPanel.add(new JLabel("Find:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Options Panel (Match case and Wrap around)
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        caseSensitiveBox = new JCheckBox("Match case");
        wrapAroundCheckBox = new JCheckBox("Wrap around");
        optionsPanel.add(caseSensitiveBox);
        optionsPanel.add(wrapAroundCheckBox);

        // Direction Panel with improved radio button handling
        JPanel directionPanel = new JPanel();
        directionPanel.setBorder(BorderFactory.createTitledBorder("Search Direction"));
        directionPanel.setLayout(new GridLayout(1, 2));

        JRadioButton upButton = new JRadioButton("Up");
        JRadioButton downButton = new JRadioButton("Down");
        ButtonGroup directionGroup = new ButtonGroup();
        directionGroup.add(upButton);
        directionGroup.add(downButton);
        downButton.setSelected(true); // Default selection

        directionPanel.add(upButton);
        directionPanel.add(downButton);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton findButton = new JButton("Find");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(findButton);
        buttonPanel.add(cancelButton);

        // Action Listeners
        findButton.addActionListener(e -> findNextOrPrevious(upButton.isSelected()));
        cancelButton.addActionListener(e -> searchDialog.dispose());

        // Add Enter key functionality for search
        searchField.addActionListener(e -> findButton.doClick());

        // Combine panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(optionsPanel, BorderLayout.CENTER);
        mainPanel.add(directionPanel, BorderLayout.WEST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        searchDialog.add(mainPanel);
        searchDialog.pack();
        searchDialog.setLocationRelativeTo(null);
        searchDialog.setVisible(true);
    }

    private static void findNextOrPrevious(boolean searchUp) {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null)
            return;

        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty())
            return;

        // Use SwingWorker for search to prevent UI lag
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                JTextArea textArea = currentTab.textArea;
                String text = textArea.getText();
                int caretPos = textArea.getCaretPosition();
                int textLength = text.length();

                boolean caseSensitive = caseSensitiveBox.isSelected();
                String searchText = caseSensitive ? text : text.toLowerCase();
                String searchQuery = caseSensitive ? searchTerm : searchTerm.toLowerCase();

                int foundPos = -1;
                if (searchUp) {
                    int startSearchPos = caretPos - 1;
                    foundPos = searchText.lastIndexOf(searchQuery, startSearchPos);

                    if (foundPos == -1 && wrapAroundCheckBox.isSelected()) {
                        foundPos = searchText.lastIndexOf(searchQuery);
                    }
                } else {
                    int startSearchPos = caretPos + 1;
                    foundPos = searchText.indexOf(searchQuery, startSearchPos);

                    if (foundPos == -1 && wrapAroundCheckBox.isSelected()) {
                        foundPos = searchText.indexOf(searchQuery);
                    }
                }

                return foundPos;
            }

            @Override
            protected void done() {
                try {
                    int foundPos = get();
                    if (foundPos != -1) {
                        currentTab.textArea.setCaretPosition(foundPos);
                        currentTab.textArea.select(foundPos, foundPos + searchTerm.length());
                        currentTab.textArea.requestFocusInWindow();
                    } else {
                        JOptionPane.showMessageDialog(null, "Text not found");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Search error: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private static TabInfo getCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
    }

    private static void closeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            TabInfo tab = tabs.get(index);
            if (tab.monitoringThread != null) {
                tab.monitoringThread.interrupt();
            }
            tabs.remove(index);
            tabbedPane.remove(index);

            // Update tab titles and names
            for (int i = 0; i < tabs.size(); i++) {
                TabInfo currentTab = tabs.get(i);
                currentTab.tabName = "Log " + (i + 1);
                createTabComponent(currentTab.tabName, i);
            }

            try {
                saveConfiguration();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveConfiguration() throws JSONException {
        List<Configuration.TabConfig> tabConfigs = new ArrayList<>();
        for (TabInfo tab : tabs) {
            Configuration.TabConfig config = new Configuration.TabConfig();
            config.host = tab.host;
            config.user = tab.user;
            config.logFile = tab.logFile;
            config.tabName = tab.tabName;
            config.textColor = tab.textColor;
            config.backgroundColor = tab.backgroundColor;
            config.font = tab.currentFont;
            tabConfigs.add(config);
        }
        Configuration.saveConfig(isDarkMode, tabConfigs);
    }

    private static void saveTabSettings(TabInfo tabInfo, String host, String user, String password, String logFile) {
        tabInfo.host = host;
        tabInfo.user = user;
        tabInfo.password = password;
        tabInfo.logFile = logFile;

        // Restart monitoring with new settings
        if (tabInfo.monitoringThread != null) {
            tabInfo.monitoringThread.interrupt();
        }

        startFileMonitoring(tabInfo);

        JOptionPane.showMessageDialog(null, "Settings saved successfully!");
    }

    private static void updateStatus(TabInfo tabInfo, boolean connected, String message) {
        SwingUtilities.invokeLater(() -> {
            tabInfo.statusLabel.setText("Status: " + (connected ? "Connected" : "Disconnected")
                    + (message.isEmpty() ? "" : " - " + message));
            tabInfo.statusLabel.setForeground(connected ? new Color(0, 150, 0) : Color.RED);
            tabInfo.isConnected.set(connected);
            updateLastUpdateTime(tabInfo);
        });
    }

    private static void updateLastUpdateTime(TabInfo tabInfo) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tabInfo.lastUpdateLabel.setText("Last Update: " + sdf.format(new Date()));
    }

    private static void appendToLog(TabInfo tabInfo, String content) {
        SwingUtilities.invokeLater(() -> {
            if (!content.contains("JNI_OnLoad called")) {
                String formattedContent;
                // Check if content already has a timestamp
                if (content.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
                    formattedContent = content.replace("\r", "") + "\n";
                } else {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    formattedContent = String.format("[%s] %s\n", timestamp, content.trim().replace("\r", ""));
                }

                // Handle HTML special characters
                String escapedContent = formattedContent
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");

                tabInfo.textArea.append(escapedContent);

                if (tabInfo.rawLogContent == null) {
                    tabInfo.rawLogContent = new StringBuilder();
                }
                tabInfo.rawLogContent.append(escapedContent);

                try {
                    tabInfo.textArea.setCaretPosition(tabInfo.textArea.getDocument().getLength());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void appendToLogWithFiltering(TabInfo tabInfo, String content, boolean addToRaw) {
        SwingUtilities.invokeLater(() -> {
            if (!content.contains("JNI_OnLoad called")) {
                boolean shouldAppend = !tabInfo.filterDuplicates ||
                                     !tabInfo.textArea.getText().contains(content);

                if (shouldAppend) {
                    String formattedContent;
                    // Check if content already has a timestamp
                    if (content.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
                        formattedContent = content + "\n";
                    } else {
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        formattedContent = String.format("[%s] %s\n", timestamp, content.trim());
                    }

                    tabInfo.textArea.append(formattedContent);

                    if (addToRaw) {
                        if (tabInfo.rawLogContent == null) {
                            tabInfo.rawLogContent = new StringBuilder();
                        }
                        tabInfo.rawLogContent.append(formattedContent);
                    }

                    try {
                        tabInfo.textArea.setCaretPosition(tabInfo.textArea.getDocument().getLength());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private static void clearLog(TabInfo tabInfo) {
        SwingUtilities.invokeLater(() -> {
            tabInfo.textArea.setText("");
            tabInfo.lastContent = "";
            // Clear raw content as well
            tabInfo.rawLogContent = new StringBuilder();
            // Reset caret position
            tabInfo.textArea.setCaretPosition(0);
        });
    }

    private static void fetchLogFile(TabInfo tabInfo) {
        Session session = null;
        ChannelExec channel = null;
        BufferedReader reader = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(tabInfo.user, tabInfo.host, 22);
            session.setPassword(tabInfo.password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(30000);

            // Check if file exists
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("test -f " + tabInfo.logFile + " && echo 'EXISTS' || echo 'NOT_FOUND'");

            reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8));
            channel.connect();

            String fileExists = reader.readLine();
            if (!"EXISTS".equals(fileExists)) {
                updateStatus(tabInfo, true, "Log file not found");
                return;
            }

            // Get file size
            channel.disconnect();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("wc -c < " + tabInfo.logFile);

            reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8));
            channel.connect();

            String fileSizeStr = reader.readLine();
            long currentSize = Long.parseLong(fileSizeStr.trim());

            if (currentSize == tabInfo.lastModified) {
                updateStatus(tabInfo, true, "No changes");
                return;
            }

            // Read new content using base64 encoding to preserve special characters
            channel.disconnect();
            channel = (ChannelExec) session.openChannel("exec");

            String command;
            if (tabInfo.lastModified == 0) {
                command = "base64 " + tabInfo.logFile;
            } else {
                long bytesToRead = currentSize - tabInfo.lastModified;
                command = String.format("tail -c %d %s | base64", bytesToRead, tabInfo.logFile);
            }

            channel.setCommand(command);
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8));
            channel.connect();

            StringBuilder base64Content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                base64Content.append(line);
            }

            // Decode base64 content
            byte[] decodedBytes = Base64.getDecoder().decode(base64Content.toString());
            String decodedContent = new String(decodedBytes, StandardCharsets.UTF_8);

            if (!decodedContent.isEmpty()) {
                String[] lines = decodedContent.split("\n");
                for (String logLine : lines) {
                    if (!logLine.trim().isEmpty() && !logLine.contains("JNI_OnLoad called")) {
                        appendToLog(tabInfo, logLine);
                    }
                }
            }

            tabInfo.lastModified = currentSize;
            updateStatus(tabInfo, true, "Updated");

        } catch (JSchException e) {
            updateStatus(tabInfo, false, "Connection error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            updateStatus(tabInfo, false, "File read error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (channel != null)
                    channel.disconnect();
                if (session != null)
                    session.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void refreshLog(TabInfo tabInfo) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                fetchLogFile(tabInfo);
                return null;
            }
        };
        worker.execute();
    }

    private static void startFileMonitoring(TabInfo tabInfo) {
        tabInfo.monitoringThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    fetchLogFile(tabInfo);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        tabInfo.monitoringThread.setDaemon(true);
        tabInfo.monitoringThread.start();
    }

    private static JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton newTabButton = new JButton("New Tab");
        styleButton(newTabButton);
        newTabButton.addActionListener(e -> {
            try {
                addNewTab();
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        });
        toolbar.add(newTabButton);
        toolbar.addSeparator();

        JButton searchButton = new JButton("Search");
        styleButton(searchButton);
        searchButton.addActionListener(e -> showSearchDialog());
        toolbar.add(searchButton);

        JButton themeButton = new JButton("Toggle Theme");
        styleButton(themeButton);
        themeButton.addActionListener(e -> toggleTheme());
        toolbar.add(themeButton);

        JButton textOptionsButton = new JButton("Text Options");
        styleButton(textOptionsButton);
        textOptionsButton.addActionListener(e -> showTextOptionsDialog());
        toolbar.add(textOptionsButton);

        JButton resetButton = new JButton("Reset To Default");
        styleButton(resetButton);
        resetButton.addActionListener(e -> resetToDefault());
        toolbar.add(resetButton);

        JButton exportButton = new JButton("Export Log");
        styleButton(exportButton);
        exportButton.addActionListener(e -> exportLog());
        toolbar.add(exportButton);

        JButton toggleDuplicatesButton = new JButton("Toggle Duplicates");
        styleButton(toggleDuplicatesButton);
        toggleDuplicatesButton.addActionListener(e -> toggleDuplicateFiltering());
        toolbar.add(toggleDuplicatesButton);

        JButton wordWrapButton = new JButton("Word Wrap");
        styleButton(wordWrapButton);
        wordWrapButton.addActionListener(e -> toggleWordWrap(wordWrapButton));
        toolbar.add(wordWrapButton);

        return toolbar;
    }

    private static void createTabComponent(String title, int index) {
        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabComponent.setOpaque(false);

        TabInfo tab = tabs.get(index);
        JLabel titleLabel = new JLabel(tab.tabName + " ");
        titleLabel.setOpaque(false);

        // Add right-click menu for renaming
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename Tab");
        renameItem.addActionListener(e -> showRenameDialog(index));
        popupMenu.add(renameItem);

        titleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(titleLabel, e.getX(), e.getY());
                }
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(titleLabel, e.getX(), e.getY());
                }
            }
        });

        JButton closeButton = new JButton("\u2715");
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setContentAreaFilled(true);
        closeButton.setBackground(isDarkMode ? DARK_BUTTON_BG : LIGHT_BUTTON_BG);
        closeButton.setForeground(isDarkMode ? DARK_BUTTON_TEXT : LIGHT_BUTTON_TEXT);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        closeButton.setFocusable(false);
        closeButton.addActionListener(e -> closeTab(index));

        tabComponent.add(titleLabel);
        tabComponent.add(closeButton);

        tabbedPane.setTabComponentAt(index, tabComponent);
    }

    private static void showRenameDialog(int index) {
        TabInfo tab = tabs.get(index);
        String newName = JOptionPane.showInputDialog(tabbedPane, "Enter new tab name:", tab.tabName);

        if (newName != null && !newName.trim().isEmpty()) {
            tab.tabName = newName.trim();
            createTabComponent(tab.tabName, index);
            try {
                saveConfiguration();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static void setupHotkeys(JFrame frame) {
        KeyStroke newTabKey = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            try {
                addNewTab();
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }, newTabKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke closeTabKey = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex != -1) {
                closeTab(selectedIndex);
            }
        }, closeTabKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke saveKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            try {
                saveTabSettings(getCurrentTab(), hostField.getText(), userField.getText(),
                        new String(passwordField.getPassword()), logFileField.getText());
                saveConfiguration();
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }
        }, saveKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke renameKey = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        frame.getRootPane().registerKeyboardAction(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex != -1) {
                showRenameDialog(selectedIndex);
            }
        }, renameKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke searchKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            showSearchDialog();
        }, searchKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke exportKey = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            exportLog();
        }, exportKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(isDarkMode ? DARK_BUTTON_BG : LIGHT_BUTTON_BG);
        button.setForeground(isDarkMode ? DARK_BUTTON_TEXT : LIGHT_BUTTON_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? DARK_BUTTON_TEXT : Color.GRAY, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    private static void addOptionField(JPanel optionsPanel, String label, JComponent field,
            GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        optionsPanel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        optionsPanel.add(field, gbc);
    }

    private static void showTextOptionsDialog() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null || currentTab.currentFont == null)
            return;

        JDialog dialog = new JDialog((Frame) null, "Text Options", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        JComboBox<String> fontFamily = new JComboBox<>(fonts);
        fontFamily.setSelectedItem(currentTab.currentFont.getFamily());
        addOptionField(optionsPanel, "Font:", fontFamily, gbc, 0);

        Integer[] sizes = { 8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40, 48, 56, 64, 72 };
        JComboBox<Integer> fontSize = new JComboBox<>(sizes);
        fontSize.setSelectedItem(currentTab.currentFont.getSize());
        addOptionField(optionsPanel, "Size:", fontSize, gbc, 1);

        String[] styles = { "Plain", "Bold", "Italic", "Bold Italic" };
        JComboBox<String> fontStyle = new JComboBox<>(styles);
        fontStyle.setSelectedIndex(currentTab.currentFont.getStyle());
        addOptionField(optionsPanel, "Style:", fontStyle, gbc, 2);

        JButton textColorBtn = new JButton("Choose Text Color");
        textColorBtn.setBackground(currentTab.textColor);
        addOptionField(optionsPanel, "Text Color:", textColorBtn, gbc, 3);

        JButton bgColorBtn = new JButton("Choose Background Color");
        bgColorBtn.setBackground(currentTab.backgroundColor);
        addOptionField(optionsPanel, "Background:", bgColorBtn, gbc, 4);

        JTextArea preview = new JTextArea("Preview Text");
        preview.setFont(currentTab.currentFont);
        preview.setForeground(currentTab.textColor);
        preview.setBackground(currentTab.backgroundColor);
        preview.setPreferredSize(new Dimension(300, 100));
        preview.setMargin(new Insets(5, 5, 5, 5));
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.add(new JScrollPane(preview), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        fontFamily.addActionListener(e -> updatePreview(preview, fontFamily, fontSize, fontStyle));
        fontSize.addActionListener(e -> updatePreview(preview, fontFamily, fontSize, fontStyle));
        fontStyle.addActionListener(e -> updatePreview(preview, fontFamily, fontSize, fontStyle));

        textColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Text Color", currentTab.textColor);
            if (newColor != null) {
                preview.setForeground(newColor);
                textColorBtn.setBackground(newColor);
            }
        });

        bgColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Background Color", currentTab.backgroundColor);
            if (newColor != null) {
                preview.setBackground(newColor);
                bgColorBtn.setBackground(newColor);
            }
        });

        applyButton.addActionListener(e -> {
            currentTab.currentFont = preview.getFont();
            currentTab.textColor = preview.getForeground();
            currentTab.backgroundColor = preview.getBackground();
            currentTab.textArea.setFont(currentTab.currentFont);
            currentTab.textArea.setForeground(currentTab.textColor);
            currentTab.textArea.setBackground(currentTab.backgroundColor);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(optionsPanel, BorderLayout.NORTH);
        dialog.add(previewPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private static void updatePreview(JTextArea preview, JComboBox<String> fontFamily,
            JComboBox<Integer> fontSize, JComboBox<String> fontStyle) {
        int style;
        switch (fontStyle.getSelectedIndex()) {
            case 1:
                style = Font.BOLD;
                break;
            case 2:
                style = Font.ITALIC;
                break;
            case 3:
                style = Font.BOLD | Font.ITALIC;
                break;
            default:
                style = Font.PLAIN;
        }

        Font newFont = new Font(
                (String) fontFamily.getSelectedItem(),
                style,
                (Integer) fontSize.getSelectedItem());
        preview.setFont(newFont);
    }

    private static void resetToDefault() {
        int confirm = JOptionPane.showConfirmDialog(null,
                "Reset all settings to default values?",
                "Reset Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            isDarkMode = false;

            Window window = SwingUtilities.getWindowAncestor(tabbedPane);
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;

                applyTheme(frame.getContentPane(), false);

                for (TabInfo tab : tabs) {
                    tab.currentFont = new Font("Monospaced", Font.PLAIN, 12);
                    tab.textColor = Color.BLACK;
                    tab.backgroundColor = Color.WHITE;
                    tab.textArea.setFont(tab.currentFont);
                    tab.textArea.setForeground(tab.textColor);
                    tab.textArea.setBackground(tab.backgroundColor);
                }

                SwingUtilities.updateComponentTreeUI(frame);
            }

            try {
                saveConfiguration();
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }
        }
    }

    // Clear the log file on the linux machine
    private static void clearLogFromLinux(TabInfo tabInfo) {
        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(tabInfo.user, tabInfo.host, 22);
            session.setPassword(tabInfo.password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(30000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("truncate -s 0 " + tabInfo.logFile);
            channel.connect();

            // Clear all content including raw content
            tabInfo.textArea.setText("");
            tabInfo.lastContent = "";
            tabInfo.rawLogContent = new StringBuilder();
            tabInfo.lastModified = 0;
            tabInfo.textArea.setCaretPosition(0);

            updateStatus(tabInfo, true, "Log cleared");
        } catch (Exception e) {
            updateStatus(tabInfo, false, "Error clearing log: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private static void exportLog() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null) {
            JOptionPane.showMessageDialog(null, "No tab selected to export");
            return;
        }

        // Get default documents directory
        String userHome = System.getProperty("user.home");
        File documentsDir = new File(userHome, "Documents");

        // Create file chooser
        JFileChooser fileChooser = new JFileChooser(documentsDir);
        fileChooser.setDialogTitle("Export Log");

        // Set default file name with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String defaultFileName = "log_export_" + timestamp + ".txt";
        fileChooser.setSelectedFile(new File(defaultFileName));

        // Add file filter
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }

            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });

        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Add .txt extension if not present
            if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
            }

            // Confirm overwrite if file exists
            if (selectedFile.exists()) {
                int response = JOptionPane.showConfirmDialog(null,
                    "File already exists. Do you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                StringBuilder content = new StringBuilder();
                content.append("Log Export from SCP Log Viewer\n");
                content.append("Exported on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
                content.append("Host: ").append(currentTab.host).append("\n");
                content.append("Log File: ").append(currentTab.logFile).append("\n");
                content.append("\n=== Log Content ===\n\n");

                // Use the raw content if available, otherwise fall back to text area content
                if (currentTab.rawLogContent != null && currentTab.rawLogContent.length() > 0) {
                    content.append(currentTab.rawLogContent);
                } else {
                    content.append(currentTab.textArea.getText());
                }

                // Write to file
                Files.write(selectedFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));

                JOptionPane.showMessageDialog(null,
                    "Log exported successfully to:\n" + selectedFile.getAbsolutePath(),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                    "Error exporting log: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void toggleDuplicateFiltering() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null) return;

        currentTab.filterDuplicates = !currentTab.filterDuplicates;

        // Only reprocess if there's content
        if (currentTab.rawLogContent != null && currentTab.rawLogContent.length() > 0) {
            currentTab.textArea.setText("");
            String[] lines = currentTab.rawLogContent.toString().split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    appendToLogWithFiltering(currentTab, line, false);
                }
            }
        }

        // Find and update the toggle button in the toolbar
        Window window = SwingUtilities.getWindowAncestor(tabbedPane);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) comp;
                    for (Component button : toolbar.getComponents()) {
                        if (button instanceof JButton &&
                            (((JButton) button).getText().contains("Duplicates"))) {
                            ((JButton) button).setText(currentTab.filterDuplicates ?
                                "Show Duplicates" : "Filter Duplicates");
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private static void toggleWordWrap(JButton wordWrapButton) {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null) return;

        boolean isWrapped = !currentTab.textArea.getLineWrap();
        currentTab.textArea.setLineWrap(isWrapped);
        currentTab.textArea.setWrapStyleWord(isWrapped);
        wordWrapButton.setText(isWrapped ? "Disable Wrap" : "Word Wrap");
    }
}
