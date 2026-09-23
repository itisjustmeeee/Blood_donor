package ru.mirea.project.model;

import java.time.LocalDate;

public record Donation(int id, int examinationId, int batchId, LocalDate donationDate,
                       int bloodVolume, String donationType, String result,
                       String donorName, String bloodGroup) {
}
