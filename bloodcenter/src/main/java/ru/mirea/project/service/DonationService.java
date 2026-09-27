package ru.mirea.project.service;

import ru.mirea.project.repository.*;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DonationEligibilityException;
import ru.mirea.project.exception.BloodBatchException;
import ru.mirea.project.model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class DonationService {
    private static final int MIN_WEIGHT = 50;
    private static final int MIN_AGE = 18;
    private final DonationRequestRepository requestDao;
    private final MedicalExaminationRepository examinationDao;
    private final DonationRepository donationDao;
    private final BloodBatchRepository batchDao;

    public DonationService(DonationRequestRepository requestDao, MedicalExaminationRepository examinationDao,
            DonationRepository donationDao, BloodBatchRepository batchDao) {
        this.requestDao = requestDao;
        this.examinationDao = examinationDao;
        this.donationDao = donationDao;
        this.batchDao = batchDao;
    }

    public DonationRequest bookExamination(Donor donor, LocalDate date, BigDecimal hemoglobin)
            throws SQLException, BusinessException {
        validateDonor(donor);
        validateHemoglobin(donor, hemoglobin);
        if (requestDao.hasPendingRequest(donor.id())) {
            throw new DonationEligibilityException("У донора уже есть нерассмотренная заявка.");
        }
        DonationRequest request = requestDao.create(donor.id(), date, "created");
        examinationDao.create(request.id(), date, hemoglobin);
        return request;
    }

    private void validateHemoglobin(Donor donor, BigDecimal hemoglobin) throws BusinessException {
        if (hemoglobin == null) {
            throw new DonationEligibilityException("Необходимо указать гемоглобин для записи на обследование.");
        }
        BigDecimal min = "male".equalsIgnoreCase(donor.gender())
                ? BigDecimal.valueOf(130)
                : BigDecimal.valueOf(120);
        BigDecimal max = "male".equalsIgnoreCase(donor.gender())
                ? BigDecimal.valueOf(160)
                : BigDecimal.valueOf(140);
        if (hemoglobin.compareTo(min) < 0 || hemoglobin.compareTo(max) > 0) {
            throw new DonationEligibilityException("Гемоглобин вне нормы: "
                    + min + "-" + max + " г/л.");
        }
    }

    public DonationRequest bookDonation(Donor donor, LocalDate date)
            throws SQLException, BusinessException {
        validateDonor(donor);
        List<Integer> usedExaminationIds = donationDao.findByDonor(donor.id()).stream()
                .map(Donation::examinationId)
                .toList();
        List<MedicalExamination> accepted = examinationDao.findByDonor(donor.id()).stream()
                .filter(item -> "accepted".equalsIgnoreCase(item.admissionStatus()))
                .filter(item -> !usedExaminationIds.contains(item.id()))
                .toList();
        if (accepted.isEmpty()) {
            throw new DonationEligibilityException(
                    "Для записи на донацию необходимо одно положительное обследование без донации.");
        }
        LocalDate lastExamination = accepted.stream()
                .map(MedicalExamination::examinationDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
        if (date.isBefore(lastExamination)) {
            throw new DonationEligibilityException("Дата донации не может быть раньше даты медицинского обследования.");
        }
        if (requestDao.hasPendingRequest(donor.id())) {
            throw new DonationEligibilityException("У донора уже есть нерассмотренная заявка на донацию.");
        }
        return requestDao.create(donor.id(), date, "created");
    }

    public Donation process(DonationRequest request, MedicalExamination examination,
            BloodBatch batch, int volume, String type, String result,
            Donor donor) throws SQLException, BusinessException {
        validateDonor(donor);
        if (!"accepted".equalsIgnoreCase(examination.admissionStatus())) {
            throw new DonationEligibilityException("К донации допускаются только после положительного обследования.");
        }
        if (!request.status().equalsIgnoreCase("created")
                && !request.status().equalsIgnoreCase("confirmed")) {
            throw new DonationEligibilityException("Эта заявка уже обработана или недоступна.");
        }
        if (!donor.bloodGroup().equalsIgnoreCase(batch.bloodGroup())) {
            throw new BloodBatchException("Группа крови партии не соответствует группе крови донора.");
        }
        if (examination.examinationDate().isAfter(request.donationDate())) {
            throw new DonationEligibilityException("Дата донации не может быть раньше даты обследования.");
        }
        if (!batch.expirationDate().isAfter(batch.preparationDate())) {
            throw new BloodBatchException("Срок годности партии должен быть позже даты заготовки.");
        }
        if (volume <= 0) {
            throw new BloodBatchException("Количество крови в донации должно быть больше нуля.");
        }
        if (volume > 450) {
            throw new BloodBatchException("За одну донацию можно взять не более 450 мл.");
        }
        Donation donation = donationDao.create(request.id(), examination.id(), batch.id(),
                request.donationDate(), volume, type, result);
        requestDao.updateStatus(request.id(), "completed");
        return donation;
    }

    private void validateDonor(Donor donor) throws BusinessException {
        if (donor.weight() < MIN_WEIGHT) {
            throw new DonationEligibilityException("К донации допускаются доноры весом не менее 50 кг.");
        }
        if (donor.age() < MIN_AGE) {
            throw new DonationEligibilityException("К донации допускаются только лица старше 18 лет.");
        }
    }
}
