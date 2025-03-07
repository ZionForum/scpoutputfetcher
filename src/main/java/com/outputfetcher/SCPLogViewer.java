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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

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

    // Connection settings
    private static String host = "localhost";
    private static int port = 22;
    private static String username = "";
    private static String password = "";

    private static final Color DARK_MODE_BACKGROUND = new Color(43, 43, 43);
    private static final Color LIGHT_MODE_BACKGROUND = Color.WHITE;
    private static final Color DARK_MODE_TEXT = new Color(255, 255, 255);
    private static final Color LIGHT_MODE_BUTTON_BACKGROUND = new Color(230, 230, 230);
    private static final Color LIGHT_MODE_BUTTON_TEXT = Color.BLACK;
    private static final Color DARK_LIGHT_MODE_BUTTON_BACKGROUND = new Color(86, 156, 214);
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
        frame.setMinimumSize(new Dimension(800, 600)); // Set minimum size

        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // Set the background color of the tabbed pane
        tabbedPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        tabbedPane.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        JPanel tabPanel = new JPanel(new BorderLayout());
        // Set the background color of the tab panel
        tabPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
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

        // Update the UI with the new theme
        Window window = SwingUtilities.getWindowAncestor(tabbedPane);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;

            // Update tabbed pane background
            tabbedPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
            tabbedPane.getParent().setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

            // Update the toolbar first
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) comp;
                    toolbar.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0,
                                    isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                    toolbar.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));

                    // Update theme button text
                    for (Component button : toolbar.getComponents()) {
                        if (button instanceof JButton) {
                            JButton btn = (JButton) button;
                            btn.setBorderPainted(false);
                            btn.setOpaque(true);
                            if (btn.getText().contains("Theme")) {
                                btn.setText(isDarkMode ? "Light Theme" : "Dark Theme");
                                btn.setToolTipText("Switch to " + (isDarkMode ? "light" : "dark") + " theme");
                            }
                            // Ensure all toolbar buttons have proper styling
                            styleButton(btn);
                        }
                    }
                    break;
                }
            }

            // Apply theme to all components
            applyTheme(frame.getContentPane(), isDarkMode);
            updateButtonStyles();

            // Recreate tab components to ensure proper styling
            for (int i = 0; i < tabs.size(); i++) {
                createTabComponent(tabs.get(i).tabName, i);
            }

            // Refresh all tab contents to ensure proper styling
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof JPanel) {
                    updatePanelTheme((JPanel) comp);
                }
            }
        }

        try {
            saveConfiguration();
        } catch (JSONException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving theme configuration: " + e.getMessage());
        }
    }

    // Helper method to update panel theme
    private static void updatePanelTheme(JPanel panel) {
        panel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        panel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                updatePanelTheme((JPanel) comp);
            } else if (comp instanceof JButton) {
                styleButton((JButton) comp);
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                scrollPane.setBorder(BorderFactory.createLineBorder(
                        isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)));

                if (scrollPane.getViewport() != null &&
                        scrollPane.getViewport().getView() instanceof JTextArea) {
                    // Don't change text area styling as it's handled separately
                }
            }
        }
    }

    private static void applyTheme(Container container, boolean dark) {
        Color bg = dark ? DARK_MODE_BACKGROUND : Color.WHITE;
        Color fg = dark ? DARK_MODE_TEXT : Color.BLACK;
        Color inputBg = dark ? new Color(60, 60, 60) : Color.WHITE;

        container.setBackground(bg);
        container.setForeground(fg);

        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextArea) {
                // Don't override tab-specific text area settings
                TabInfo tab = getTabForTextArea((JTextArea) comp);
                if (tab == null) {
                    comp.setBackground(bg);
                    comp.setForeground(fg);
                }
            } else if (comp instanceof JTextField || comp instanceof JPasswordField) {
                comp.setBackground(inputBg);
                comp.setForeground(fg);
                if (comp instanceof JTextField) {
                    ((JTextField) comp).setCaretColor(fg);
                } else if (comp instanceof JPasswordField) {
                    ((JPasswordField) comp).setCaretColor(fg);
                }
            } else if (comp instanceof JComboBox) {
                comp.setBackground(inputBg);
                comp.setForeground(fg);
            } else if (comp instanceof JButton) {
                styleButton((JButton) comp);
            } else if (comp instanceof JLabel) {
                comp.setForeground(fg);
            } else if (comp instanceof JCheckBox || comp instanceof JRadioButton) {
                comp.setBackground(bg);
                comp.setForeground(fg);
            } else if (comp instanceof JPanel) {
                comp.setBackground(bg);
                comp.setForeground(fg);
                applyTheme((Container) comp, dark);
            } else if (comp instanceof JScrollPane) {
                comp.setBackground(bg);
                comp.setForeground(fg);
                JScrollPane scrollPane = (JScrollPane) comp;
                if (scrollPane.getViewport() != null) {
                    scrollPane.getViewport().setBackground(bg);
                    scrollPane.getViewport().setForeground(fg);
                    Component view = scrollPane.getViewport().getView();
                    if (view instanceof Container) {
                        applyTheme((Container) view, dark);
                    }
                }
            } else if (comp instanceof JTabbedPane) {
                comp.setBackground(bg);
                System.out.println("fg: " + fg);
                comp.setForeground(fg);
                JTabbedPane tp = (JTabbedPane) comp;
                for (int i = 0; i < tp.getTabCount(); i++) {
                    Component tabComp = tp.getComponentAt(i);
                    String tabText = tp.getTitleAt(i);
                    System.out.println("Tab text: " + tabText);
                    if (tabComp instanceof Container) {
                        applyTheme((Container) tabComp, dark);
                    }
                }
            } else if (comp instanceof Container) {
                applyTheme((Container) comp, dark);
            }
        }
    }

    // Helper method to find which tab a text area belongs to
    private static TabInfo getTabForTextArea(JTextArea textArea) {
        for (TabInfo tab : tabs) {
            if (tab.textArea == textArea) {
                return tab;
            }
        }
        return null;
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
        statusPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        tabInfo.statusLabel = new JLabel("Status: Disconnected");
        tabInfo.statusLabel.setForeground(Color.RED);
        tabInfo.statusLabel.setFont(tabInfo.statusLabel.getFont().deriveFont(Font.BOLD));

        tabInfo.lastUpdateLabel = new JLabel("Last Update: Never");
        tabInfo.lastUpdateLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : new Color(100, 100, 100));

        statusPanel.add(tabInfo.statusLabel);
        statusPanel.add(tabInfo.lastUpdateLabel);

        // Text area setup
        setupTextArea(tabInfo);
        JScrollPane scrollPane = new JScrollPane(tabInfo.textArea);
        scrollPane.setBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling

        // Button panel
        JPanel buttonPanel = createButtonPanel(tabInfo);

        // Combine panels
        JPanel topPanel = new JPanel(new BorderLayout(0, 10)); // Add spacing between components
        topPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
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
        tabInfo.textArea.setMargin(new Insets(8, 8, 8, 8)); // Add padding for better readability

        // Add right-click menu
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)));

        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem selectAllItem = new JMenuItem("Select All");
        JMenuItem clearItem = new JMenuItem("Clear Log");
        JMenuItem findItem = new JMenuItem("Find...");

        // Style menu items
        for (JMenuItem item : new JMenuItem[] { copyItem, selectAllItem, clearItem, findItem }) {
            item.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
            item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
            item.setFont(item.getFont().deriveFont(12f)); // Consistent font size

            // Add hover effect
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    item.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                    item.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    item.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
                    item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
                }
            });
        }

        copyItem.addActionListener(e -> tabInfo.textArea.copy());
        selectAllItem.addActionListener(e -> tabInfo.textArea.selectAll());
        clearItem.addActionListener(e -> clearLogFromLinux(tabInfo));
        findItem.addActionListener(e -> showSearchDialog());

        // Add keyboard shortcuts
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));

        popup.add(copyItem);
        popup.add(selectAllItem);
        popup.addSeparator();
        popup.add(findItem);
        popup.addSeparator();
        popup.add(clearItem);

        tabInfo.textArea.setComponentPopupMenu(popup);
    }

    private static JPanel createSettingsPanel(TabInfo tabInfo) {
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        settingsPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Host field
        hostField = new JTextField(tabInfo.host != null ? tabInfo.host : "192.168.0.103");
        hostField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        hostField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        hostField.setCaretColor(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        hostField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        addSettingsField(settingsPanel, "Host:", hostField, gbc, 0);

        // User field
        userField = new JTextField(tabInfo.user != null ? tabInfo.user : "root");
        userField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        userField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        userField.setCaretColor(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        userField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        addSettingsField(settingsPanel, "User:", userField, gbc, 1);

        // Password field
        passwordField = new JPasswordField(tabInfo.password);
        passwordField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        passwordField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        passwordField.setCaretColor(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        addSettingsField(settingsPanel, "Password:", passwordField, gbc, 2);

        // Log file field
        logFileField = new JTextField(tabInfo.logFile != null ? tabInfo.logFile : "/home/user1/Desktop/output.log");
        logFileField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        logFileField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        logFileField.setCaretColor(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        logFileField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        addSettingsField(settingsPanel, "Log File:", logFileField, gbc, 3);

        // Connect button
        JButton connectBtn = new JButton("CONNECT");
        connectBtn.setBorderPainted(false);
        connectBtn.setOpaque(true);
        connectBtn.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        connectBtn.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
        connectBtn.setFont(connectBtn.getFont().deriveFont(Font.BOLD));
        connectBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(0, 90, 180)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        connectBtn.setFocusPainted(false);

        // Add hover effect
        connectBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                connectBtn.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                connectBtn.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }
        });

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 5, 5, 5); // Add more top padding
        settingsPanel.add(connectBtn, gbc);

        // Store references
        tabInfo.host = hostField.getText();
        tabInfo.user = userField.getText();
        tabInfo.password = new String(passwordField.getPassword());
        tabInfo.logFile = logFileField.getText();

        // Add action to connect button
        ActionListener connectAction = e -> {
            saveTabSettings(tabInfo, hostField.getText(),
                    userField.getText(), new String(passwordField.getPassword()), logFileField.getText());
            try {
                saveConfiguration();
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }
        };

        // Add listeners
        connectBtn.addActionListener(connectAction);

        // Add Enter key listener to password field
        passwordField.addActionListener(connectAction);

        return settingsPanel;
    }

    private static void addSettingsField(JPanel panel, String label, JComponent field,
            GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;

        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD));
        panel.add(labelComponent, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private static JPanel createButtonPanel(TabInfo tabInfo) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Add more vertical padding
        buttonPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); // Add some vertical padding

        // Create buttons with proper styling
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBorderPainted(false);
        refreshButton.setOpaque(true);
        refreshButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFont(refreshButton.getFont().deriveFont(Font.BOLD));
        refreshButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(0, 90, 180)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15) // Larger padding for better appearance
        ));
        refreshButton.setFocusPainted(false);

        JButton clearButton = new JButton("Clear");
        clearButton.setBorderPainted(false);
        clearButton.setOpaque(true);
        clearButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        clearButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
        clearButton.setFont(clearButton.getFont().deriveFont(Font.BOLD));
        clearButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND),
                BorderFactory.createEmptyBorder(8, 15, 8, 15) // Larger padding for better appearance
        ));
        clearButton.setFocusPainted(false);

        // Add hover effects
        refreshButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                refreshButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                refreshButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }
        });

        clearButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (isDarkMode) {
                    clearButton.setBackground(DARK_LIGHT_MODE_BUTTON_BACKGROUND);
                    clearButton.setForeground(Color.WHITE);
                } else {
                    clearButton.setBackground(LIGHT_MODE_BUTTON_BACKGROUND);
                    clearButton.setForeground(Color.BLACK);
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                clearButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                clearButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
            }
        });

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
        searchDialog.setMinimumSize(new Dimension(400, 200)); // Set minimum size
        searchDialog.setLocationRelativeTo(null);

        // Apply theme to dialog
        Container dialogContainer = searchDialog.getContentPane();
        dialogContainer.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        dialogContainer.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        // Search Input Panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        searchField = new JTextField(30);
        searchField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        searchField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        searchField.setCaretColor(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JLabel findLabel = new JLabel("Find:");
        findLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        findLabel.setFont(findLabel.getFont().deriveFont(Font.BOLD));
        searchPanel.add(findLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Options Panel (Match case and Wrap around)
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        caseSensitiveBox = new JCheckBox("Match case");
        wrapAroundCheckBox = new JCheckBox("Wrap around");
        wrapAroundCheckBox.setSelected(true); // Default to wrap around

        caseSensitiveBox.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        caseSensitiveBox.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        wrapAroundCheckBox.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        wrapAroundCheckBox.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        optionsPanel.add(caseSensitiveBox);
        optionsPanel.add(wrapAroundCheckBox);

        // Direction Panel with improved radio button handling
        JPanel directionPanel = new JPanel();
        directionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(200, 200, 200)),
                "Search Direction",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                null,
                isDarkMode ? DARK_MODE_TEXT : Color.BLACK));
        directionPanel.setLayout(new GridLayout(1, 2));
        directionPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        directionPanel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        JRadioButton upButton = new JRadioButton("Up");
        JRadioButton downButton = new JRadioButton("Down");
        upButton.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        upButton.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        downButton.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        downButton.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        ButtonGroup directionGroup = new ButtonGroup();
        directionGroup.add(upButton);
        directionGroup.add(downButton);
        downButton.setSelected(true); // Default selection

        directionPanel.add(upButton);
        directionPanel.add(downButton);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

        // Create buttons with proper styling
        JButton findButton = new JButton("Find");
        JButton cancelButton = new JButton("Cancel");

        // Apply custom styling to buttons
        findButton.setBorderPainted(false);
        findButton.setOpaque(true);
        findButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        findButton.setForeground(Color.WHITE);
        findButton.setFont(findButton.getFont().deriveFont(Font.BOLD));
        findButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(0, 90, 180)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        findButton.setFocusPainted(false);

        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        cancelButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        cancelButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
        cancelButton.setFont(cancelButton.getFont().deriveFont(Font.BOLD));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        cancelButton.setFocusPainted(false);

        // Add hover effects
        findButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                findButton.setBackground(isDarkMode ? new Color(100, 170, 230) : new Color(30, 144, 255));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                findButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }
        });

        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (isDarkMode) {
                    cancelButton.setBackground(new Color(100, 100, 100));
                    cancelButton.setForeground(new Color(255, 255, 255));
                } else {
                    cancelButton.setBackground(new Color(210, 210, 210));
                    cancelButton.setForeground(new Color(0, 0, 0));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                cancelButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
            }
        });

        buttonPanel.add(findButton);
        buttonPanel.add(cancelButton);

        // Action Listeners
        findButton.addActionListener(e -> findNextOrPrevious(upButton.isSelected()));
        cancelButton.addActionListener(e -> searchDialog.dispose());

        // Add Enter key functionality for search
        searchField.addActionListener(e -> findButton.doClick());

        // Combine panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
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

                boolean caseSensitive = caseSensitiveBox.isSelected();
                String searchText = caseSensitive ? text : text.toLowerCase();
                String searchQuery = caseSensitive ? searchTerm : searchTerm.toLowerCase();

                int foundPos = -1;
                if (searchUp) {
                    // Fix for "Up" search - make sure we're searching from the current position
                    // upward
                    int startSearchPos = Math.min(caretPos, searchText.length());

                    // If we have a selection, start from the beginning of the selection
                    if (textArea.getSelectionStart() < textArea.getSelectionEnd() &&
                            textArea.getSelectionEnd() == caretPos) {
                        startSearchPos = textArea.getSelectionStart();
                    }

                    // Search upward from the current position
                    if (startSearchPos > 0) {
                        foundPos = searchText.lastIndexOf(searchQuery, startSearchPos - 1);
                    }

                    // If not found and wrap around is enabled, search from the end
                    if (foundPos == -1 && wrapAroundCheckBox.isSelected()) {
                        foundPos = searchText.lastIndexOf(searchQuery);
                    }
                } else {
                    // For "Down" search, start from the end of the current selection or caret
                    // position
                    int startSearchPos = textArea.getSelectionEnd();
                    if (startSearchPos == textArea.getSelectionStart()) {
                        startSearchPos = caretPos;
                    }

                    if (startSearchPos < searchText.length()) {
                        foundPos = searchText.indexOf(searchQuery, startSearchPos);
                    }

                    // If not found and wrap around is enabled, search from the beginning
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
                        JOptionPane.showMessageDialog(null,
                                "Text not found" + (wrapAroundCheckBox.isSelected() ? " in the entire document"
                                        : " in the search direction"),
                                "Search Result",
                                JOptionPane.INFORMATION_MESSAGE);
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
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        toolbar.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));

        // Create a consistent button style
        JButton newTabButton = createToolbarButton("New Tab", "Create a new log tab");
        newTabButton.addActionListener(e -> {
            try {
                addNewTab();
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        });
        toolbar.add(newTabButton);
        toolbar.addSeparator();

        JButton searchButton = createToolbarButton("Search", "Search in the current log");
        searchButton.addActionListener(e -> showSearchDialog());
        toolbar.add(searchButton);

        JButton themeButton = createToolbarButton(isDarkMode ? "Light Theme" : "Dark Theme",
                "Toggle between light and dark theme");
        themeButton.addActionListener(e -> {
            toggleTheme();
            themeButton.setText(isDarkMode ? "Light Theme" : "Dark Theme");
            themeButton.setToolTipText("Switch to " + (isDarkMode ? "light" : "dark") + " theme");
        });
        toolbar.add(themeButton);

        JButton textOptionsButton = createToolbarButton("Text Options", "Change font and colors");
        textOptionsButton.addActionListener(e -> showTextOptionsDialog());
        toolbar.add(textOptionsButton);

        JButton resetButton = createToolbarButton("Reset Defaults", "Reset all settings to default values");
        resetButton.addActionListener(e -> resetToDefault());
        toolbar.add(resetButton);

        JButton exportButton = createToolbarButton("Export Log", "Save the current log to a file");
        exportButton.addActionListener(e -> exportLog());
        toolbar.add(exportButton);

        TabInfo currentTab = getCurrentTab();
        boolean filteringEnabled = currentTab != null && currentTab.filterDuplicates;

        JButton toggleDuplicatesButton = createToolbarButton(
                filteringEnabled ? "Show Duplicates" : "Filter Duplicates",
                "Toggle filtering of duplicate log entries");
        toggleDuplicatesButton.addActionListener(e -> {
            toggleDuplicateFiltering();
            boolean isFiltering = getCurrentTab() != null && getCurrentTab().filterDuplicates;
            toggleDuplicatesButton.setText(isFiltering ? "Show Duplicates" : "Filter Duplicates");
        });
        toolbar.add(toggleDuplicatesButton);

        JButton wordWrapButton = createToolbarButton("Word Wrap", "Toggle word wrapping in the log view");
        wordWrapButton.addActionListener(e -> toggleWordWrap(wordWrapButton));
        toolbar.add(wordWrapButton);

        JButton settingsButton = createToolbarButton("Settings", "Configure global application settings");
        settingsButton.addActionListener(e -> showSettingsDialog());
        toolbar.add(settingsButton);

        return toolbar;
    }

    private static JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        styleButton(button);

        // Add some padding for better appearance
        button.setMargin(new Insets(6, 12, 6, 12));

        return button;
    }

    private static void createTabComponent(String title, int index) {
        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabComponent.setOpaque(true);
        tabComponent.setBackground(isDarkMode ? new Color(60, 63, 65) : new Color(230, 230, 230));

        // Create gradient effect for active tab
        if (tabbedPane.getSelectedIndex() == index) {
            tabComponent.setBackground(isDarkMode ? new Color(80, 83, 85) : new Color(210, 210, 210));
        }

        // Set the background of the tab area
        tabbedPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        // Remove the content border
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        TabInfo tab = tabs.get(index);
        JLabel titleLabel = new JLabel(tab.tabName + " ");
        titleLabel.setOpaque(false);
        titleLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));

        // Adjust the border to eliminate any gaps
        tabComponent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0,
                        tabbedPane.getSelectedIndex() == index
                                ? (isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : new Color(0, 120, 215))
                                : tabComponent.getBackground()),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        // Add right-click menu for renaming
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)));

        JMenuItem renameItem = new JMenuItem("Rename Tab");
        JMenuItem closeItem = new JMenuItem("Close Tab");

        // Style menu items
        for (JMenuItem item : new JMenuItem[] { renameItem, closeItem }) {
            item.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
            item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
            item.setFont(item.getFont().deriveFont(12f)); // Consistent font size

            // Add hover effect
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    item.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                    item.setForeground(Color.WHITE);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    item.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
                    item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
                }
            });
        }

        renameItem.addActionListener(e -> showRenameDialog(index));
        closeItem.addActionListener(e -> closeTab(index));

        popupMenu.add(renameItem);
        popupMenu.add(closeItem);

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

            // Add hover effect
            public void mouseEntered(java.awt.event.MouseEvent e) {
                titleLabel.setForeground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : new Color(0, 120, 215));
                tabComponent.setBackground(isDarkMode ? new Color(70, 73, 75) : new Color(220, 220, 220));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                titleLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
                tabComponent.setBackground(isDarkMode ? new Color(60, 63, 65) : new Color(230, 230, 230));
                if (tabbedPane.getSelectedIndex() == index) {
                    tabComponent.setBackground(isDarkMode ? new Color(80, 83, 85) : new Color(210, 210, 210));
                }
            }
        });

        // Modern close button with X symbol
        JButton closeButton = new JButton("\u2715");
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(false);
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setFont(closeButton.getFont().deriveFont(10f)); // Smaller font for X
        closeButton.setForeground(isDarkMode ? new Color(180, 180, 180) : new Color(100, 100, 100));
        closeButton.setFocusable(false);
        closeButton.setToolTipText("Close this tab");
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        // Add hover effect for close button
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeButton.setForeground(new Color(240, 71, 71)); // Red color on hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeButton.setForeground(isDarkMode ? new Color(180, 180, 180) : new Color(100, 100, 100));
            }
        });

        closeButton.addActionListener(e -> closeTab(index));

        tabComponent.add(titleLabel);
        tabComponent.add(closeButton);

        tabbedPane.setTabComponentAt(index, tabComponent);

        // Add a change listener to update the selected tab appearance
        tabbedPane.addChangeListener(e -> {
            // Ensure the tab area maintains the correct background
            tabbedPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : LIGHT_MODE_BACKGROUND);

            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getTabComponentAt(i);
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    panel.setBackground(isDarkMode ? new Color(60, 63, 65) : new Color(230, 230, 230));
                    if (tabbedPane.getSelectedIndex() == i) {
                        panel.setBackground(isDarkMode ? new Color(80, 83, 85) : new Color(210, 210, 210));
                        panel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(2, 0, 0, 0,
                                        isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : new Color(0, 120, 215)),
                                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                    } else {
                        panel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(2, 0, 0, 0, panel.getBackground()),
                                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                    }
                }
            }
        });
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

        // Add clear log hotkey
        KeyStroke clearKey = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            TabInfo currentTab = getCurrentTab();
            if (currentTab != null) {
                clearLogFromLinux(currentTab);
            }
        }, clearKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Add connect hotkey
        KeyStroke connectKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            TabInfo currentTab = getCurrentTab();
            if (currentTab != null) {
                saveTabSettings(currentTab, hostField.getText(), userField.getText(),
                        new String(passwordField.getPassword()), logFileField.getText());
                try {
                    saveConfiguration();
                } catch (JSONException ex) {
                    JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
                }
            }
        }, connectKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        button.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);

        // Make button text bold for better visibility
        button.setFont(button.getFont().deriveFont(Font.BOLD));

        // Modern button styling with rounded corners
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (isDarkMode) {
                    button.setBackground(DARK_LIGHT_MODE_BUTTON_BACKGROUND.brighter());
                    button.setForeground(DARK_MODE_TEXT);
                } else {
                    button.setBackground(LIGHT_MODE_BUTTON_BACKGROUND.darker());
                    button.setForeground(LIGHT_MODE_BUTTON_TEXT);
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                button.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
            }
        });
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
        dialog.setMinimumSize(new Dimension(400, 300)); // Set minimum size

        // Apply current theme to dialog
        Container dialogContainer = dialog.getContentPane();
        dialogContainer.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        dialogContainer.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        optionsPanel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Font family dropdown
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        JComboBox<String> fontFamily = new JComboBox<>(fonts);
        fontFamily.setSelectedItem(currentTab.currentFont.getFamily());
        fontFamily.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        fontFamily.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        // Add label with proper styling
        JLabel fontLabel = new JLabel("Font:");
        fontLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        fontLabel.setFont(fontLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        optionsPanel.add(fontLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        optionsPanel.add(fontFamily, gbc);

        // Font size dropdown
        Integer[] sizes = { 8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40, 48, 56, 64, 72 };
        JComboBox<Integer> fontSize = new JComboBox<>(sizes);
        fontSize.setSelectedItem(currentTab.currentFont.getSize());
        fontSize.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        fontSize.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        addOptionField(optionsPanel, "Size:", fontSize, gbc, 1);

        // Font style dropdown
        String[] styles = { "Plain", "Bold", "Italic", "Bold Italic" };
        JComboBox<String> fontStyle = new JComboBox<>(styles);
        fontStyle.setSelectedIndex(currentTab.currentFont.getStyle());
        fontStyle.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        fontStyle.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        addOptionField(optionsPanel, "Style:", fontStyle, gbc, 2);

        // Create color buttons with proper styling
        JButton textColorBtn = new JButton("Choose Text Color");
        textColorBtn.setBorderPainted(false);
        textColorBtn.setOpaque(true);
        textColorBtn.setBackground(currentTab.textColor);
        textColorBtn.setForeground(getContrastColor(currentTab.textColor));
        textColorBtn.setFont(textColorBtn.getFont().deriveFont(Font.BOLD));
        textColorBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        textColorBtn.setFocusPainted(false);

        // Add label with proper styling
        JLabel textColorLabel = new JLabel("Text Color:");
        textColorLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        textColorLabel.setFont(textColorLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        optionsPanel.add(textColorLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        optionsPanel.add(textColorBtn, gbc);

        JButton bgColorBtn = new JButton("Choose Background Color");
        bgColorBtn.setBorderPainted(false);
        bgColorBtn.setOpaque(true);
        bgColorBtn.setBackground(currentTab.backgroundColor);
        bgColorBtn.setForeground(getContrastColor(currentTab.backgroundColor));
        bgColorBtn.setFont(bgColorBtn.getFont().deriveFont(Font.BOLD));
        bgColorBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        bgColorBtn.setFocusPainted(false);

        // Add label with proper styling
        JLabel bgColorLabel = new JLabel("Background:");
        bgColorLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        bgColorLabel.setFont(bgColorLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        optionsPanel.add(bgColorLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        optionsPanel.add(bgColorBtn, gbc);

        // Create preview with proper styling
        JTextArea preview = new JTextArea("Preview Text");
        preview.setFont(currentTab.currentFont);
        preview.setForeground(currentTab.textColor);
        preview.setBackground(currentTab.backgroundColor);
        preview.setPreferredSize(new Dimension(300, 100));
        preview.setMargin(new Insets(5, 5, 5, 5));

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(200, 200, 200)),
                "Preview",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                null,
                isDarkMode ? DARK_MODE_TEXT : Color.BLACK));
        previewPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        previewPanel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        JScrollPane previewScrollPane = new JScrollPane(preview);
        previewScrollPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        previewScrollPane.getViewport().setBackground(preview.getBackground());
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

        // Create buttons with proper styling
        JButton applyButton = new JButton("Apply");
        applyButton.setBorderPainted(false);
        applyButton.setOpaque(true);
        applyButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        applyButton.setForeground(Color.WHITE);
        applyButton.setFont(applyButton.getFont().deriveFont(Font.BOLD));
        applyButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(0, 90, 180)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        applyButton.setFocusPainted(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        cancelButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        cancelButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
        cancelButton.setFont(cancelButton.getFont().deriveFont(Font.BOLD));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        cancelButton.setFocusPainted(false);

        // Add hover effects
        applyButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                applyButton.setBackground(isDarkMode ? new Color(100, 170, 230) : new Color(30, 144, 255));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                applyButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }
        });

        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (isDarkMode) {
                    cancelButton.setBackground(new Color(100, 100, 100));
                    cancelButton.setForeground(new Color(255, 255, 255));
                } else {
                    cancelButton.setBackground(new Color(210, 210, 210));
                    cancelButton.setForeground(new Color(0, 0, 0));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                cancelButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
            }
        });

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
                textColorBtn.setForeground(getContrastColor(newColor));
            }
        });

        bgColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Background Color", currentTab.backgroundColor);
            if (newColor != null) {
                preview.setBackground(newColor);
                previewScrollPane.getViewport().setBackground(newColor);
                bgColorBtn.setBackground(newColor);
                bgColorBtn.setForeground(getContrastColor(newColor));
            }
        });

        applyButton.addActionListener(e -> {
            currentTab.currentFont = preview.getFont();
            currentTab.textColor = preview.getForeground();
            currentTab.backgroundColor = preview.getBackground();
            currentTab.textArea.setFont(currentTab.currentFont);
            currentTab.textArea.setForeground(currentTab.textColor);
            currentTab.textArea.setBackground(currentTab.backgroundColor);

            // Save the configuration after applying changes
            try {
                saveConfiguration();
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }

            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(optionsPanel, BorderLayout.NORTH);
        dialog.add(previewPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // Helper method to get a contrasting color for text based on background
    private static Color getContrastColor(Color background) {
        // Calculate the perceptive luminance (human eye favors green)
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue())
                / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
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
                content.append("Exported on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                        .append("\n");
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
        if (currentTab == null)
            return;

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
                            ((JButton) button)
                                    .setText(currentTab.filterDuplicates ? "Show Duplicates" : "Filter Duplicates");
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
        if (currentTab == null)
            return;

        boolean isWrapped = !currentTab.textArea.getLineWrap();
        currentTab.textArea.setLineWrap(isWrapped);
        currentTab.textArea.setWrapStyleWord(isWrapped);
        wordWrapButton.setText(isWrapped ? "Disable Wrap" : "Word Wrap");
    }

    private static void showSettingsDialog() {
        JDialog settingsDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(tabbedPane), "Settings", true);
        settingsDialog.setLayout(new BorderLayout());
        settingsDialog.setSize(500, 400);
        settingsDialog.setLocationRelativeTo(null);
        settingsDialog.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane settingsTabs = new JTabbedPane();
        settingsTabs.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        settingsTabs.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        settingsTabs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Connection Settings Panel
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Host settings
        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        hostLabel.setFont(hostLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(hostLabel, gbc);

        JTextField hostField = new JTextField(host, 20);
        hostField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        hostField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        hostField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        connectionPanel.add(hostField, gbc);

        // Port settings
        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        portLabel.setFont(portLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        connectionPanel.add(portLabel, gbc);

        JTextField portField = new JTextField(String.valueOf(port), 5);
        portField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        portField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        portField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        gbc.gridy = 1;
        connectionPanel.add(portField, gbc);

        // Username settings
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 2;
        connectionPanel.add(usernameLabel, gbc);

        JTextField usernameField = new JTextField(username, 20);
        usernameField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        usernameField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        connectionPanel.add(usernameField, gbc);

        // Password settings
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        passwordLabel.setFont(passwordLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        connectionPanel.add(passwordLabel, gbc);

        JPasswordField passwordField = new JPasswordField(password, 20);
        passwordField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        passwordField.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        connectionPanel.add(passwordField, gbc);

        // Add connection panel to tabs
        settingsTabs.addTab("Connection", connectionPanel);

        // Add tabs to main panel
        mainPanel.add(settingsTabs, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton saveButton = new JButton("Save");
        saveButton.setBorderPainted(false);
        saveButton.setOpaque(true);
        saveButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(0, 90, 180)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        saveButton.setFocusPainted(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        cancelButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
        cancelButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
        cancelButton.setFont(cancelButton.getFont().deriveFont(Font.BOLD));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(100, 100, 100) : new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        cancelButton.setFocusPainted(false);

        // Add hover effects
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                saveButton.setBackground(isDarkMode ? new Color(100, 170, 230) : new Color(30, 144, 255));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                saveButton.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
            }
        });

        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (isDarkMode) {
                    cancelButton.setBackground(new Color(100, 100, 100));
                    cancelButton.setForeground(new Color(255, 255, 255));
                } else {
                    cancelButton.setBackground(new Color(210, 210, 210));
                    cancelButton.setForeground(new Color(0, 0, 0));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton
                        .setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                cancelButton.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_BUTTON_TEXT);
            }
        });

        saveButton.addActionListener(e -> {
            host = hostField.getText();
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(settingsDialog, "Invalid port number", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            username = usernameField.getText();
            password = new String(passwordField.getPassword());

            try {
                saveConfiguration();
                settingsDialog.dispose();
            } catch (JSONException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(settingsDialog, "Error saving configuration: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> settingsDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        settingsDialog.add(mainPanel);
        settingsDialog.setVisible(true);
    }

}
