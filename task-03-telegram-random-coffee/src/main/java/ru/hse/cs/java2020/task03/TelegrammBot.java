package ru.hse.cs.java2020.task03;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendMessage;

import java.io.IOException;
import java.util.Optional;

public class TelegrammBot {
    private static final int MAGIC_NUBMER_3 = 3;
    private static final int MAGIC_NUBMER_4 = 4;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int AUTHORIZE_SIZE = 4;
    private static final int MAKE_TASK_SIZE = 4;

    public void run() {
        db.start();
        bot.setUpdatesListener(updates -> {
            for (var u : updates) {
                long chatId = u.message().chat().id();
                var body = u.message().text();
                var request = body.split("\n");
                processUpdate(chatId, request);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        //db.stop();
    }

    public void processUpdate(long chatId, String[] request) {
        int flag = 1;
        if (request[0].equals("/authorize")) {
            authorize(chatId, request);
            flag = 0;
        }
        if (request[0].equals("/makeTask")) {
            makeTask(chatId, request);
            flag = 0;
        }
        if (request[0].equals("/getTaskInfo")) {
            getTask(chatId, request);
            flag = 0;
        }
        if (request[0].equals("/getMyTasks")) {
            getMyTasks(chatId, request);
            flag = 0;
        }
        if (request[0].equals("/getMyQueues")) {
            getMyQueues(chatId);
            flag = 0;
        }
        if (request[0].equals("/start")) {
            message(chatId);
            flag = 0;
        }
        if (request[0].equals("/stop")) {
            bot.execute(new SendMessage(chatId, "The connection with database is now stopped, you are unauthorized. "
                    + "Type /start for more info"));
            db.stop();
            flag = 0;
        }
        if (flag == 1) {
            bot.execute(new SendMessage(chatId, "That's an unknown command! Type /start for more info"));
        }
    }

    public void message(long chatId) {
        bot.execute(new SendMessage(chatId, "This bot can execute these commands:\n"
                + "/authorize - authorizes you in the system. Required arguments - token, Organization ID, login\n"
                + "/makeTask  - creates a new task. Required arguments - name, description, queueId, performer flag\n"
                + "/getTaskInfo - returns task by ID. Required argument - TaskID\n"
                + "/getMyTasks - returns tasks assigned to you. Required argument - number of tasks per page, number of your page.\n"
                + "/getMyQueues - returns all your queues. No Agruments required\n\n"
                + "Remember, each argument should be in it's own line!\n"
                + "Type /stop to stop the connection with database and unauthorize"));
    }

    public void authorize(long chatId, String[] request) {
        db.start();
        if (request.length < AUTHORIZE_SIZE) {
            bot.execute(new SendMessage(chatId, "That's not the expected amount of arguments\n"
                    + "Type /start to get more info"));
        } else {
            db.insert(chatId, new User(request[1], request[2], request[MAGIC_NUBMER_3]));
            bot.execute(new SendMessage(chatId, "You have been succesfully authorized"));
        }
    }

    public void makeTask(long chatId, String[] request) {
        var user = db.get(chatId);
        if (user.isEmpty()) {
            bot.execute(new SendMessage(chatId, "You are not authorized"));
            return;
        }
        if (request.length < MAKE_TASK_SIZE) {
            bot.execute(new SendMessage(chatId, "That's not the expected amount of arguments\n"
                    + "Type /start to get more info"));
            return;
        }
        Optional<String> created;
        if (request.length > MAKE_TASK_SIZE) {
            created = client.createTask(user.get().getToken(), user.get().getOrg(), request[1], request[2],
                    Optional.of(request[MAGIC_NUBMER_4]), request[MAGIC_NUBMER_3]);
        } else {
            created = client.createTask(user.get().getToken(), user.get().getOrg(), request[1],
                    request[2], Optional.empty(), request[MAGIC_NUBMER_3]);
        }
        if (created.isEmpty()) {
            bot.execute(new SendMessage(chatId, "There is some problem with making new task"));
        } else {
            bot.execute(new SendMessage(chatId, "The new task was made, it's id is " + created.get()));
        }
    }

    public void getTask(long chatId, String[] request) {
        var user = db.get(chatId);
        if (user.isEmpty()) {
            bot.execute(new SendMessage(chatId, "You are not authorized"));
            return;
        }
        if (request.length < 2) {
            bot.execute(new SendMessage(chatId, "That's not the expected amount of arguments\n"
                    + "Type /start to get more info"));
        } else {
            try {
                var task = client.getTask(user.get().getToken(), user.get().getOrg(), request[1]);
                bot.execute(new SendMessage(chatId, "Task name - " + task.getName()));
                bot.execute(new SendMessage(chatId, "Description: " + task.getDescription()));
                bot.execute(new SendMessage(chatId, "Author is " + task.getAuthor()));
                var assignedTo = task.getAssignedTo();
                assignedTo.ifPresent(s -> bot.execute(new SendMessage(chatId, "The task is assigned to " + s)));
                var followers = task.getFollowers();
                if (followers.size() > 0) {
                    bot.execute(new SendMessage(chatId, "Here are the followers:"));
                    for (var follower : followers) {
                        bot.execute(new SendMessage(chatId, follower));
                    }
                }
                var comments = task.getComments();
                if (comments.size() > 0) {
                    bot.execute(new SendMessage(chatId, "Here are the comments: "));
                    for (var comment : comments) {
                        bot.execute(new SendMessage(chatId, comment.getAuthor() + " - '" + comment.getComment() + "'"));
                    }
                }
            } catch (IOException | InterruptedException exc) {
                bot.execute(new SendMessage(chatId, "Something is wrong, may be try again?"));
            } catch (AuthorizationException exc) {
                bot.execute(new SendMessage(chatId, "You are not authorized"));
                bot.execute(new SendMessage(chatId, exc.getMessage()));
            } catch (TrackerException exc) {
                bot.execute(new SendMessage(chatId, "Task not found"));
                bot.execute(new SendMessage(chatId, exc.getMessage()));
            }
        }
    }

    public void getMyTasks(long chatId, String[] request) {
        var user = db.get(chatId);
        if (user.isEmpty()) {
            bot.execute(new SendMessage(chatId, "You are not authorized"));
            return;
        }
        try {
            var tasks = client.getTasksByUser(user.get().getToken(), user.get().getOrg(), user.get().getLogin());
            int pageLen, pageNum, begin, end;
            if (request.length == 2) {
                pageLen = Integer.parseInt(request[1]);
                pageNum = 1;
                begin = 0;
                end = pageLen * pageNum;
                if (tasks.size() < end) {
                    bot.execute(new SendMessage(chatId, "Ooops, that's too many tasks!"));
                    end = tasks.size();
                }
                bot.execute(new SendMessage(chatId, "Here are your last " + String.valueOf(end) + " tasks:"));
            } else {
                if (request.length < 2) {
                    pageNum = 1;
                    if (tasks.size() < DEFAULT_PAGE_SIZE) {
                        pageLen = tasks.size();
                    } else {
                        pageLen = DEFAULT_PAGE_SIZE;
                    }
                    begin = 0;
                    end = pageLen * pageNum;
                    bot.execute(new SendMessage(chatId, "Here are your last " + String.valueOf(pageLen) + " tasks:"));
                } else {
                    pageLen = Integer.parseInt(request[1]);
                    pageNum = Integer.parseInt(request[2]);
                    begin = (pageNum - 1) * pageLen;
                    end = pageLen * pageNum;
                    if (tasks.size() < end) {
                        end = tasks.size();
                    }
                    if (begin + 1 > end) {
                        bot.execute(new SendMessage(chatId, "Theree are no tasks from " + String.valueOf(begin + 1)
                                + " to " + String.valueOf(end)
                                + ". Please, enter valid numbers"));
                    } else {
                        bot.execute(new SendMessage(chatId, "Here are your tasks from " + String.valueOf(begin + 1)
                                + " to " + String.valueOf(end)));
                    }
                }
            }
            for (int i = begin; i < end; i++) {
                bot.execute(new SendMessage(chatId, tasks.get(i)));
            }
        } catch (TrackerException e) {
            bot.execute(new SendMessage(chatId, "Something is wrong, may be try again?"));
        }
    }

    public void getMyQueues(long chatId) {
        try {
            var user = db.get(chatId);
            if (user.isEmpty()) {
                bot.execute(new SendMessage(chatId, "You are not authorized"));
                return;
            }
            var queues = client.getAllQueues(user.get().getToken(), user.get().getOrg());
            bot.execute(new SendMessage(chatId, "Here are all your queues:"));
            for (var q : queues) {
                bot.execute(new SendMessage(chatId, q.getKey() + " with id = " + q.getId()));
            }
        } catch (AuthorizationException | IOException | InterruptedException e) {
            bot.execute(new SendMessage(chatId, "Something is wrong, may be try again?"));
        }
    }

    public TelegrammBot(TelegramBot b, Database d, Client c) {
        bot = b;
        db = d;
        client = c;
    }

    private final TelegramBot bot;
    private final Database db;
    private final Client client;
}
