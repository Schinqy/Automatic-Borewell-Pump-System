package com.luitech.abops;

public class NotificationModel {
    private long id; // Unique identifier for each notification
    private String boardId; // Example field
    private String text; // Example field
    private String timestamp; // Example field

    // Constructor
    public NotificationModel(long id, String boardId, String text, String timestamp) {
        this.id = id;
        this.boardId = boardId;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getBoardId() {
        return boardId;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // Optionally override equals() and hashCode() for comparison in DiffUtil
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NotificationModel that = (NotificationModel) obj;
        return id == that.id; // Compare IDs
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id); // Generate hash code based on ID
    }
}
