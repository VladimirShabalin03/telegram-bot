package com.vladimir;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class SussyLuckyJavaPonyBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    public SussyLuckyJavaPonyBot(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = String.valueOf(update.getMessage().getChatId());
            String text = update.getMessage().getText();

            if (text.startsWith("/")) {
                commandsHandler(chatId, text);
            } else {
                sendMessage(chatId, text);
            }

        }
    }

    void commandsHandler(String chatId, String command) {
        switch (command) {
            case "/start" -> handleStart(chatId);
            case "/help" -> handleHelp(chatId);
            default -> sendMessage(chatId, "Неизвестная команда. Введите /help");
        }
    }

    void handleStart(String chatId) {
        sendMessage(chatId, "Добро пожаловать!");
    }

    void handleHelp(String chatId) {
        sendMessage(chatId, "/start - приветствие\n/help - список команд");
    }

    void sendMessage(String chatId, String text) {

        SendMessage sendMessage = new SendMessage(chatId, text);

        try {
            // Execute it
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

}
