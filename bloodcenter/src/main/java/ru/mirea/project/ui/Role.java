package ru.mirea.project.ui;

public enum Role {
    DONOR("Донор"),
    DOCTOR("Врач");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}