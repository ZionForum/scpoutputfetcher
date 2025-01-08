package com.outputfetcher;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Configuration {
    private static final String CONFIG_FILE = "log_viewer_config.json";
    
    public static class TabConfig {
        String host;
        String user;
        String logFile;
        String tabName;
        Color textColor;
        Color backgroundColor;
        Font font;
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("host", host != null ? host : "");
                json.put("user", user != null ? user : "");
                json.put("logFile", logFile != null ? logFile : "");
                json.put("tabName", tabName != null ? tabName : "");
                json.put("textColorRGB", textColor != null ? textColor.getRGB() : Color.BLACK.getRGB());
                json.put("backgroundColorRGB", backgroundColor != null ? backgroundColor.getRGB() : Color.WHITE.getRGB());
                if (font != null) {
                    json.put("fontName", font.getName());
                    json.put("fontSize", font.getSize());
                    json.put("fontStyle", font.getStyle());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }
        
        public static TabConfig fromJson(JSONObject json) {
            TabConfig config = new TabConfig();
            config.host = json.optString("host", "");
            config.user = json.optString("user", "");
            config.logFile = json.optString("logFile", "");
            config.tabName = json.optString("tabName", "");
            config.textColor = new Color(json.optInt("textColorRGB", Color.BLACK.getRGB()));
            config.backgroundColor = new Color(json.optInt("backgroundColorRGB", Color.WHITE.getRGB()));
            
            String fontName = json.optString("fontName", "Monospaced");
            int fontSize = json.optInt("fontSize", 12);
            int fontStyle = json.optInt("fontStyle", Font.PLAIN);
            config.font = new Font(fontName, fontStyle, fontSize);
            
            return config;
        }
    }
    
    private static JSONObject loadConfig() throws JSONException {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return new JSONObject().put("darkMode", false).put("tabs", new JSONArray());
        }
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            return new JSONObject(content);
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject().put("darkMode", false).put("tabs", new JSONArray());
        }
    }
    
    public static void saveConfig(boolean darkMode, List<TabConfig> tabConfigs) throws JSONException {
        JSONObject config = new JSONObject();
        config.put("darkMode", darkMode);
        
        JSONArray tabsArray = new JSONArray();
        for (TabConfig tabConfig : tabConfigs) {
            tabsArray.put(tabConfig.toJson());
        }
        config.put("tabs", tabsArray);
        
        try {
            Files.write(Paths.get(CONFIG_FILE), config.toString(2).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static boolean isDarkMode() {
        try {
            return loadConfig().optBoolean("darkMode", false);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<TabConfig> loadTabConfigs() {
        List<TabConfig> configs = new ArrayList<>();
        try {
            JSONArray tabsArray = loadConfig().optJSONArray("tabs");
            if (tabsArray != null) {
                for (int i = 0; i < tabsArray.length(); i++) {
                    configs.add(TabConfig.fromJson(tabsArray.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return configs;
    }
}
