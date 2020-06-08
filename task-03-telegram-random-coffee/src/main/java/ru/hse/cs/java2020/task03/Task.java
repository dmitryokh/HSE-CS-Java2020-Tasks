package ru.hse.cs.java2020.task03;

import java.util.ArrayList;
import java.util.Optional;

public class Task {
    private Optional<String> assignedTo;
    private String name;
    private String description;
    private String author;
    private ArrayList<String> followers;
    private ArrayList<Comment> comments;

    public void setName(String n) {
        this.name = n;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public void setAuthor(String a) {
        this.author = a;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    void addComment(Comment i) {
        comments.add(i);
    }

    void addFollower(String f) {
        followers.add(f);
    }

    public ArrayList<String> getFollowers() {
        return followers;
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public Optional<String> getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assigned) {
        this.assignedTo = Optional.ofNullable(assigned);
    }

    public Task() {
        followers = new ArrayList<>();
        comments = new ArrayList<>();
    }
}
