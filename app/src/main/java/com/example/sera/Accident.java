package com.example.sera;

import java.util.Map;

public class Accident {
    private String userId;
    private String name;
    private String history;
    private Map<String, Object> location;
    private long timestamp;

    public Accident() {
        // Required for Firebase
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getHistory() {
        return history;
    }

    public Map<String, Object> getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
