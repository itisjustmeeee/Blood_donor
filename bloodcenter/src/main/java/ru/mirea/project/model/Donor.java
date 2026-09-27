package ru.mirea.project.model;

public record Donor(
        int id,
        String fullName,
        int age,
        String role,
        String gender,
        int weight,
        String email,
        String passwordHash,
        int bloodGroupId,
        String bloodGroup) {
}
