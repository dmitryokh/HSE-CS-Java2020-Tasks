package ru.hse.cs.java2020.task03;

import com.pengrad.telegrambot.TelegramBot;

public class Main {
    public static void main(String[] args) {
        Database db = new Database("/Users/dmitry/Documents/test.db");
        var client = Client.getTrackerClient();
        var bot = new TelegramBot("1078136387:AAGiOtQprRIfCV3zb8oaXSy2GHDGIDlqDTU");
        var tBot = new TelegrammBot(bot, db, client);
        tBot.run();
    }
}
