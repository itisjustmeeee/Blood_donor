package ru.mirea.project.service;

import ru.mirea.project.repository.DonationRequestRepository;
import ru.mirea.project.repository.DonorRepository;
import ru.mirea.project.repository.MedicalExaminationRepository;
import ru.mirea.project.model.Donor;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.exception.ExaminationValidationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import ru.mirea.project.model.MedicalExamination;

public class MedicalExaminationService {
    private final MedicalExaminationRepository examinationDao;
    private final DonationRequestRepository requestDao;
    private final DonorRepository donorDao;

    public MedicalExaminationService(MedicalExaminationRepository examinationDao,
                                     DonationRequestRepository requestDao, DonorRepository donorDao) {
        this.examinationDao = examinationDao;
        this.requestDao = requestDao;
        this.donorDao = donorDao;
    }

    public void process(int examinationId, int requestId, BigDecimal hemoglobin,
                        String pressure, String conclusion, String status)
            throws SQLException, BusinessException {
        if (!"accepted".equalsIgnoreCase(status) && !"rejected".equalsIgnoreCase(status)) {
            throw new ExaminationValidationException("Недопустимый статус медицинского обследования.");
        }
        if (hemoglobin == null || hemoglobin.signum() <= 0) {
            throw new ExaminationValidationException("Гемоглобин должен быть больше нуля.");
        }
        var request = requestDao.findById(requestId);
        if (request == null) {
            throw new EntityNotFoundException("Заявка на обследование не найдена.");
        }
        Donor donor = donorDao.findById(request.donorId());
        if (donor == null) {
            throw new EntityNotFoundException("Донор обследования не найден.");
        }
        if ("accepted".equalsIgnoreCase(status) && !isNormal(hemoglobin, donor.gender())) {
            String range = "male".equalsIgnoreCase(donor.gender()) ? "130-160" : "120-140";
            throw new ExaminationValidationException("Гемоглобин вне нормы для пола донора. Норма: " + range + " г/л.");
        }
        examinationDao.updateResult(examinationId, hemoglobin, pressure, conclusion, status);
    }

    public void updateDateForDonor(int examinationId, Donor donor, LocalDate date)
            throws SQLException, BusinessException {
        MedicalExamination examination = findOwned(examinationId, donor);
        var request = requestDao.findById(examination.requestId());
        if (request == null) {
            throw new EntityNotFoundException("Заявка на обследование не найдена.");
        }
        if (!"pending".equalsIgnoreCase(examination.admissionStatus())
                || "completed".equalsIgnoreCase(request.status())
                || "cancelled".equalsIgnoreCase(request.status())) {
            throw new ExaminationValidationException(
                    "Изменять дату можно только у активной записи без вынесенного заключения.");
        }
        examinationDao.updateDate(examinationId, date);
    }

    public void deleteForDonor(int examinationId, Donor donor)
            throws SQLException, BusinessException {
        findOwned(examinationId, donor);
        examinationDao.deleteWithRequest(examinationId);
    }

    public void updateDateForDoctor(int examinationId, LocalDate date)
            throws SQLException, BusinessException {
        MedicalExamination examination = examinationDao.findById(examinationId);
        if (examination == null) {
            throw new EntityNotFoundException("Медицинское обследование не найдено.");
        }
        var request = requestDao.findById(examination.requestId());
        if (request == null) {
            throw new EntityNotFoundException("Заявка на обследование не найдена.");
        }
        Donor donor = donorDao.findById(request.donorId());
        if (donor == null) {
            throw new EntityNotFoundException("Донор обследования не найден.");
        }
        examinationDao.updateDate(examinationId, date);
    }

    public void updateResultForDoctor(int examinationId, BigDecimal hemoglobin,
                                      String pressure, String conclusion, String status)
            throws SQLException, BusinessException {
        MedicalExamination examination = examinationDao.findById(examinationId);
        if (examination == null) {
            throw new EntityNotFoundException("Медицинское обследование не найдено.");
        }
        process(examination.id(), examination.requestId(), hemoglobin, pressure, conclusion, status);
    }

    public void deleteForDoctor(int examinationId)
            throws SQLException, BusinessException {
        if (examinationDao.findById(examinationId) == null) {
            throw new EntityNotFoundException("Медицинское обследование не найдено.");
        }
        examinationDao.deleteWithRequest(examinationId);
    }

    private MedicalExamination findOwned(int examinationId, Donor donor)
            throws SQLException, EntityNotFoundException {
        MedicalExamination examination = examinationDao.findByIdAndDonor(examinationId, donor.id());
        if (examination == null) {
            throw new EntityNotFoundException("Обследование не найдено у текущего донора.");
        }
        return examination;
    }

    private boolean isNormal(BigDecimal hemoglobin, String gender) {
        BigDecimal min = "male".equalsIgnoreCase(gender) ? BigDecimal.valueOf(130) : BigDecimal.valueOf(120);
        BigDecimal max = "male".equalsIgnoreCase(gender) ? BigDecimal.valueOf(160) : BigDecimal.valueOf(140);
        return hemoglobin.compareTo(min) >= 0 && hemoglobin.compareTo(max) <= 0;
    }
}
