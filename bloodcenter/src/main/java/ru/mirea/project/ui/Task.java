package ru.mirea.project.ui;

public class Task {
    private final String text;
    private boolean completed;

    public Task(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getText() {
        return text;
    }

    public void complete() {
        completed = true;
    }

    @Override
    public String toString() {
        return (completed ? "[Выполнено] " : "[В работе] ") + text;
    }
}