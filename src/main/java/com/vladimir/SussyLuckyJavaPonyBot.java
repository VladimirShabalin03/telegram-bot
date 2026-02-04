package com.vladimir;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

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

public class SussyLuckyJavaPonyBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private enum UserState {
        START,
        WAITING_VACANCY_INPUT,
    }
    Map<String, UserState> userStates = new HashMap<>();


    public SussyLuckyJavaPonyBot(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            String chatId = String.valueOf(update.getMessage().getChatId());
            if (text.startsWith("/")) {
                commandsHandler(chatId, text);
            } else {
                switch (userStates.getOrDefault(chatId, UserState.START)){
                    case UserState.WAITING_VACANCY_INPUT -> handleSearchVacancies(chatId, text);
                    default -> sendMessage(chatId, text);
                }
            }
        } else if (update.hasCallbackQuery()) {
            callbackQueryHandler(String.valueOf(update.getCallbackQuery().getMessage().getChatId()), update.getCallbackQuery());
        }
    }

    void commandsHandler(String chatId, String command) {
        switch (command) {
            case "/start" -> handleStart(chatId);
            case "/help" -> handleHelp(chatId);
            default -> sendMessage(chatId, "Неизвестная команда. Введите /help");
        }
    }

    void callbackQueryHandler(String chatId, CallbackQuery callbackQuery) {
        String callbackQueryData = callbackQuery.getData();
        String callbackQueryId = callbackQuery.getId();

        switch (callbackQueryData) {
            case "search_vacancies" -> handleCallbackSearchVacancies(chatId);
            case "parse_prices" -> handleCallbackParsePrices(chatId);
            default -> sendMessage(chatId, "Неизвестная команда. Введите /help");
        }
        answerCallbackQuery(callbackQueryId);
    }

    void  handleCallbackSearchVacancies(String chatId) {
        userStates.put(chatId, UserState.WAITING_VACANCY_INPUT);
    }

    void  handleCallbackParsePrices(String chatId) {

    }

    void handleSearchVacancies(String chatId, String text) {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.hh.ru/vacancies?text=" + text))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(response.body(), JsonObject.class);
            JsonArray items = root.getAsJsonArray("items");

            for (JsonElement item : items) {
                JsonObject vacancy = item.getAsJsonObject();
                String name = vacancy.get("name").getAsString();
                JsonObject employer = vacancy.getAsJsonObject("employer");
                String employerName = employer.get("name").getAsString();

                JsonElement salaryElement = vacancy.get("salary");
                String salaryInfo;
                JsonObject salary = salaryElement.getAsJsonObject();
                JsonElement fromEl = salary.get("from");
                JsonElement toEl = salary.get("to");
                String currency = salary.get("currency").getAsString();

                String from = (fromEl != null && !fromEl.isJsonNull()) ? fromEl.getAsString() : null;
                String to = (toEl != null && !toEl.isJsonNull()) ? toEl.getAsString() : null;

                if (from != null && to != null) {
                    salaryInfo = String.format("от %s до %s %s", from, to, currency);
                } else if (from != null) {
                    salaryInfo = String.format("от %s %s", from, currency);
                } else if (to != null) {
                    salaryInfo = String.format("до %s %s", to, currency);
                } else {
                    salaryInfo = "не указана";
                }

                JsonElement areaElement = vacancy.get("area");
                JsonObject areaObject = areaElement.getAsJsonObject();
                String area = areaObject.get("name").getAsString();
                String url = vacancy.get("alternate_url").getAsString();

                String message = String.format("Название вакансии: %s%nНазвание компании: %s%nЗарплата: %s%nРегион: %s%nСсылка: %s",
                        name, employerName,salaryInfo, area, url);

                sendMessage(chatId, message);

            }

        } catch (IOException | InterruptedException e) {
            sendMessage(chatId, "Ошибка");
        }


        userStates.remove(chatId);
    }

    void handleStart(String chatId) {
        InlineKeyboardMarkup menuKeyboard = createMenuKeyboard();
        sendMessage(chatId, "Добро пожаловать!", menuKeyboard);
    }

    void handleHelp(String chatId) {
        sendMessage(chatId, "/start - приветствие\n/help - список команд");
    }

    void sendMessage(String chatId, String text) {
        sendMessage(chatId, text, null);
    }

    void sendMessage(String chatId, String text, InlineKeyboardMarkup keyboard) {

        SendMessage sendMessage = new SendMessage(chatId, text);
        if (keyboard != null ) sendMessage.setReplyMarkup(keyboard);

        execute(sendMessage);
    }

    static InlineKeyboardMarkup createMenuKeyboard() {

        // Создание кнопок
        InlineKeyboardButton searchVacanciesButton = createInlineKeyboardButton("Найти вакансии", "search_vacancies");
        InlineKeyboardButton parsePricesButton = createInlineKeyboardButton("Парсинг цен", "parse_prices");

        // Создание рядов (при необходимости создать новый ряд)
        InlineKeyboardRow firstRow = new InlineKeyboardRow();
        firstRow.add(searchVacanciesButton);
        firstRow.add(parsePricesButton);

        // Создание списка рядов
        List<InlineKeyboardRow> rowList = new ArrayList<>();
        rowList.add(firstRow);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rowList);
        return markup;
    }

    static InlineKeyboardButton createInlineKeyboardButton(String buttonText, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(buttonText);
        button.setCallbackData(callbackData);

        return  button;
    }

    void execute(BotApiMethod<?> method) {
        try {
            // Execute it
            telegramClient.execute(method);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void answerCallbackQuery(String callBackQueryId) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callBackQueryId);
        execute(answerCallbackQuery);
    }
}
