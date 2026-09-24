package ru.mirea.project.ui;

public class DonationAppointment {
    private final User donor;
    private final String date;
    private final String time;
    private final String clinic;
    private String status;

    public DonationAppointment(User donor, String date, String time, String clinic) {
        this.donor = donor;
        this.date = date;
        this.time = time;
        this.clinic = clinic;
        this.status = "Запланирована";
    }

    public User getDonor() {
        return donor;
    }

    @Override
    public String toString() {
        return donor.getName() + ": " + date + " в " + time + ", " + clinic
                + " (" + status + ")";
    }
}