package com.vladimir;

import com.google.gson.Gson;
import com.vladimir.model.UserState;
import com.vladimir.model.Vacancy;
import com.vladimir.model.VacancyResponse;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SussyLuckyJavaPonyBot implements LongPollingSingleThreadUpdateConsumer {

    // Константы {
    private static final String HH_API_URL = "https://api.hh.ru/vacancies?text=";
    private static final String CMD_START = "/start";
    private static final String CMD_HELP = "/help";
    private static final String CALLBACK_SEARCH_VACANCIES = "search_vacancies";
    private static final String CALLBACK_PARSE_PRICES = "parse_prices";
    // Константы }

    // Поля {
    private final TelegramClient telegramClient;
    private final Map<String, UserState> userStates = new HashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    // Поля }

    public SussyLuckyJavaPonyBot(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        }
    }

    // Обработчики {
    private void handleTextMessage(Message message) {
        String chatId = extractChatId(message);
        String text = message.getText();

        if (text.startsWith("/")) {
            commandsHandler(chatId, text);
        } else {
            handleUserInput(chatId, text);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String chatId = String.valueOf(callbackQuery.getMessage().getChatId());
        String callbackData = callbackQuery.getData();
        String callbackId = callbackQuery.getId();

        switch (callbackData) {
            case CALLBACK_SEARCH_VACANCIES -> handleCallbackSearchVacancies(chatId);
            case CALLBACK_PARSE_PRICES -> handleCallbackParsePrices(chatId);
            default -> sendMessage(chatId, "Неизвестная команда");
        }
        answerCallbackQuery(callbackId);
    }

    private void handleUserInput(String chatId, String text) {
        UserState state = userStates.getOrDefault(chatId, UserState.START);
        switch (state) {
            case WAITING_VACANCY_INPUT -> handleSearchVacancies(chatId, text);
            default -> sendMessage(chatId, "Не понял. Нажмите " + CMD_START);
        }
    }

    private void commandsHandler(String chatId, String command) {
        switch (command) {
            case CMD_START -> handleStart(chatId);
            case CMD_HELP -> handleHelp(chatId);
            default -> sendMessage(chatId, "Неизвестная команда. Введите " + CMD_HELP);
        }
    }

    private void handleStart(String chatId) {
        sendMessage(chatId, "Добро пожаловать!", createMenuKeyboard());
    }

    private void handleHelp(String chatId) {
        sendMessage(chatId, String.format("%s - приветствие%n%s - список команд",
                CMD_START, CMD_HELP));
    }

    private void handleCallbackSearchVacancies(String chatId) {
        userStates.put(chatId, UserState.WAITING_VACANCY_INPUT);
        sendMessage(chatId, "Введите название вакансии:");
    }

    private void handleCallbackParsePrices(String chatId) {
        // TODO: Реализовать
    }

    private void handleSearchVacancies(String chatId, String text) {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HH_API_URL + encodedText))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> processVacancyResponse(chatId, response.body()))
                .exceptionally(e -> {
                    sendMessage(chatId, "Ошибка при поиске вакансий");
                    userStates.remove(chatId);
                    return null;
                });
    }

    // Обработчики }

    // Вспомогательные методы {
    private void processVacancyResponse(String chatId, String responseBody) {
        VacancyResponse vacancyResponse = gson.fromJson(responseBody, VacancyResponse.class);

        if (vacancyResponse.getItems().isEmpty()) {
            sendMessage(chatId, "Вакансий не найдено");
            userStates.remove(chatId);
            return;
        }

        for (Vacancy vacancy : vacancyResponse.getItems()) {
            sendMessage(chatId, vacancy.vacancyMessage());
        }
        userStates.remove(chatId);
    }

    private void sendMessage(String chatId, String text) {
        sendMessage(chatId, text, null);
    }

    private void sendMessage(String chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage(chatId, text);
        if (keyboard != null) {
            sendMessage.setReplyMarkup(keyboard);
        }
        execute(sendMessage);
    }

    private void execute(BotApiMethod<?> method) {
        try {
            telegramClient.execute(method);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void answerCallbackQuery(String callbackQueryId) {
        execute(new AnswerCallbackQuery(callbackQueryId));
    }

    private String extractChatId(Message message) {
        return String.valueOf(message.getChatId());
    }

    private static InlineKeyboardMarkup createMenuKeyboard() {
        // Один комментарий для блядей хейтеров 1С
        InlineKeyboardButton searchButton = createButton("Найти вакансии", CALLBACK_SEARCH_VACANCIES);
        InlineKeyboardButton pricesButton = createButton("Парсинг цен", CALLBACK_PARSE_PRICES);

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(searchButton);
        row.add(pricesButton);

        return new InlineKeyboardMarkup(List.of(row));
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callbackData);
        return button;
    }
    // Вспомогательные методы }
}