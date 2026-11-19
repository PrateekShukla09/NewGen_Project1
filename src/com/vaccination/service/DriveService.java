package com.vaccination.service;

import com.vaccination.dao.CenterDAO;
import com.vaccination.dao.DriveDAO;
import com.vaccination.dao.VaccineDAO;
import com.vaccination.exception.AppException;
import com.vaccination.exception.ValidationException;
import com.vaccination.model.Center;
import com.vaccination.model.Drive;
import com.vaccination.model.Vaccine;
import com.vaccination.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Service managing Vaccination Drive lifecycle, scheduling dates,
 * and enforcing active Center and Vaccine referential integrity.
 */
public class DriveService {

    private final DriveDAO driveDAO;
    private final CenterDAO centerDAO;
    private final VaccineDAO vaccineDAO;

    public DriveService() {
        this.driveDAO = new DriveDAO();
        this.centerDAO = new CenterDAO();
        this.vaccineDAO = new VaccineDAO();
    }

    public DriveService(DriveDAO driveDAO, CenterDAO centerDAO, VaccineDAO vaccineDAO) {
        this.driveDAO = driveDAO;
        this.centerDAO = centerDAO;
        this.vaccineDAO = vaccineDAO;
    }

    public Drive createDrive(String driveName, int centerId, int vaccineId, 
                             LocalDate startDate, LocalDate endDate) throws AppException {
        ValidationUtil.validateName(driveName, "Drive name");
        ValidationUtil.validateDateRange(startDate, endDate);

        // Verify Center exists and is active
        Center center = centerDAO.findById(centerId)
                .orElseThrow(() -> new ValidationException("Vaccination Center #" + centerId + " does not exist."));
        if (!center.isActive()) {
            throw new ValidationException("Cannot assign drive to inactive center: '" + center.getCenterName() + "'.");
        }

        // Verify Vaccine exists and is active
        Vaccine vaccine = vaccineDAO.findById(vaccineId)
                .orElseThrow(() -> new ValidationException("Vaccine #" + vaccineId + " does not exist."));
        if (!vaccine.isActive()) {
            throw new ValidationException("Cannot schedule drive with inactive vaccine: '" + vaccine.getVaccineName() + "'.");
        }

        Drive drive = new Drive(driveName.trim(), centerId, vaccineId, startDate, endDate);
        return driveDAO.insert(drive);
    }

    public List<Drive> getAllDrives() throws AppException {
        return driveDAO.findAll();
    }

    public List<Drive> getActiveDrives() throws AppException {
        return driveDAO.findAllActive();
    }

    public List<Drive> getUpcomingDrives() throws AppException {
        return driveDAO.findUpcoming(LocalDate.now());
    }

    public List<Drive> getCurrentActiveDrives() throws AppException {
        return driveDAO.findCurrentActive(LocalDate.now());
    }

    public List<Drive> getCompletedDrives() throws AppException {
        return driveDAO.findCompleted(LocalDate.now());
    }

    public List<Drive> getDrivesByCity(String city) throws AppException {
        ValidationUtil.validateNotBlank(city, "City name");
        return driveDAO.findByCity(city);
    }

    public List<Drive> getDrivesByCenter(int centerId) throws AppException {
        return driveDAO.findByCenterId(centerId);
    }

    public Drive getDriveById(int driveId) throws AppException {
        return driveDAO.findById(driveId)
                .orElseThrow(() -> new ValidationException("Vaccination Drive #" + driveId + " does not exist."));
    }

    public void updateDrive(int driveId, String driveName, int centerId, int vaccineId, 
                            LocalDate startDate, LocalDate endDate, boolean active) throws AppException {
        Drive existing = getDriveById(driveId);

        ValidationUtil.validateName(driveName, "Drive name");
        ValidationUtil.validateDateRange(startDate, endDate);

        Center center = centerDAO.findById(centerId)
                .orElseThrow(() -> new ValidationException("Vaccination Center #" + centerId + " does not exist."));
        if (!center.isActive() && active) {
            throw new ValidationException("Cannot set drive active while assigned center is inactive.");
        }

        Vaccine vaccine = vaccineDAO.findById(vaccineId)
                .orElseThrow(() -> new ValidationException("Vaccine #" + vaccineId + " does not exist."));
        if (!vaccine.isActive() && active) {
            throw new ValidationException("Cannot set drive active while assigned vaccine is inactive.");
        }

        existing.setDriveName(driveName.trim());
        existing.setCenterId(centerId);
        existing.setVaccineId(vaccineId);
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);
        existing.setActive(active);

        driveDAO.update(existing);
    }

    public void toggleActiveStatus(int driveId, boolean active) throws AppException {
        getDriveById(driveId); // Verify existence
        driveDAO.setActiveStatus(driveId, active);
    }
}
