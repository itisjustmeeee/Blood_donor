package ru.mirea.project.model;

import java.time.LocalDate;

public record BloodBatch(int id, int bloodGroupId, String batchNumber,
                         LocalDate preparationDate, LocalDate expirationDate,
                         int totalVolume, String status, String bloodGroup) {
}
