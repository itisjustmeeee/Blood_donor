package ru.mirea.project.ui;

public class MedicalInstitution {
    private static long nextId = 1;

    private final long institutionId;
    private final String name;
    private final String address;
    private final String phone;

    public MedicalInstitution(String name, String address, String phone) {
        institutionId = nextId++;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public long getInstitutionId() {
        return institutionId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " | адрес: " + address + " | телефон: " + phone;
    }
}