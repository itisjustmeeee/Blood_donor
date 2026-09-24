package ru.mirea.project.ui;

public class User {
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final String details;

    public User(String name, String email, String password, Role role, String details) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}