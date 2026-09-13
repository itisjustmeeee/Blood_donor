package ru.mirea.project.ui;

public class Notification {
    private final User recipient;
    private final String message;
    private boolean read;

    public Notification(User recipient, String message) {
        this.recipient = recipient;
        this.message = message;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void markAsRead() {
        read = true;
    }
}