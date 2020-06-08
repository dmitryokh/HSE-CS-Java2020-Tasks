package ru.hse.cs.java2020.task03;

public class Queue {
    private String key;
    private long id;

    public String getKey() {
        return key;
    }

    public long getId() {
        return id;
    }

    public Queue(String newKey, long newId) {
        this.key = newKey;
        this.id = newId;
    }
}
