package com.vladimir;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class App
{
    public static void main( String[] args )
    {

        Config config = new Config("config.properties");
        String botToken = config.getBotToken();
        try {
            TelegramClient telegramClient = new OkHttpTelegramClient(botToken);
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new SussyLuckyJavaPonyBot(telegramClient));

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
