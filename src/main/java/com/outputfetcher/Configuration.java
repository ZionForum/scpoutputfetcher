package com.outputfetcher;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration class for managing application settings and tab configurations.
 * Handles saving/loading of settings to/from JSON file including encrypted
 * passwords.
 */
public class Configuration {
    private static final String CONFIG_FILE = "log_viewer_config.json";

    /**
     * Configuration for a single tab including connection details and display
     * settings
     */
    public static class TabConfig {
        public String host;
        public int port = 22;
        public String user;
        public String password;
        public String logFile;
        public String tabName;
        public Color textColor = Color.BLACK;
        public Color backgroundColor = Color.WHITE;
        public Font font = new Font("Monospaced", Font.PLAIN, 12);
        public boolean wordWrap = false;
        public boolean filterDuplicates = false;
        public boolean showTimestamp = true;
        public boolean autoConnect = false;

        public int textColorRGB;
        public int backgroundColorRGB;
        public String fontName;
        public int fontSize;
        public int fontStyle;

        public void setEncryptedPassword(String password) {
            try {
                if (password == null || password.isEmpty()) {
                    this.password = "";
                    return;
                }
                String shifted = shiftString(password, true);
                String encoded = Base64.getEncoder().encodeToString(shifted.getBytes());
                this.password = shiftString(encoded, true);
            } catch (Exception e) {
                e.printStackTrace();
                this.password = "";
            }
        }

        public String getDecryptedPassword() {
            try {
                if (this.password == null || this.password.isEmpty()) {
                    return "";
                }
                String unshifted = shiftString(this.password, false);
                byte[] decoded = Base64.getDecoder().decode(unshifted);
                return shiftString(new String(decoded), false);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        private String shiftString(String input, boolean encrypt) {
            StringBuilder result = new StringBuilder();
            int shift = 5;
            for (char c : input.toCharArray()) {
                char shifted = encrypt ? (char)(c + shift) : (char)(c - shift);
                result.append(shifted);
            }
            return result.toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("host", host != null ? host : "");
                json.put("port", port);
                json.put("user", user != null ? user : "");
                json.put("password", password != null ? password : "");
                json.put("logFile", logFile != null ? logFile : "");
                json.put("tabName", tabName != null ? tabName : "");
                json.put("textColorRGB", textColor != null ? textColor.getRGB() : Color.BLACK.getRGB());
                json.put("backgroundColorRGB",
                        backgroundColor != null ? backgroundColor.getRGB() : Color.WHITE.getRGB());
                if (font != null) {
                    json.put("fontName", font.getName());
                    json.put("fontSize", font.getSize());
                    json.put("fontStyle", font.getStyle());
                }
                json.put("wordWrap", wordWrap);
                json.put("filterDuplicates", filterDuplicates);
                json.put("showTimestamp", showTimestamp);
                json.put("autoConnect", autoConnect);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        public static TabConfig fromJson(JSONObject json) {
            TabConfig config = new TabConfig();
            config.host = json.optString("host", "");
            config.port = json.optInt("port", 22);
            config.user = json.optString("user", "");
            config.password = json.optString("password", "");
            config.logFile = json.optString("logFile", "");
            config.tabName = json.optString("tabName", "");
            config.textColor = new Color(json.optInt("textColorRGB", Color.BLACK.getRGB()));
            config.backgroundColor = new Color(json.optInt("backgroundColorRGB", Color.WHITE.getRGB()));

            String fontName = json.optString("fontName", "Monospaced");
            int fontSize = json.optInt("fontSize", 12);
            int fontStyle = json.optInt("fontStyle", Font.PLAIN);
            config.font = new Font(fontName, fontStyle, fontSize);

            config.wordWrap = json.optBoolean("wordWrap", false);
            config.filterDuplicates = json.optBoolean("filterDuplicates", false);
            config.showTimestamp = json.optBoolean("showTimestamp", true);
            config.autoConnect = json.optBoolean("autoConnect", false);

            return config;
        }
    }

    private static JSONObject loadConfig() throws JSONException {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return new JSONObject()
                    .put("darkMode", false)
                    .put("tabs", new JSONArray())
                    .put("globalHost", "localhost")
                    .put("globalPort", 22)
                    .put("globalUsername", "")
                    .put("globalPassword", "");
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            return new JSONObject(content);
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject()
                    .put("darkMode", false)
                    .put("tabs", new JSONArray())
                    .put("globalHost", "localhost")
                    .put("globalPort", 22)
                    .put("globalUsername", "")
                    .put("globalPassword", "");
        }
    }

    public static void saveConfig(boolean darkMode, List<TabConfig> tabConfigs) throws JSONException {
        JSONObject config = new JSONObject();
        config.put("darkMode", darkMode);

        JSONArray tabsArray = new JSONArray();
        for (TabConfig tabConfig : tabConfigs) {
            JSONObject tabJson = tabConfig.toJson();
            tabJson.put("autoConnect", tabConfig.autoConnect);
            tabsArray.put(tabJson);
        }
        config.put("tabs", tabsArray);

        try (FileWriter file = new FileWriter(CONFIG_FILE)) {
            file.write(config.toString(2));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isDarkMode() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            JSONObject config = new JSONObject(content);
            return config.optBoolean("darkMode", false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getGlobalHost() {
        try {
            return loadConfig().optString("globalHost", "localhost");
        } catch (JSONException e) {
            e.printStackTrace();
            return "localhost";
        }
    }

    public static int getGlobalPort() {
        try {
            return loadConfig().optInt("globalPort", 22);
        } catch (JSONException e) {
            e.printStackTrace();
            return 22;
        }
    }

    public static String getGlobalUsername() {
        try {
            return loadConfig().optString("globalUsername", "");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getGlobalPassword() {
        try {
            String encodedPassword = loadConfig().optString("globalPassword", "");
            if (!encodedPassword.isEmpty()) {
                return new String(Base64.getDecoder().decode(encodedPassword));
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static List<TabConfig> loadTabConfigs() {
        List<TabConfig> configs = new ArrayList<>();
        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            return configs;
        }

        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            JSONObject config = new JSONObject(content);
            JSONArray tabsArray = config.getJSONArray("tabs");

            for (int i = 0; i < tabsArray.length(); i++) {
                JSONObject tabJson = tabsArray.getJSONObject(i);
                TabConfig tabConfig = TabConfig.fromJson(tabJson);
                tabConfig.autoConnect = tabJson.optBoolean("autoConnect", false);
                configs.add(tabConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return configs;
    }
}
