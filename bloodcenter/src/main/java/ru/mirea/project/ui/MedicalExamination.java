package ru.mirea.project.ui;

public class MedicalExamination {
    private final User donor;
    private final MedicalInstitution institution;
    private final String date;
    private String bloodGroup;
    private String rhesus;
    private String hemoglobin;
    private String pressure;
    private String conclusion;
    private String donationDecision;

    public MedicalExamination(User donor, MedicalInstitution institution, String date) {
        this.donor = donor;
        this.institution = institution;
        this.date = date;
        this.donationDecision = "Ожидает решения";
    }

    public User getDonor() {
        return donor;
    }

    public String getDate() {
        return date;
    }

    public MedicalInstitution getInstitution() {
        return institution;
    }

    public String getBloodGroup() {
        return bloodGroup == null ? "не указана" : bloodGroup;
    }

    public String getRhesus() {
        return rhesus == null ? "не указан" : rhesus;
    }

    public String getHemoglobin() {
        return hemoglobin == null ? "не указан" : hemoglobin;
    }

    public String getPressure() {
        return pressure == null ? "не указано" : pressure;
    }

    public String getConclusion() {
        return conclusion == null ? "результат не внесён" : conclusion;
    }

    public String getDonationDecision() {
        return donationDecision;
    }

    public boolean isApproved() {
        return "Одобрено".equals(donationDecision);
    }

    public void setResult(String bloodGroup, String rhesus, String hemoglobin,
            String pressure, String conclusion) {
        this.bloodGroup = bloodGroup;
        this.rhesus = rhesus;
        this.hemoglobin = hemoglobin;
        this.pressure = pressure;
        this.conclusion = conclusion;
    }

    public void setDonationDecision(String donationDecision) {
        this.donationDecision = donationDecision;
    }
}