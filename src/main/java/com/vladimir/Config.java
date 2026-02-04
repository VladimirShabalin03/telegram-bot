package com.vladimir;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    Properties config = new Properties();
    String filename;

    public Config(String filename) {
        this.filename = filename;
    }

    public String getBotToken() {

        try (FileInputStream fis = new FileInputStream(filename)) {
            config.load(fis);
            return config.getProperty("bot.token");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить конфиг: " + filename, e);
        }
    }

}
