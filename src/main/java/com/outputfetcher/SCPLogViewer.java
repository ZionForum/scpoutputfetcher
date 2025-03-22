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
import java.awt.event.KeyListener;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
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

/**
 * SCP Log Viewer - A Java-based tool for remote log monitoring via SSH/SCP
 *
 * This application provides a GUI interface for viewing and monitoring log
 * files on remote servers.
 * It supports multiple tabs, each connecting to a different remote host and log
 * file.
 * Features include:
 * - SSH/SCP based secure remote file access
 * - Real-time log monitoring
 * - Multiple tab support for monitoring different logs
 * - Dark/Light theme modes
 * - Text search functionality
 * - Configurable auto-connect
 * - Duplicate line filtering
 * - Timestamp display toggle
 * - Word wrap support
 * - Fast scrolling
 * - Copy/paste and other text operations
 *
 * The application uses JSch for SSH connectivity and Swing for the GUI
 * interface.
 * Configuration is persisted between sessions.
 *
 * @author Ryu Saplad
 * @version 1.0
 */

public class SCPLogViewer {
    /** ANSI escape code to clear the entire log content */
    private static final String ESCAPE_CODE_CLEAR = "\u001B[C]";

    /** ANSI escape code to clear log content from cursor position to bottom */
    private static final String ESCAPE_CODE_CLEAR_BOTTOM = "\u001B[CB]";

    /** Main tabbed pane containing all log viewer tabs */
    private static JTabbedPane tabbedPane;

    /** List storing tab information and state for all open tabs */
    private static List<TabInfo> tabs = new ArrayList<>();

    /** Text field for entering search terms */
    private static JTextField searchField;

    /** Checkboxes for various log viewing options */
    private static JCheckBox duplicateFilterBox, wordWrapBox, showTimestampBox;

    /** Checkboxes for search options */
    private static JCheckBox caseSensitiveBox, wrapAroundCheckBox;

    /** Maximum number of tabs that can be opened */
    private static final int MAX_TABS = 100;

    /** Text fields for SSH connection details */
    private static JTextField hostField, userField, logFileField;
    private static JPasswordField passwordField;

    /** Number of lines to scroll when using fast scroll */
    private static final int FAST_SCROLL_LINES = 10;

    /** Color constants for dark/light themes */
    private static final Color DARK_MODE_BACKGROUND = new Color(43, 43, 43);
    private static final Color LIGHT_MODE_BACKGROUND = Color.WHITE;
    private static final Color DARK_MODE_TEXT = new Color(255, 255, 255);
    private static final Color LIGHT_MODE_BUTTON_BACKGROUND = new Color(230, 230, 230);
    private static final Color LIGHT_MODE_BUTTON_TEXT = Color.BLACK;
    private static final Color DARK_LIGHT_MODE_BUTTON_BACKGROUND = new Color(86, 156, 214);
    private static final Color LIGHT_MODE_TEXT = Color.BLACK;

    /** Flag indicating if dark mode is enabled */
    private static boolean isDarkMode = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = createAndShowGUI();
                setupWindowListener(frame);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates and displays the main application GUI window.
     * 
     * This method initializes the main JFrame window and sets up all the core UI
     * components including:
     * - Setting the system look and feel
     * - Creating the main tabbed pane for log viewing
     * - Adding the toolbar with controls
     * - Loading any saved tab configurations
     * - Applying theme settings
     * - Setting up keyboard shortcuts
     * - Handling auto-connect for saved tabs
     *
     * @return The configured and displayed main JFrame window
     * @throws JSONException If there is an error loading the saved configuration
     */
    private static JFrame createAndShowGUI() throws JSONException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (Exception e) {
            e.printStackTrace();
        }

        isDarkMode = Configuration.isDarkMode();

        JFrame frame = new JFrame("SCP Log Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(1200, 800));
        frame.setMinimumSize(new Dimension(800, 600));

        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        tabbedPane.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        tabPanel.add(tabbedPane, BorderLayout.CENTER);

        frame.add(tabPanel);

        JToolBar toolBar = createToolBar();
        frame.add(toolBar, BorderLayout.NORTH);

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
        handleAutoConnect();

        return frame;
    }

    /**
     * Adds a new tab with default configuration.
     *
     * This method creates a new tab using the default configuration and adds it to
     * the tabbed pane.
     * It also updates the UI fields with the default settings.
     *
     * @throws JSONException If there is an error loading the saved configuration
     */
    private static void addNewTab() throws JSONException {
        addNewTab(new Configuration.TabConfig());
    }

    /**
     * Adds a new tab with specified configuration.
     *
     * This method creates a new tab using the provided configuration and adds it to
     * the tabbed pane.
     * It also updates the UI fields with the saved settings.
     *
     * @param config The configuration to use for the new tab
     * @throws JSONException If there is an error loading the saved configuration
     */
    private static void addNewTab(Configuration.TabConfig config) throws JSONException {
        if (tabs.size() >= MAX_TABS) {
            JOptionPane.showMessageDialog(null, "Maximum number of tabs reached!");
            return;
        }

        TabInfo tab = new TabInfo();
        tab.host = config.host;
        tab.user = config.user;
        tab.port = config.port;
        tab.password = config.password;
        tab.logFile = config.logFile;
        tab.textColor = config.textColor;
        tab.backgroundColor = config.backgroundColor;
        tab.currentFont = config.font;
        tab.filterDuplicates = config.filterDuplicates;
        tab.wordWrap = config.wordWrap;
        tab.showTimestamp = config.showTimestamp;
        tab.autoConnect = config.autoConnect;
        tab.tabName = config.tabName != null ? config.tabName : "Log " + (tabs.size() + 1);
        tab.textArea.setFont(tab.currentFont);
        tab.textArea.setForeground(tab.textColor);
        tab.textArea.setBackground(tab.backgroundColor);
        tab.textArea.setLineWrap(tab.wordWrap);
        tab.textArea.setWrapStyleWord(tab.wordWrap);

        tabs.add(tab);

        JPanel tabContent = createTabContent(tab);
        int index = tabbedPane.getTabCount();
        tabbedPane.addTab(tab.tabName, tabContent);
        createTabComponent(tab.tabName, index);
        tabbedPane.setSelectedIndex(index);

        if (hostField != null)
            hostField.setText(tab.host);
        if (userField != null)
            userField.setText(tab.user);
        if (passwordField != null)
            passwordField.setText(tab.password);
        if (logFileField != null)
            logFileField.setText(tab.logFile);

        saveConfiguration();

        if (tab.autoConnect) {
            SwingUtilities.invokeLater(() -> {
                startFileMonitoring(tab);
            });
        }
    }

