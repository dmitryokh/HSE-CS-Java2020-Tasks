package ru.hse.cs.java2020.task03;

public class Comment {
    private String author;
    private String comment;

    public String getAuthor() {
        return author;
    }

    public String getComment() {
        return comment;
    }

    public Comment(String a, String c) {
        this.author = a;
        this.comment = c;
    }
}
