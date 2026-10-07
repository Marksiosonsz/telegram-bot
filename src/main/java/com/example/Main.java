package com.example;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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

        // Telegram User ID ng nag-request
        long userId = update.getMessage().getFrom().getId();

        String text = update.getMessage().getText();

        if (text.equals("/start")) {

            send(
                    chatId,
                    "🤖 Generator Bot\n\n" +
                    "Use:\n" +
                    "/gen ABCDEFGHIJ/AA/BB/CCC 5"
            );

        } else if (text.equals("/help")) {

            send(
                    chatId,
                    "Format:\n\n" +
                    "/gen BASE/AA/BB/CCC AMOUNT\n\n" +
                    "Example:\n" +
                    "/gen ABCDEFGHIJ/AA/BB/CCC 5\n\n" +
                    "1-59 results = message\n" +
                    "60-5000 results = TXT file"
            );

        } else if (text.startsWith("/gen ")) {

            generateAndSend(chatId, userId, text);

        } else {

            send(
                    chatId,
                    "Unknown command. Use /help"
            );
        }
    }

    private void generateAndSend(
            long chatId,
            long userId,
            String text) {

        try {

            String input = text.substring(5).trim();

            String[] data = input.split("\\s+");

            if (data.length != 2) {

                send(
                        chatId,
                        "Usage:\n/gen ABCDEFGHIJ/AA/BB/CCC 5"
                );

                return;
            }

            String[] parts = data[0].split("/", -1);

            if (parts.length != 4) {

                send(
                        chatId,
                        "Format:\nBASE/AA/BB/CCC AMOUNT"
                );

                return;
            }

            int amount;

            try {

                amount = Integer.parseInt(data[1]);

            } catch (NumberFormatException e) {

                send(
                        chatId,
                        "Amount must be a number."
                );

                return;
            }

            if (amount < 1 || amount > 5000) {

                send(
                        chatId,
                        "Amount must be between 1 and 5000."
                );

                return;
            }

            String base = parts[0];

            if (base.length() > 16) {

                send(
                        chatId,
                        "Base cannot exceed 16 characters."
                );

                return;
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

            /*
             * 60 OR MORE:
             * Send everything as one TXT file.
             */
            if (amount >= 60) {

                sendAsFile(
                        chatId,
                        userId,
                        amount,
                        result.toString()
                );

            } else {

                // Less than 60 = normal message
                send(
                        chatId,
                        result.toString()
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            send(
                    chatId,
                    "❌ Invalid command."
            );
        }
    }

    private String completeTo16(String value) {

        StringBuilder result =
                new StringBuilder(value);

        String letters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        while (result.length() < 16) {

            int index =
                    (int) (Math.random() * letters.length());

            result.append(
                    letters.charAt(index)
            );
        }

        return result.toString();
    }

    private void send(
            long chatId,
            String text) {

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

    private void sendAsFile(
            long chatId,
            long userId,
            int amount,
            String text) {

        try {

            // Filename:
            // 61_generated_by_7438848823_data.txt

            String fileName =
                    amount +
                    "_generated_by_" +
                    userId +
                    "_data.txt";

            byte[] fileBytes =
                    text.getBytes(StandardCharsets.UTF_8);

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(fileBytes);

            InputFile inputFile =
                    new InputFile(
                            inputStream,
                            fileName
                    );

            SendDocument document =
                    SendDocument.builder()
                            .chatId(chatId)
                            .document(inputFile)
                            .caption(
                                    "📄 " +
                                    amount +
                                    " generated data"
                            )
                            .build();

            telegramClient.execute(document);

        } catch (Exception e) {

            e.printStackTrace();

            send(
                    chatId,
                    "❌ Failed to send TXT file."
            );
        }
    }

    public static void main(String[] args) {

        String token =
                System.getenv("BOT_TOKEN");

        if (token == null || token.isEmpty()) {

            System.out.println(
                    "BOT_TOKEN is missing."
            );

            return;
        }

        try {

            TelegramBotsLongPollingApplication application =
                    new TelegramBotsLongPollingApplication();

            application.registerBot(
                    token,
                    new Main(token)
            );

            System.out.println(
                    "Bot is running!"
            );

            Thread.currentThread().join();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