    /**
     * Toggles the theme between dark and light modes.
     *
     * This method toggles the theme between dark and light modes and updates the UI
     * accordingly.
     * It also saves the new theme state to the configuration file.
     *
     * @throws JSONException If there is an error saving the theme configuration
     */
    private static void toggleTheme() throws JSONException {
        isDarkMode = !isDarkMode;

        Window window = SwingUtilities.getWindowAncestor(tabbedPane);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;

            tabbedPane.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
            tabbedPane.getParent().setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) comp;
                    toolbar.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0,
                                    isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                    toolbar.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));

                    for (Component button : toolbar.getComponents()) {
                        if (button instanceof JButton) {
                            JButton btn = (JButton) button;
                            btn.setBorderPainted(false);
                            btn.setOpaque(true);
                            if (btn.getText().contains("Theme")) {
                                btn.setText(isDarkMode ? "Light Theme" : "Dark Theme");
                                btn.setToolTipText("Switch to " + (isDarkMode ? "light" : "dark") + " theme");
                            }
                            styleButton(btn);
                        }
                    }
                    break;
                }
            }

            applyTheme(frame.getContentPane(), isDarkMode);
            updateButtonStyles();

            for (int i = 0; i < tabs.size(); i++) {
                createTabComponent(tabs.get(i).tabName, i);
            }

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

    /**
     * Updates the theme of a given panel.
     *
     * This method updates the background and foreground colors of the given panel
     * to match the current theme.
     * It also recursively updates the theme of all nested components.
     *
     * @param panel The panel to update the theme for
     */
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
                }
            }
        }
    }

    /**
     * Applies the theme to a given container.
     *
     * This method updates the background and foreground colors of the given
     * container
     * to match the current theme.
     * It also recursively updates the theme of all nested components.
     *
     * @param container The container to update the theme for
     * @param dark      Whether the dark mode is enabled
     */
    private static void applyTheme(Container container, boolean dark) {
        Color bg = dark ? DARK_MODE_BACKGROUND : Color.WHITE;
        Color fg = dark ? DARK_MODE_TEXT : Color.BLACK;
        Color inputBg = dark ? new Color(60, 60, 60) : Color.WHITE;

        container.setBackground(bg);
        container.setForeground(fg);

        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextArea) {
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

    /**
     * Retrieves the tab associated with a given text area.
     *
     * This method iterates through all tabs and returns the one whose text area
     * matches the given text area.
     *
     * @param textArea The text area to find the associated tab for
     * @return The tab associated with the given text area, or null if no match is
     *         found
     */
    private static TabInfo getTabForTextArea(JTextArea textArea) {
        for (TabInfo tab : tabs) {
            if (tab.textArea == textArea) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Updates the styles of all buttons in the container.
     *
     * This method iterates through all components in the given container and
     * updates the styles of any buttons.
     * It also recursively updates the styles of all nested components.
     *
     * @param container The container to update the button styles for
     */
    private static void updateButtonStyles() {
        Window window = SwingUtilities.getWindowAncestor(tabbedPane);
        if (window instanceof JFrame) {
            Container contentPane = ((JFrame) window).getContentPane();
            updateButtonStylesInContainer(contentPane);
        }
    }

    /**
     * Updates the styles of all buttons in the given container.
     *
     * This method iterates through all components in the given container and
     * updates the styles of any buttons.
     * It also recursively updates the styles of all nested components.
     *
     * @param container The container to update the button styles for
     */
    private static void updateButtonStylesInContainer(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                styleButton((JButton) comp);
            } else if (comp instanceof Container) {
                updateButtonStylesInContainer((Container) comp);
            }
        }
    }

    /**
     * Creates the content panel for a new tab.
     *
     * This method creates a new JPanel with a BorderLayout and adds a settings
     * panel
     * and a status panel to it.
     * It also sets up the text area with the appropriate theme and mouse wheel
     * listener for fast scrolling.
     *
     * @param tabInfo The tab information for the new tab
     * @return The content panel for the new tab
     */
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

        // Add tooltip to show fast scrolling information
        scrollPane.setToolTipText("Alt+Scroll or Alt+Arrow keys for fast scrolling");

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

    /**
     * Sets up the text area for a given tab.
     *
     * This method configures the text area with the appropriate theme and
     * mouse wheel listener for fast scrolling.
     *
     * @param tabInfo The tab information for the tab to configure
     */
    private static void setupTextArea(TabInfo tabInfo) {
        tabInfo.textArea.setEditable(false);
        tabInfo.textArea.setFont(tabInfo.currentFont);
        tabInfo.textArea.setBackground(tabInfo.backgroundColor);
        tabInfo.textArea.setForeground(tabInfo.textColor);
        tabInfo.textArea.setMargin(new Insets(8, 8, 8, 8));

        // Add mouse wheel listener for both regular and fast scrolling
        tabInfo.textArea.addMouseWheelListener(e -> {
            Container parent = tabInfo.textArea.getParent();
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent();
            }

            if (parent instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) parent;
                JScrollBar vbar = scrollPane.getVerticalScrollBar();

                if (e.isAltDown()) {
                    int scrollAmount = e.getWheelRotation() * FAST_SCROLL_LINES;
                    vbar.setValue(vbar.getValue() + (scrollAmount * vbar.getUnitIncrement()));
                } else {
                    int scrollAmount = e.getWheelRotation() * vbar.getUnitIncrement();
                    vbar.setValue(vbar.getValue() + scrollAmount);
                }
                e.consume();
            }
        });

        // Add key listener for Alt+Arrow fast scrolling
        tabInfo.textArea.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isAltDown()) {
                    Container parent = tabInfo.textArea.getParent();
                    while (parent != null && !(parent instanceof JScrollPane)) {
                        parent = parent.getParent();
                    }

                    if (parent instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) parent;
                        JScrollBar vbar = scrollPane.getVerticalScrollBar();
                        int direction = 0;

                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            direction = -1;
                        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            direction = 1;
                        }

                        if (direction != 0) {
                            vbar.setValue(vbar.getValue() + (direction * FAST_SCROLL_LINES * vbar.getUnitIncrement()));
                            e.consume();
                        }
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        // Add right-click menu
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200), 2));

        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem selectAllItem = new JMenuItem("Select All");
        JMenuItem clearItem = new JMenuItem("Clear Log");
        JMenuItem findItem = new JMenuItem("Find...");

        copyItem.addActionListener(e -> tabInfo.textArea.copy());
        selectAllItem.addActionListener(e -> tabInfo.textArea.selectAll());
        clearItem.addActionListener(e -> clearLogFromLinux(tabInfo));
        findItem.addActionListener(e -> showSearchDialog());

        // Apply styling to all menu items
        for (JMenuItem menuItem : new JMenuItem[] { copyItem, selectAllItem, clearItem, findItem }) {
            menuItem.setBorderPainted(false);
            menuItem.setOpaque(true);
            menuItem.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
            menuItem.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
            menuItem.setFont(menuItem.getFont().deriveFont(12f));

            menuItem.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    menuItem.setBackground(
                            isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                    menuItem.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.WHITE);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    menuItem.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
                    menuItem.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
                }
            });
        }
        // Add keyboard shortcuts
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));

        JSeparator separator = new JSeparator();
        separator.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : DARK_LIGHT_MODE_BUTTON_BACKGROUND);
        JSeparator separator2 = new JSeparator();
        separator2.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : DARK_LIGHT_MODE_BUTTON_BACKGROUND);
        popup.add(copyItem);
        popup.add(selectAllItem);
        popup.add(separator);
        popup.add(findItem);
        popup.add(separator2);
        popup.add(clearItem);

        tabInfo.textArea.setComponentPopupMenu(popup);
    }

    /**
     * Creates a settings panel for configuring SSH connection and log file
     * monitoring.
     * 
     * @param tabInfo The TabInfo object containing the tab's configuration settings
     * @return A JPanel containing the settings UI components including:
     *         - Host field for SSH server address
     *         - Username field for SSH authentication
     *         - Password field for SSH authentication
     *         - Log file path field
     *         - Connect button to establish connection
     *         - Auto-connect checkbox to enable automatic monitoring
     */
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
        gbc.insets = new Insets(10, 5, 5, 5);
        settingsPanel.add(connectBtn, gbc);

        // Store references
        tabInfo.host = hostField.getText();
        tabInfo.user = userField.getText();
        tabInfo.password = new String(passwordField.getPassword());
        tabInfo.logFile = logFileField.getText();

        // Add auto-connect checkbox
        JCheckBox autoConnectBox = new JCheckBox("Auto Connect");
        autoConnectBox.setSelected(tabInfo.autoConnect);
        autoConnectBox.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));
        autoConnectBox.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        settingsPanel.add(autoConnectBox, gbc);

        // Update connect button action to handle auto-connect
        ActionListener connectAction = e -> {
            tabInfo.autoConnect = autoConnectBox.isSelected();
            saveTabSettings(tabInfo, hostField.getText(),
                    userField.getText(), new String(passwordField.getPassword()), logFileField.getText());

            // Start or stop monitoring based on auto-connect state
            if (tabInfo.autoConnect) {
                if (tabInfo.monitoringThread != null) {
                    tabInfo.monitoringThread.interrupt();
                }
                startFileMonitoring(tabInfo);
            } else {
                if (tabInfo.monitoringThread != null) {
                    tabInfo.monitoringThread.interrupt();
                    tabInfo.monitoringThread = null;
                }
            }

            try {
                saveConfiguration();
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }
        };

        // Add listeners for both button and checkbox
        connectBtn.addActionListener(connectAction);
        autoConnectBox.addActionListener(e -> {
            tabInfo.autoConnect = autoConnectBox.isSelected();
            try {
                saveConfiguration();
                if (tabInfo.autoConnect) {
                    if (tabInfo.monitoringThread != null) {
                        tabInfo.monitoringThread.interrupt();
                    }
                    startFileMonitoring(tabInfo);
                } else {
                    if (tabInfo.monitoringThread != null) {
                        tabInfo.monitoringThread.interrupt();
                        tabInfo.monitoringThread = null;
                    }
                }
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
            }
        });

        return settingsPanel;
    }

    /**
     * Adds a labeled field to a settings panel using GridBagLayout.
     *
     * @param panel The panel to add the field to
     * @param label The label text for the field
     * @param field The component to add as the field
     * @param gbc   The GridBagConstraints to use for layout
     * @param row   The row number in the grid to place the field
     */
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

    /**
     * Creates a panel containing refresh and clear buttons for a log tab.
     * 
     * This method creates and configures a button panel with refresh and clear
     * buttons
     * that match the current theme. The buttons include hover effects and proper
     * styling.
     * The refresh button triggers a log refresh and the clear button clears the log
     * from Linux.
     *
     * @param tabInfo The tab information object containing the tab's state and
     *                configuration
     * @return A JPanel containing the styled refresh and clear buttons
     */
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

    /**
     * Displays a search dialog for finding text in the current tab's text area.
     * 
     * This method creates and shows a modal dialog with search functionality
     * including:
     * - Text input field for search term
     * - Case sensitive matching option
     * - Wrap around search option
     * - Search direction selection (up/down)
     * - Find and Cancel buttons
     * 
     * The dialog is themed according to the current dark/light mode setting.
     * Search can be triggered by clicking Find or pressing Enter.
     * The dialog includes hover effects on buttons and proper styling of all
     * components.
     * 
     * If no tab is currently open, the method returns without showing the dialog.
     */
    private static void showSearchDialog() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null) {
            return; // No tab to search in
        }
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
        downButton.setSelected(true);

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

    /**
     * Performs a search for the next or previous occurrence of text in the current
     * tab.
     * 
     * This method searches for the text entered in the search field either forward
     * or backward
     * from the current caret position, depending on the search direction specified.
     * The search is performed asynchronously using a SwingWorker to prevent UI
     * freezing.
     * 
     * Features:
     * - Case sensitive/insensitive search based on checkbox selection
     * - Wrap around search if enabled
     * - Handles text selection when searching up/down
     * - Shows message dialog if text is not found
     * - Highlights found text by selecting it
     * 
     * @param searchUp true to search upward (backward), false to search downward
     *                 (forward)
     */
    private static void findNextOrPrevious(boolean searchUp) {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null)
            return;

        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty())
            return;

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

    /**
     * Gets the currently selected tab's TabInfo object.
     * 
     * @return The TabInfo object for the currently selected tab, or null if no tab
     *         is selected
     */
    private static TabInfo getCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
    }

    /**
     * Closes the tab at the specified index.
     * 
     * This method:
     * - Stops any monitoring thread for the tab
     * - Removes the tab from the tabs list and UI
     * - Updates remaining tab titles/names
     * - Saves the updated configuration
     *
     * @param index The index of the tab to close
     */
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

    /**
     * Saves the current configuration state to persistent storage.
     * 
     * This method saves:
     * - Theme settings (dark/light mode)
     * - All tab configurations including:
     * - Connection details (host, user, password, port)
     * - Visual settings (colors, fonts)
     * - Behavior settings (filtering, word wrap, timestamps)
     *
     * @throws JSONException If there is an error saving the configuration
     */
    private static void saveConfiguration() throws JSONException {
        List<Configuration.TabConfig> tabConfigs = new ArrayList<>();
        for (TabInfo tab : tabs) {
            Configuration.TabConfig config = new Configuration.TabConfig();
            config.host = tab.host;
            config.user = tab.user;
            config.password = tab.password;
            config.port = tab.port;
            config.logFile = tab.logFile;
            config.tabName = tab.tabName;
            config.textColor = tab.textColor;
            config.backgroundColor = tab.backgroundColor;
            config.font = tab.currentFont;
            config.filterDuplicates = tab.filterDuplicates;
            config.wordWrap = tab.wordWrap;
            config.showTimestamp = tab.showTimestamp;
            config.autoConnect = tab.autoConnect;
            tabConfigs.add(config);
        }
        Configuration.saveConfig(isDarkMode, tabConfigs);
    }

    /**
     * Saves connection settings for a tab and restarts monitoring if needed.
     * 
     * This method:
     * - Updates the tab's connection settings
     * - Updates corresponding UI fields
     * - Saves configuration to persistent storage
     * - Restarts monitoring with new settings if previously active
     * - Shows confirmation dialog
     * - Starts monitoring if auto-connect is enabled
     *
     * @param tabInfo  The TabInfo object to update
     * @param host     The SSH host address
     * @param user     The SSH username
     * @param password The SSH password
     * @param logFile  The remote log file path to monitor
     */
    private static void saveTabSettings(TabInfo tabInfo, String host, String user, String password, String logFile) {
        tabInfo.host = host;
        tabInfo.user = user;
        tabInfo.password = password;
        tabInfo.logFile = logFile;
        tabInfo.port = 22;

        // Update the UI fields
        if (hostField != null)
            hostField.setText(host);
        if (userField != null)
            userField.setText(user);
        if (passwordField != null)
            passwordField.setText(password);
        if (logFileField != null)
            logFileField.setText(logFile);

        // Save configuration immediately
        try {
            saveConfiguration();
        } catch (JSONException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving configuration: " + ex.getMessage());
        }

        // Restart monitoring with new settings
        if (tabInfo.monitoringThread != null) {
            tabInfo.monitoringThread.interrupt();
        }

        startFileMonitoring(tabInfo);

        JOptionPane.showMessageDialog(null, "Settings saved successfully!");

        // Start monitoring if auto-connect is enabled
        if (tabInfo.autoConnect) {
            startFileMonitoring(tabInfo);
        }
    }

    /**
     * Updates the connection status and message for a tab.
     * 
     * This method updates the status label with the connection state and optional
     * message.
     * It also updates the status color (green for connected, red for disconnected)
     * and
     * triggers an update of the last update timestamp.
     *
     * @param tabInfo   The tab information to update
     * @param connected Whether the tab is currently connected
     * @param message   Optional status message to display
     */
    private static void updateStatus(TabInfo tabInfo, boolean connected, String message) {
        SwingUtilities.invokeLater(() -> {
            tabInfo.statusLabel.setText("Status: " + (connected ? "Connected" : "Disconnected")
                    + (message.isEmpty() ? "" : " - " + message));
            tabInfo.statusLabel.setForeground(connected ? new Color(0, 150, 0) : Color.RED);
            tabInfo.isConnected.set(connected);
            updateLastUpdateTime(tabInfo);
        });
    }

    /**
     * Updates the last update timestamp for a tab.
     * 
     * This method updates the last update label with the current date and time
     * formatted as "yyyy-MM-dd HH:mm:ss".
     *
     * @param tabInfo The tab information to update
     */
    private static void updateLastUpdateTime(TabInfo tabInfo) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tabInfo.lastUpdateLabel.setText("Last Update: " + sdf.format(new Date()));
    }

    /**
     * Appends log content to a tab's text area with optional filtering.
     * 
     * This method processes and appends log content to the text area, handling:
     * - Special escape codes for clearing logs
     * - Optional duplicate line filtering
     * - Optional timestamp prefixing
     * - Raw log content storage
     * - Auto-scrolling to latest content
     *
     * The content is processed on the Event Dispatch Thread to ensure thread
     * safety.
     * JNI_OnLoad messages are filtered out.
     *
     * @param tabInfo        The tab information to append logs to
     * @param initialContent The raw log content to process and append
     * @param addToRaw       Whether to store the content in raw log storage
     */
    private static void appendToLogWithFiltering(TabInfo tabInfo, final String initialContent, boolean addToRaw) {
        SwingUtilities.invokeLater(() -> {
            if (!initialContent.contains("JNI_OnLoad called")) {
                // Create a mutable copy of the content
                String content = initialContent;

                // Handle ESCAPE_CODE_CLEAR - clear all logs
                if (content.contains(ESCAPE_CODE_CLEAR)) {
                    tabInfo.textArea.setText("");
                    tabInfo.seenLogContents.clear();
                    if (tabInfo.rawLogContent != null) {
                        tabInfo.rawLogContent = new StringBuilder();
                    }
                    // Remove the escape code from content
                    content = content.replace(ESCAPE_CODE_CLEAR, "");
                }

                // Handle ESCAPE_CODE_CLEAR_BOTTOM - clear logs from current position to end
                if (content.contains(ESCAPE_CODE_CLEAR_BOTTOM)) {
                    int caretPosition = tabInfo.textArea.getCaretPosition();
                    String topContent = tabInfo.textArea.getText().substring(0, caretPosition);
                    tabInfo.textArea.setText(topContent);

                    // Also update raw content if it exists
                    if (tabInfo.rawLogContent != null) {
                        tabInfo.rawLogContent = new StringBuilder(topContent);
                    }

                    // Remove the escape code from content
                    content = content.replace(ESCAPE_CODE_CLEAR_BOTTOM, "");
                }

                final String processedContent = content;
                boolean shouldAppend = true;

                if (tabInfo.filterDuplicates) {
                    // Extract the actual log content without timestamp
                    String logContent = processedContent;
                    if (processedContent.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
                        logContent = processedContent.substring(processedContent.indexOf("]") + 1).trim();
                    }

                    // Check if we've seen this content before
                    if (tabInfo.seenLogContents.contains(logContent)) {
                        shouldAppend = false;
                    } else {
                        tabInfo.seenLogContents.add(logContent);
                    }
                }

                // Continue with existing append logic if content should be appended
                if (shouldAppend && !processedContent.trim().isEmpty()) {
                    String formattedContent;
                    // Extract the actual content without timestamp if it exists
                    String actualContent;
                    if (processedContent.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
                        actualContent = processedContent.substring(processedContent.indexOf("]") + 1).trim();
                    } else {
                        actualContent = processedContent.trim();
                    }

                    // Add timestamp only if showTimestamp is true
                    if (tabInfo.showTimestamp) {
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        formattedContent = String.format("[%s] %s\n", timestamp, actualContent);
                    } else {
                        formattedContent = actualContent + "\n";
                    }

                    tabInfo.textArea.append(formattedContent);

                    if (addToRaw) {
                        if (tabInfo.rawLogContent == null) {
                            tabInfo.rawLogContent = new StringBuilder();
                        }
                        // Store the raw content with timestamp for future toggling
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        String rawContent = String.format("[%s] %s\n", timestamp, actualContent);
                        tabInfo.rawLogContent.append(rawContent);
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

    /**
     * Fetches and processes log file content from a remote server via SSH.
     * 
     * This method connects to a remote server using SSH, checks if the specified
     * log file exists,
     * gets its current size, and reads any new content since the last check. The
     * content is read
     * using base64 encoding to preserve special characters and is then decoded and
     * filtered before
     * being appended to the text area.
     * 
     * The method performs the following steps:
     * 1. Establishes SSH connection using JSch
     * 2. Verifies log file existence
     * 3. Gets current file size
     * 4. Reads new content (either full file or only new bytes)
     * 5. Decodes base64 content and processes line by line
     * 6. Updates the UI with new content and status
     *
     * @param tabInfo The TabInfo object containing connection details and log file
     *                information
     *                including host, user, password, log file path, and UI
     *                components
     */
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
                        appendToLogWithFiltering(tabInfo, logLine, true);
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

    /**
     * Refreshes the log content for the given tab in a background thread.
     * 
     * This method creates a SwingWorker to fetch the log file content
     * asynchronously,
     * preventing UI freezes during the fetch operation.
     *
     * @param tabInfo The TabInfo object containing the tab's configuration and
     *                content
     */
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

    /**
     * Starts monitoring a log file for changes in a background thread.
     * 
     * This method creates a daemon thread that periodically checks for new content
     * in the specified log file. If a monitoring thread already exists for the tab,
     * it is interrupted before starting a new one.
     * 
     * The monitoring thread:
     * - Runs every 2 seconds
     * - Fetches new content if available
     * - Continues until interrupted
     * - Is marked as a daemon thread to not prevent JVM shutdown
     *
     * @param tabInfo The TabInfo object containing the tab's configuration and
     *                content
     */
    private static void startFileMonitoring(TabInfo tabInfo) {
        // Stop existing monitoring thread if it exists
        if (tabInfo.monitoringThread != null) {
            tabInfo.monitoringThread.interrupt();
        }

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

    /**
     * Creates and configures the main toolbar for the application.
     * 
     * This method builds a non-floating toolbar with the following features:
     * - File menu (New Tab, Export Log)
     * - View menu (Theme toggle, Text options)
     * - Filter options (Duplicate filtering, Word wrap, Timestamp display)
     * - Settings menu (Settings dialog, Reset defaults)
     * 
     * The toolbar is styled according to the current theme (dark/light mode) and
     * includes:
     * - Custom button styling with hover effects
     * - Proper spacing and separators between groups
     * - Checkbox states that sync with the current tab
     * - Menu items with theme-appropriate colors
     *
     * @return JToolBar The configured toolbar component
     */
    private static JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        toolbar.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : new Color(245, 245, 245));

        // File Group
        JPopupMenu fileMenu = new JPopupMenu();
        fileMenu.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

        JMenuItem newTabItem = createMenuItem("New Tab", e -> {
            try {
                addNewTab();
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        });
        JMenuItem exportItem = createMenuItem("Export Log", e -> exportLog());

        fileMenu.add(newTabItem);
        fileMenu.add(exportItem);

        JButton fileButton = createToolbarButton("File", "File operations");
        fileButton.addActionListener(e -> fileMenu.show(fileButton, 0, fileButton.getHeight()));
        toolbar.add(fileButton);

        toolbar.addSeparator();

        // View Group
        JPopupMenu viewMenu = new JPopupMenu();
        viewMenu.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

        JMenuItem themeItem = createMenuItem(isDarkMode ? "Light Theme" : "Dark Theme", null);
        themeItem.addActionListener(e -> {
            try {
                toggleTheme();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            themeItem.setText(isDarkMode ? "Light Theme" : "Dark Theme");
        });

        JMenuItem textOptionsItem = createMenuItem("Text Options", e -> showTextOptionsDialog());

        viewMenu.add(themeItem);
        viewMenu.add(textOptionsItem);

        JButton viewButton = createToolbarButton("View", "View options");
        viewButton.addActionListener(e -> viewMenu.show(viewButton, 0, viewButton.getHeight()));
        toolbar.add(viewButton);

        toolbar.addSeparator();
        // Filter Group
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.setBackground(toolbar.getBackground());

        duplicateFilterBox = new JCheckBox("Filter Duplicates");
        duplicateFilterBox.setBackground(toolbar.getBackground());
        duplicateFilterBox.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        duplicateFilterBox.addActionListener(e -> toggleDuplicateFiltering());

        wordWrapBox = new JCheckBox("Word Wrap");
        wordWrapBox.setBackground(toolbar.getBackground());
        wordWrapBox.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        wordWrapBox.addActionListener(e -> toggleWordWrap(null));

        showTimestampBox = new JCheckBox("Show Timestamp");
        showTimestampBox.setBackground(toolbar.getBackground());
        showTimestampBox.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        showTimestampBox.setSelected(true); // Default to showing timestamps
        showTimestampBox.addActionListener(e -> toggleTimestamp());

        // Set initial states based on current tab
        TabInfo currentTab = getCurrentTab();
        if (currentTab != null) {
            duplicateFilterBox.setSelected(currentTab.filterDuplicates);
            wordWrapBox.setSelected(currentTab.wordWrap);
            showTimestampBox.setSelected(currentTab.showTimestamp);
        }

        // Update tab change listener
        tabbedPane.addChangeListener(e -> {
            TabInfo selectedTab = getCurrentTab();
            if (selectedTab != null) {
                duplicateFilterBox.setSelected(selectedTab.filterDuplicates);
                wordWrapBox.setSelected(selectedTab.wordWrap);
                showTimestampBox.setSelected(selectedTab.showTimestamp);
            }
        });

        filterPanel.add(duplicateFilterBox);
        filterPanel.add(wordWrapBox);
        filterPanel.add(showTimestampBox);

        toolbar.add(filterPanel);

        toolbar.addSeparator();

        // Settings Group
        JPopupMenu settingsMenu = new JPopupMenu();
        settingsMenu.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);

        JMenuItem settingsItem = createMenuItem("Settings", e -> showSettingsDialog());
        JMenuItem resetItem = createMenuItem("Reset Defaults", e -> resetToDefault());

        settingsMenu.add(settingsItem);
        settingsMenu.addSeparator();
        settingsMenu.add(resetItem);

        JButton settingsButton = createToolbarButton("Settings", "Application settings");
        settingsButton.addActionListener(e -> settingsMenu.show(settingsButton, 0, settingsButton.getHeight()));
        toolbar.add(settingsButton);

        return toolbar;
    }

    /**
     * Creates a styled menu item with consistent theming and hover effects.
     *
     * This method creates a JMenuItem with the following styling:
     * - No border painting
     * - Opaque background
     * - Theme-appropriate colors for dark/light mode
     * - Custom font size (12pt)
     * - Hover effects that change background/foreground colors
     * - Attached action listener
     *
     * @param text     The text to display on the menu item
     * @param listener The ActionListener to handle clicks
     * @return A styled JMenuItem
     */
    private static JMenuItem createMenuItem(String text, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setBorderPainted(false);
        item.setOpaque(true);
        item.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
        item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        item.setFont(item.getFont().deriveFont(12f));

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                item.setBackground(isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : LIGHT_MODE_BUTTON_BACKGROUND);
                item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.WHITE);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                item.setBackground(isDarkMode ? DARK_MODE_BACKGROUND : Color.WHITE);
                item.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
            }
        });

        item.addActionListener(listener);
        return item;
    }

    /**
     * Applies consistent styling to a JButton including theme colors and hover
     * effects.
     *
     * This method styles a button with:
     * - No focus or border painting
     * - Theme-appropriate colors for dark/light mode
     * - Bold text
     * - Rounded corners with border
     * - Hover effects that adjust colors
     * - Proper padding
     *
     * @param button The JButton to style
     */
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

    /**
     * Adds a labeled field to an options panel using GridBagLayout.
     *
     * This method adds a label and corresponding input field to a panel with:
     * - Proper theme colors
     * - GridBag constraints for layout
     * - Label in first column, field in second column
     * - Field stretches to fill available space
     *
     * @param optionsPanel The panel to add components to
     * @param label        The label text
     * @param field        The input component to add
     * @param gbc          The GridBagConstraints to use
     * @param row          The row number to place components in
     */
    private static void addOptionField(JPanel optionsPanel, String label, JComponent field,
            GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_TEXT);
        optionsPanel.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        optionsPanel.add(field, gbc);
    }

    /**
     * Displays a dialog for customizing text appearance options for the current
     * tab.
     * 
     * This dialog allows users to modify:
     * - Font family (from system available fonts)
     * - Font size (predefined sizes from 8 to 72)
     * - Font style (Plain, Bold, Italic, Bold Italic)
     * - Text color
     * - Background color
     * 
     * Features:
     * - Live preview of changes
     * - Theme-aware styling (adapts to dark/light mode)
     * - Minimum dialog size enforcement
     * - Hover effects on buttons
     * - Color contrast checking for button text
     * - Configuration persistence
     * 
     * The dialog includes:
     * - Font selection controls
     * - Color picker buttons
     * - Preview panel showing live changes
     * - Apply/Cancel buttons
     * 
     * Changes are applied to the current tab and saved to configuration when
     * the Apply button is clicked. The dialog is modal and closes on Apply or
     * Cancel.
     * 
     * If no tab is open or the current tab has no font set, the method returns
     * without showing the dialog.
     */
    private static void showTextOptionsDialog() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null || currentTab.currentFont == null)
            return;

        JDialog dialog = new JDialog((Frame) null, "Text Options", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 350);
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

        // Add label with proper styling
        JLabel fontLabel = new JLabel("Font:");
        fontLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_TEXT);
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
        addOptionField(optionsPanel, "Size:", fontSize, gbc, 1);

        // Font style dropdown
        String[] styles = { "Plain", "Bold", "Italic", "Bold Italic" };
        JComboBox<String> fontStyle = new JComboBox<>(styles);
        fontStyle.setSelectedIndex(currentTab.currentFont.getStyle());
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
        textColorLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_TEXT);
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
        bgColorLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : LIGHT_MODE_TEXT);
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

    /**
     * Calculates and returns a contrasting color (black or white) based on
     * background color brightness.
     * 
     * Uses the luminance formula to determine the relative brightness of the
     * background color:
     * Luminance = (0.299 * Red + 0.587 * Green + 0.114 * Blue) / 255
     * Returns black for light backgrounds (luminance > 0.5) and white for dark
     * backgrounds.
     *
     * @param background The background Color to calculate contrast against
     * @return Color.BLACK for light backgrounds, Color.WHITE for dark backgrounds
     */
    private static Color getContrastColor(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue())
                / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Updates the preview text area with the selected font settings.
     * 
     * Creates a new Font object using the selected family, style and size from the
     * combo boxes
     * and applies it to the preview text area. Font styles are mapped as:
     * - Index 0: Plain
     * - Index 1: Bold
     * - Index 2: Italic
     * - Index 3: Bold Italic
     *
     * @param preview    The JTextArea to update with new font settings
     * @param fontFamily ComboBox containing font family selection
     * @param fontSize   ComboBox containing font size selection
     * @param fontStyle  ComboBox containing font style selection
     */
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

    /**
     * Resets all application settings to their default values after user
     * confirmation.
     * 
     * Default values:
     * - Dark mode: disabled
     * - Font: Monospaced, Plain, 12pt
     * - Text color: Black
     * - Background color: White
     * 
     * The method:
     * 1. Shows a confirmation dialog
     * 2. If confirmed:
     * - Disables dark mode
     * - Resets theme on main window
     * - Resets font/colors for all tabs
     * - Updates UI components
     * - Saves new configuration
     * 
     * Any configuration save errors are displayed to the user.
     */
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

    /**
     * Clears the log file on the remote Linux machine and resets the tab's content.
     * 
     * This method:
     * 1. Establishes SSH connection to remote host
     * 2. Executes truncate command to clear log file
     * 3. Resets tab content and tracking variables:
     * - Clears text area
     * - Resets raw content buffer
     * - Resets last modified timestamp
     * - Clears seen log entries cache
     * 4. Updates status to indicate completion
     *
     * @param tabInfo The TabInfo object containing connection details and content
     */
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
            // Clear the seen contents when logs are cleared
            tabInfo.seenLogContents.clear();

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

    /**
     * Exports the current tab's log content to a text file.
     * 
     * This method provides a file save dialog with the following features:
     * - Default save location in user's Documents folder
     * - Auto-generated filename with timestamp
     * - .txt extension enforcement
     * - Overwrite confirmation
     * - File content includes:
     * - Export metadata (timestamp, host, log file path)
     * - Complete log content (raw if available, otherwise displayed content)
     * 
     * The export process:
     * 1. Validates current tab selection
     * 2. Shows file chooser dialog
     * 3. Handles file selection and extension
     * 4. Confirms file overwrite if needed
     * 5. Writes content with metadata header
     * 6. Shows success/error message
     * 
     * If no tab is selected or export fails, appropriate error messages are shown.
     */
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

    /**
     * Toggles duplicate line filtering for the current tab.
     * 
     * When enabled, filters out duplicate log lines. When disabled, shows all
     * lines.
     * Reprocesses existing content and updates UI state to reflect the change.
     * Saves the configuration after toggling.
     */
    private static void toggleDuplicateFiltering() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null)
            return;

        currentTab.filterDuplicates = !currentTab.filterDuplicates;

        // Clear the seen contents when disabling filtering
        if (!currentTab.filterDuplicates) {
            currentTab.seenLogContents.clear();
        }

        // Only reprocess if there's content
        if (currentTab.rawLogContent != null && currentTab.rawLogContent.length() > 0) {
            currentTab.textArea.setText("");
            currentTab.seenLogContents.clear(); // Reset seen contents when reprocessing
            String[] lines = currentTab.rawLogContent.toString().split("\n");

            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    appendToLogWithFiltering(currentTab, line, false);
                }
            }
        }

        // Update checkbox state
        Component[] components = ((Container) tabbedPane.getParent()).getComponents();
        for (Component comp : components) {
            if (comp instanceof JToolBar) {
                for (Component toolbarComp : ((JToolBar) comp).getComponents()) {
                    if (toolbarComp instanceof JPanel) {
                        for (Component panelComp : ((JPanel) toolbarComp).getComponents()) {
                            if (panelComp instanceof JCheckBox
                                    && ((JCheckBox) panelComp).getText().equals("Filter Duplicates")) {
                                ((JCheckBox) panelComp).setSelected(currentTab.filterDuplicates);
                                break;
                            }
                        }
                    }
                }
            }
        }

        try {
            saveConfiguration();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Toggles word wrapping for the current tab.
     * 
     * Enables/disables line wrapping and word wrapping in the text area.
     * Updates UI state and saves configuration after toggling.
     *
     * @param wordWrapButton The button that triggered the toggle (unused)
     */
    private static void toggleWordWrap(JButton wordWrapButton) {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null)
            return;

        currentTab.wordWrap = !currentTab.wordWrap;
        currentTab.textArea.setLineWrap(currentTab.wordWrap);
        currentTab.textArea.setWrapStyleWord(currentTab.wordWrap);

        // Update checkbox state if it exists
        Component[] components = ((Container) tabbedPane.getParent()).getComponents();
        for (Component comp : components) {
            if (comp instanceof JToolBar) {
                for (Component toolbarComp : ((JToolBar) comp).getComponents()) {
                    if (toolbarComp instanceof JPanel) {
                        for (Component panelComp : ((JPanel) toolbarComp).getComponents()) {
                            if (panelComp instanceof JCheckBox
                                    && ((JCheckBox) panelComp).getText().equals("Word Wrap")) {
                                ((JCheckBox) panelComp).setSelected(currentTab.wordWrap);
                                break;
                            }
                        }
                    }
                }
            }
        }

        try {
            saveConfiguration();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a settings dialog for configuring connection parameters.
     *
     * Creates a modal dialog with the following features:
     * - Connection settings panel with fields for:
     * - Host address
     * - Port number
     * - Username
     * - Password
     * - Theme-aware styling that adapts to dark/light mode
     * - Input validation for port number
     * - Save/Cancel buttons with hover effects
     * - Live preview of changes
     *
     * When settings are saved:
     * - Updates current tab's connection details
     * - Updates main form fields to match
     * - Saves configuration to persistent storage
     * - Restarts monitoring with new settings
     * - Shows success confirmation
     *
     * The dialog is modal and closes on Save (after validation) or Cancel.
     * Connection monitoring is restarted with new settings after saving.
     * Error handling includes:
     * - Port number validation
     * - Configuration save errors
     * - Monitoring thread management
     */
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

        TabInfo tabInfo = getCurrentTab();

        JTextField hostField = new JTextField(tabInfo.host, 20);
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

        JTextField portField = new JTextField(String.valueOf(tabInfo.port), 5);
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

        JTextField usernameField = new JTextField(tabInfo.user, 20);
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
        connectionPanel.add(passwordLabel, gbc);

        JPasswordField passwordField = new JPasswordField(tabInfo.password, 20);
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
            // Update the current tab's settings
            tabInfo.host = hostField.getText();
            try {
                int newPort = Integer.parseInt(portField.getText());
                tabInfo.port = newPort;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(settingsDialog, "Invalid port number", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            tabInfo.user = usernameField.getText();
            tabInfo.password = new String(passwordField.getPassword());

            // Update the main form fields to reflect the new settings
            if (hostField != null)
                hostField.setText(tabInfo.host);
            if (userField != null)
                userField.setText(tabInfo.user);
            if (passwordField != null)
                passwordField.setText(tabInfo.password);
            if (logFileField != null)
                logFileField.setText(tabInfo.logFile);

            try {
                // Save configuration immediately
                saveConfiguration();

                // Restart monitoring with new settings
                if (tabInfo.monitoringThread != null) {
                    tabInfo.monitoringThread.interrupt();
                }
                startFileMonitoring(tabInfo);

                settingsDialog.dispose();
                JOptionPane.showMessageDialog(null, "Settings saved successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (JSONException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(settingsDialog,
                        "Error saving configuration: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> settingsDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        settingsDialog.add(mainPanel);
        settingsDialog.setVisible(true);
    }

    /**
     * Creates a styled toolbar button with text and tooltip.
     *
     * Creates a JButton with consistent styling for the toolbar including:
     * - Button text
     * - Hover tooltip
     * - Standard button styling (colors, borders, etc)
     * - Consistent padding/margins
     *
     * @param text    The text to display on the button
     * @param tooltip The tooltip text shown on hover
     * @return A styled JButton instance
     */
    private static JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        styleButton(button);
        button.setMargin(new Insets(6, 12, 6, 12));
        return button;
    }

    /**
     * Creates a tab component with title and close functionality.
     *
     * Creates a custom tab component that includes:
     * - Tab title label
     * - Right-click context menu
     * - Theme-aware styling
     * - Selected state highlighting
     *
     * @param title The text to display as the tab title
     * @param index The index position of this tab
     */
    private static void createTabComponent(String title, int index) {
        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        tabComponent.setOpaque(true);
        tabComponent.setBackground(isDarkMode ? new Color(60, 63, 65) : new Color(230, 230, 230));

        if (tabbedPane.getSelectedIndex() == index) {
            tabComponent.setBackground(isDarkMode ? new Color(80, 83, 85) : new Color(210, 210, 210));
        }

        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        TabInfo tab = tabs.get(index);
        JLabel titleLabel = new JLabel(tab.tabName + " ");
        titleLabel.setOpaque(false);
        titleLabel.setForeground(isDarkMode ? DARK_MODE_TEXT : Color.BLACK);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));

        tabComponent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0,
                        tabbedPane.getSelectedIndex() == index
                                ? (isDarkMode ? DARK_LIGHT_MODE_BUTTON_BACKGROUND : new Color(0, 120, 215))
                                : tabComponent.getBackground()),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(60, 60, 60) : new Color(200, 200, 200)));

        JMenuItem renameItem = createMenuItem("Rename Tab", e -> showRenameDialog(index));
        JMenuItem closeItem = createMenuItem("Close Tab", e -> closeTab(index));

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

        JButton closeButton = new JButton("\u2715");
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(false);
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setFont(closeButton.getFont().deriveFont(10f));
        closeButton.setForeground(isDarkMode ? new Color(180, 180, 180) : new Color(100, 100, 100));
        closeButton.setFocusable(false);
        closeButton.setToolTipText("Close this tab");
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeButton.setForeground(new Color(240, 71, 71));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeButton.setForeground(isDarkMode ? new Color(180, 180, 180) : new Color(100, 100, 100));
            }
        });

        closeButton.addActionListener(e -> closeTab(index));

        tabComponent.add(titleLabel);
        tabComponent.add(closeButton);

        tabbedPane.setTabComponentAt(index, tabComponent);

        tabbedPane.addChangeListener(e -> {
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

    /**
     * Sets up keyboard shortcuts/hotkeys for the main application window.
     * 
     * Registers the following hotkeys:
     * - Ctrl+T: Create new tab
     * - Ctrl+W: Close current tab
     * - Alt+Up: Fast scroll up
     * - Alt+Down: Fast scroll down
     * - Ctrl+S: Save current tab settings
     * - F2: Rename current tab
     * - Ctrl+F: Show search dialog
     * - Ctrl+E: Export log
     * - Ctrl+L: Clear log
     * - Ctrl+Enter: Save and connect
     *
     * Each hotkey is registered to the frame's root pane and triggers the
     * appropriate
     * action when pressed. Actions are only performed if a tab exists and is
     * selected
     * where relevant.
     *
     * @param frame The main application JFrame to register hotkeys on
     */
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

        KeyStroke fastScrollUpKey = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            TabInfo currentTab = getCurrentTab();
            if (currentTab != null) {
                JScrollPane scrollPane = (JScrollPane) currentTab.textArea.getParent().getParent();
                if (scrollPane != null) {
                    JScrollBar vbar = scrollPane.getVerticalScrollBar();
                    vbar.setValue(vbar.getValue() - FAST_SCROLL_LINES * vbar.getUnitIncrement());
                }
            }
        }, fastScrollUpKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke fastScrollDownKey = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            TabInfo currentTab = getCurrentTab();
            if (currentTab != null) {
                JScrollPane scrollPane = (JScrollPane) currentTab.textArea.getParent().getParent();
                if (scrollPane != null) {
                    JScrollBar vbar = scrollPane.getVerticalScrollBar();
                    vbar.setValue(vbar.getValue() + FAST_SCROLL_LINES * vbar.getUnitIncrement());
                }
            }
        }, fastScrollDownKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

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
        frame.getRootPane().registerKeyboardAction(e -> showSearchDialog(), searchKey,
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke exportKey = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> exportLog(), exportKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke clearKey = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
        frame.getRootPane().registerKeyboardAction(e -> {
            TabInfo currentTab = getCurrentTab();
            if (currentTab != null) {
                clearLogFromLinux(currentTab);
            }
        }, clearKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

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

    /**
     * Shows a dialog to rename the tab at the specified index.
     *
     * Prompts the user for a new tab name and updates the tab if a valid name
     * is provided. The new name is saved to the tab's configuration.
     * Empty or whitespace-only names are ignored.
     *
     * @param index The index of the tab to rename
     */
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

    /**
     * Toggles timestamp display for the current tab's log entries.
     *
     * When enabled, shows timestamps in [YYYY-MM-DD HH:mm:ss] format.
     * When disabled, shows only the log message content.
     * 
     * Features:
     * - Preserves original timestamps when available
     * - Reprocesses existing content to apply/remove timestamps
     * - Updates UI checkbox state to match
     * - Saves preference to configuration
     * - Filters empty lines
     * - Maintains duplicate filtering settings
     */
    private static void toggleTimestamp() {
        TabInfo currentTab = getCurrentTab();
        if (currentTab == null)
            return;

        currentTab.showTimestamp = !currentTab.showTimestamp;

        // Clear and reprocess existing content
        if (currentTab.rawLogContent != null && currentTab.rawLogContent.length() > 0) {
            currentTab.textArea.setText("");
            String[] lines = currentTab.rawLogContent.toString().split("\n");

            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    String actualContent;
                    if (line.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
                        actualContent = line.substring(line.indexOf("]") + 1).trim();
                    } else {
                        actualContent = line.trim();
                    }

                    if (currentTab.showTimestamp) {
                        // Use the original timestamp if available
                        if (line.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
                            appendToLogWithFiltering(currentTab, line, false);
                        } else {
                            appendToLogWithFiltering(currentTab, actualContent, false);
                        }
                    } else {
                        // Just show the content without timestamp
                        appendToLogWithFiltering(currentTab, actualContent, false);
                    }
                }
            }
        }

        // Update checkbox state
        Component[] components = ((Container) tabbedPane.getParent()).getComponents();
        for (Component comp : components) {
            if (comp instanceof JToolBar) {
                for (Component toolbarComp : ((JToolBar) comp).getComponents()) {
                    if (toolbarComp instanceof JPanel) {
                        for (Component panelComp : ((JPanel) toolbarComp).getComponents()) {
                            if (panelComp instanceof JCheckBox
                                    && ((JCheckBox) panelComp).getText().equals("Show Timestamp")) {
                                ((JCheckBox) panelComp).setSelected(currentTab.showTimestamp);
                                break;
                            }
                        }
                    }
                }
            }
        }

        try {
            saveConfiguration();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles automatic connection attempts for tabs configured with auto-connect.
     *
     * For each tab with auto-connect enabled:
     * 1. Tests SSH connection with 5 second timeout
     * 2. If successful, starts file monitoring
     * 3. If failed, updates status with error message
     *
     * Connection parameters are loaded from each tab's saved configuration.
     * Failed connections are logged but don't prevent other tabs from connecting.
     */
    private static void handleAutoConnect() {
        for (TabInfo tab : tabs) {
            if (tab.autoConnect) {
                try {
                    // Test connection before starting monitoring
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(tab.user, tab.host, tab.port);
                    session.setPassword(tab.password);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect(5000); // 5 second timeout
                    session.disconnect();

                    startFileMonitoring(tab);
                } catch (JSchException e) {
                    // Connection failed, update status and log error
                    updateStatus(tab, false, "Connection failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sets up window closing behavior for the main application frame.
     *
     * Adds a WindowListener that performs cleanup tasks when the window is closing:
     * 1. Saves current configuration state to persistent storage
     * 2. Interrupts and cleans up all tab monitoring threads
     * 3. Exits the application
     *
     * The cleanup ensures:
     * - User settings and preferences are preserved
     * - All background monitoring threads are properly terminated
     * - Application exits cleanly without resource leaks
     *
     * @param frame The main JFrame to add the window listener to
     */
    private static void setupWindowListener(JFrame frame) {
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    saveConfiguration();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Clean up monitoring threads
                for (TabInfo tab : tabs) {
                    if (tab.monitoringThread != null) {
                        tab.monitoringThread.interrupt();
                    }
                }
                System.exit(0);
            }
        });
    }

}
