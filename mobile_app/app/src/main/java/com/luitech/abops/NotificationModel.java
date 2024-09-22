package com.luitech.abops;


public class NotificationModel {
    private String board_id; // Corresponds to 'board_id'
    private String text; // Corresponds to 'message'
    private String timestamp;

    // Constructor
    public NotificationModel(String board_id, String text, String timestamp) {
        this.board_id = board_id;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters
    public String getBoardId() {
        return board_id;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
