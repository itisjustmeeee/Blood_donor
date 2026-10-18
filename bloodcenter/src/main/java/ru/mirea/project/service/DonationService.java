package ru.mirea.project.service;

import ru.mirea.project.repository.*;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DonationEligibilityException;
import ru.mirea.project.exception.BloodBatchException;
import ru.mirea.project.model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

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

    public DonationRequest bookExamination(Donor donor, LocalDate date)
            throws SQLException, BusinessException {
        validateDonor(donor);
        List<DonationRequest> requests = requestDao.findByDonor(donor.id());
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id());
        DonationRequest request = requests.stream()
                .filter(item -> "created".equalsIgnoreCase(item.status()))
                .filter(item -> examinations.stream()
                        .filter(examination -> examination.requestId() == item.id())
                        .allMatch(examination ->
                                "cancelled".equalsIgnoreCase(examination.admissionStatus())))
                .findFirst()
                .orElseThrow(() -> new DonationEligibilityException(
                        "Сначала создайте заявку на донацию со статусом «создано»."));
        if (date.isBefore(request.donationDate())) {
            throw new DonationEligibilityException(
                    "Дата обследования не может быть раньше даты записи на донацию.");
        }
        MedicalExamination cancelledExamination = examinations.stream()
                .filter(examination -> examination.requestId() == request.id())
                .filter(examination -> "cancelled".equalsIgnoreCase(examination.admissionStatus()))
                .findFirst()
                .orElse(null);
        if (cancelledExamination == null) {
            examinationDao.create(request.id(), date, null);
        } else {
            examinationDao.reopenCancelled(cancelledExamination.id(), date);
        }
        requestDao.updateStatus(request.id(), "confirmed");
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
