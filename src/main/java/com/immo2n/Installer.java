package com.immo2n;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Installer {
    private static final Logger log = LoggerFactory.getLogger(Installer.class);
    public static String currentDir = System.getProperty("user.dir");
    public static String staticPath = currentDir + "/data/";
    public boolean isInstalled(){
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
}