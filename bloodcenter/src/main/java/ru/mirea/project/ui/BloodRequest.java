package ru.mirea.project.ui;

public class BloodRequest {
    private final User clinic;
    private final MedicalInstitution institution;
    private final String bloodGroup;
    private final String quantity;
    private final String urgency;
    private final String date;

    public BloodRequest(User clinic, MedicalInstitution institution, String bloodGroup,
            String quantity, String urgency, String date) {
        this.clinic = clinic;
        this.institution = institution;
        this.bloodGroup = bloodGroup;
        this.quantity = quantity;
        this.urgency = urgency;
        this.date = date;
    }

    public User getClinic() {
        return clinic;
    }

    public MedicalInstitution getInstitution() {
        return institution;
    }

    @Override
    public String toString() {
        return "Группа: " + bloodGroup + ", количество: " + quantity
                + ", срочность: " + urgency + ", дата: " + date;
    }
}