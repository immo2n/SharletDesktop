package com.immo2n;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Installer {
    private static final Logger log = LoggerFactory.getLogger(Installer.class);
    public static String currentDir = System.getProperty("user.dir");
    public static String staticPath = getDefaultPath();
    protected boolean isInstalled(){
        File configFile = new File(currentDir, "config.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Gson gson = new Gson();
                JsonObject config = gson.fromJson(reader, JsonObject.class);
                if (config.has("staticPath") && !config.get("staticPath").getAsString().isEmpty()) {
                    staticPath = config.get("staticPath").getAsString();
                    return false;
                } else return true;
            } catch (IOException e) {
                log.error(e.toString());
                return true;
            }
        } else return true;
    }
    protected static String getDefaultPath(){
        File dataFolder = new File(currentDir, "sharlet-data");
        if (!dataFolder.exists()) {
            if (dataFolder.mkdir()) {
                return dataFolder.getAbsolutePath();
            } else {
                log.error("Failed to create data folder");
                return null;
            }
        } else return dataFolder.getAbsolutePath();
    }
    protected static void writeConfig(String staticPathValue) {
        File configFile = new File(currentDir, "config.json");
        JsonObject config = new JsonObject();
        config.addProperty("staticPath", staticPathValue);
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new Gson();
            gson.toJson(config, writer);
            log.info("Config file updated with staticPath: " + staticPathValue);
        } catch (IOException e) {
            log.error("Error writing to config file: " + e);
        }
    }
}