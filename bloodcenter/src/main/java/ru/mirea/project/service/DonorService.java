package ru.mirea.project.service;

import ru.mirea.project.repository.BloodGroupRepository;
import ru.mirea.project.repository.DonorRepository;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.RegistrationException;
import ru.mirea.project.model.BloodGroup;
import ru.mirea.project.model.Donor;
import ru.mirea.project.security.PasswordHasher;

import java.sql.SQLException;

public class DonorService {
    private static final int MIN_WEIGHT = 50;
    private final DonorRepository donorDao;
    private final BloodGroupRepository bloodGroupDao;

    public DonorService(DonorRepository donorDao, BloodGroupRepository bloodGroupDao) {
        this.donorDao = donorDao;
        this.bloodGroupDao = bloodGroupDao;
    }

    public Donor register(String name, int age, String role, String gender,
                          int weight, String email, String password,
                          int bloodGroupId) throws SQLException, BusinessException {
        if ("donor".equalsIgnoreCase(role) && bloodGroupId <= 0) {
            throw new RegistrationException("Для регистрации донора необходимо указать группу крови и резус-фактор.");
        }
        if (!"male".equalsIgnoreCase(gender) && !"female".equalsIgnoreCase(gender)) {
            throw new RegistrationException("Пол должен быть указан как male или female.");
        }
        if ("donor".equalsIgnoreCase(role) && weight < MIN_WEIGHT) {
            throw new RegistrationException("Вес донора должен быть не менее 50 кг.");
        }
        if (age < 18) {
            throw new RegistrationException("Возраст пользователя должен быть не менее 18 лет.");
        }
        if (donorDao.findByEmail(email) != null) {
            throw new RegistrationException("Пользователь с таким email уже зарегистрирован.");
        }
        if (bloodGroupId > 0 && bloodGroupDao.findAll().stream()
                .noneMatch(group -> group.id() == bloodGroupId)) {
            throw new RegistrationException("Выбранная группа крови и резус-фактор не существуют.");
        }
        return donorDao.create(name, age, role, gender, weight, email,
                PasswordHasher.hash(password), bloodGroupId);
    }

    public Donor authenticate(String email, String password) throws SQLException {
        Donor donor = donorDao.findByEmail(email);
        return donor != null && PasswordHasher.matches(password, donor.passwordHash()) ? donor : null;
    }

    public Donor updateProfile(Donor donor, int age, String gender, int weight)
            throws SQLException, BusinessException {
        if (age < 18) {
            throw new RegistrationException("Возраст пользователя должен быть не менее 18 лет.");
        }
        if (!"male".equalsIgnoreCase(gender) && !"female".equalsIgnoreCase(gender)) {
            throw new RegistrationException("Пол должен быть указан как мужской или женский.");
        }
        if (weight < MIN_WEIGHT) {
            throw new RegistrationException("Вес донора должен быть не менее 50 кг.");
        }
        return donorDao.updateProfile(donor.id(), age, gender, weight);
    }
}
