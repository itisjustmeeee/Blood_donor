package ru.mirea.project.model;

import java.time.LocalDate;

public record DonationRequest(int id, int donorId, LocalDate donationDate,
                               String status, String donorName) {
}
