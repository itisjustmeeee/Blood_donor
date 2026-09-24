package ru.mirea.project.model;

public record BloodGroup(int id, String bloodType, String rhFactor) {
    @Override
    public String toString() {
        return bloodType + rhFactor;
    }
}
