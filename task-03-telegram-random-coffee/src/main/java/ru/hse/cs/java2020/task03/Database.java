package ru.hse.cs.java2020.task03;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

public class Database {
    private static final int MAGIC_NUBMER_3 = 3;
    private static final int MAGIC_NUBMER_4 = 4;

    Database(String path) {
        url = "jdbc:sqlite:" + path;
    }

    private Connection connection;
    private final String url;

    void start() {
        try {
            connection = DriverManager.getConnection(url);
            String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                    + " chatId integer PRIMARY KEY,\n"
                    + " token text NOT NULL,\n"
                    + " org text NOT NULL,\n"
                    + " login text NOT NULL\n"
                    + ");";
        } catch (SQLException exc) {
            System.err.println("Problems with connection");
        }
    }

    void stop() {
        try {
            connection.close();
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }

    void insert(long chatId, User u) {
        try {
            var toDelete = connection.createStatement();
            toDelete.execute("DELETE FROM users WHERE chatId=" + chatId);
            var toInsert = connection.prepareStatement("INSERT INTO users(chatId, token, org, login) VALUES(?,?,?,?)");
            toInsert.setInt(1, (int) chatId);
            toInsert.setString(2, u.getToken());
            toInsert.setString(MAGIC_NUBMER_3, u.getOrg());
            toInsert.setString(MAGIC_NUBMER_4, u.getLogin());
            toInsert.executeUpdate();
        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
        }
    }

    Optional<User> get(long chatId) {
        String sql = "SELECT token, org, login FROM users WHERE chatId=" + chatId;
        try {
            var state = connection.createStatement();
            var query = state.executeQuery(sql);
            return Optional.of(new User(query.getString("token"), query.getString("org"),
                    query.getString("login")));
        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
            return Optional.empty();
        }
    }
}
