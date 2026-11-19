package com.vaccination.service;

import com.vaccination.dao.VaccineDAO;
import com.vaccination.exception.AppException;
import com.vaccination.exception.ValidationException;
import com.vaccination.model.Vaccine;
import com.vaccination.util.ValidationUtil;

import java.util.List;

/**
 * Service managing educational vaccine catalog entries and validation rules.
 */
public class VaccineService {

    private final VaccineDAO vaccineDAO;

    public VaccineService() {
        this.vaccineDAO = new VaccineDAO();
    }

    public VaccineService(VaccineDAO vaccineDAO) {
        this.vaccineDAO = vaccineDAO;
    }

    public Vaccine addVaccine(String name, String manufacturer, String description) throws AppException {
        ValidationUtil.validateName(name, "Vaccine name");

        Vaccine vaccine = new Vaccine(
            name.trim(),
            (manufacturer != null && !manufacturer.isBlank() ? manufacturer.trim() : null),
            (description != null && !description.isBlank() ? description.trim() : null)
        );
        return vaccineDAO.insert(vaccine);
    }

    public List<Vaccine> getAllVaccines() throws AppException {
        return vaccineDAO.findAll();
    }

    public List<Vaccine> getActiveVaccines() throws AppException {
        return vaccineDAO.findAllActive();
    }

    public List<Vaccine> searchVaccines(String query) throws AppException {
        ValidationUtil.validateNotBlank(query, "Search term");
        return vaccineDAO.searchByName(query);
    }

    public Vaccine getVaccineById(int vaccineId) throws AppException {
        return vaccineDAO.findById(vaccineId)
                .orElseThrow(() -> new ValidationException("Vaccine record #" + vaccineId + " does not exist."));
    }

    public void updateVaccine(int vaccineId, String name, String manufacturer, String description, boolean active) throws AppException {
        Vaccine existing = getVaccineById(vaccineId);
        ValidationUtil.validateName(name, "Vaccine name");

        existing.setVaccineName(name.trim());
        existing.setManufacturer(manufacturer != null && !manufacturer.isBlank() ? manufacturer.trim() : null);
        existing.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        existing.setActive(active);

        vaccineDAO.update(existing);
    }

    public void toggleActiveStatus(int vaccineId, boolean active) throws AppException {
        getVaccineById(vaccineId); // Verify existence
        vaccineDAO.setActiveStatus(vaccineId, active);
    }
}
