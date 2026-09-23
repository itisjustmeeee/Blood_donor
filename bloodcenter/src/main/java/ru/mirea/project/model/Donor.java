package ru.mirea.project.model;

import java.time.LocalDate;

public record Donor(int id, String fullName, LocalDate birthDate, String role,
                    String gender, int weight, String email, String phone,
                    String passwordHash, int bloodGroupId, String bloodGroup) {
}
