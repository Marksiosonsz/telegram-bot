package com.example;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class Main implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    public Main(String token) {
        telegramClient = new OkHttpTelegramClient(token);
    }

    @Override
    public void consume(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        String response;

        if (text.equals("/start")) {

            response =
                    "🤖 Generator Bot\n\n" +
                    "Use:\n" +
                    "/gen ABCDEFGHIJ/AA/BB/CCC 5";

        } else if (text.equals("/help")) {

            response =
                    "Format:\n\n" +
                    "/gen BASE/AA/BB/CCC AMOUNT\n\n" +
                    "Example:\n" +
                    "/gen ABCDEFGHIJ/AA/BB/CCC 5";

        } else if (text.startsWith("/gen ")) {

            response = generate(text);

        } else {

            response = "Unknown command. Use /help";
        }

        send(chatId, response);
    }

    private String generate(String text) {

        try {

            String input = text.substring(5).trim();

            String[] data = input.split("\\s+");

            if (data.length != 2) {
                return "Usage:\n/gen ABCDEFGHIJ/AA/BB/CCC 5";
            }

            String[] parts = data[0].split("/", -1);

            if (parts.length != 4) {
                return "Format:\nBASE/AA/BB/CCC AMOUNT";
            }

            int amount = Integer.parseInt(data[1]);

            if (amount < 1 || amount > 5000) {
                return "Amount must be between 1 and 5000.";
            }

            String base = parts[0];

            if (base.length() > 16) {
                return "Base cannot exceed 16 characters.";
            }

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < amount; i++) {

                String completed = completeTo16(base);

                result.append(completed)
                      .append("/")
                      .append(parts[1])
                      .append("/")
                      .append(parts[2])
                      .append("/")
                      .append(parts[3]);

                if (i < amount - 1) {
                    result.append("\n");
                }
            }

            return result.toString();

        } catch (NumberFormatException e) {

            return "Amount must be a number.";

        } catch (Exception e) {

            return "Invalid command.";
        }
    }

    private String completeTo16(String value) {

        StringBuilder result = new StringBuilder(value);

        String letters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        while (result.length() < 16) {

            int index =
                    (int) (Math.random() * letters.length());

            result.append(letters.charAt(index));
        }

        return result.toString();
    }

    private void send(long chatId, String text) {

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build();

        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String token = System.getenv("BOT_TOKEN");

        if (token == null || token.isEmpty()) {
            System.out.println("BOT_TOKEN is missing.");
            return;
        }

        try {

            TelegramBotsLongPollingApplication application =
                    new TelegramBotsLongPollingApplication();

            application.registerBot(
                    token,
                    new Main(token)
            );

            System.out.println("Bot is running!");

            Thread.currentThread().join();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
