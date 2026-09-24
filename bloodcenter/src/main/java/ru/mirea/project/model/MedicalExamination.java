package ru.mirea.project.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicalExamination(int id, int requestId, LocalDate examinationDate,
                                 BigDecimal hemoglobin, String bloodPressure,
                                 String conclusion, String admissionStatus,
                                 String donorName) {
}
