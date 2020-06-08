package ru.hse.cs.java2020.task03;

import java.util.Objects;

public class User {
    private String token;
    private String org;
    private String login;

    public String getToken() {
        return token;
    }

    public String getOrg() {
        return org;
    }

    public String getLogin() {
        return login;
    }

    public User(String newToken, String newOrg, String newLogin) {
        this.token = newToken;
        this.org = newOrg;
        this.login = newLogin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, org, login);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return token.equals(user.token) && org.equals(user.org) && login.equals(user.login);
    }
}
