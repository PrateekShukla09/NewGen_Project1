package com.vaccination.service;

import com.vaccination.dao.CenterDAO;
import com.vaccination.exception.AppException;
import com.vaccination.exception.ValidationException;
import com.vaccination.model.Center;
import com.vaccination.util.ValidationUtil;

import java.util.List;

/**
 * Service managing Vaccination Centers and business validation invariants.
 */
public class CenterService {

    private final CenterDAO centerDAO;

    public CenterService() {
        this.centerDAO = new CenterDAO();
    }

    public CenterService(CenterDAO centerDAO) {
        this.centerDAO = centerDAO;
    }

    public Center addCenter(String name, String address, String city, String phone) throws AppException {
        ValidationUtil.validateName(name, "Center name");
        ValidationUtil.validateNotBlank(address, "Address");
        ValidationUtil.validateNotBlank(city, "City");
        if (phone != null && !phone.isBlank()) {
            ValidationUtil.validatePhone(phone);
        }

        Center center = new Center(name.trim(), address.trim(), city.trim(), (phone != null ? phone.trim() : null));
        return centerDAO.insert(center);
    }

    public List<Center> getAllCenters() throws AppException {
        return centerDAO.findAll();
    }

    public List<Center> getActiveCenters() throws AppException {
        return centerDAO.findAllActive();
    }

    public List<Center> searchCenters(String query) throws AppException {
        ValidationUtil.validateNotBlank(query, "Search term");
        return centerDAO.searchByNameOrCity(query);
    }

    public Center getCenterById(int centerId) throws AppException {
        return centerDAO.findById(centerId)
                .orElseThrow(() -> new ValidationException("Vaccination Center #" + centerId + " does not exist."));
    }

    public void updateCenter(int centerId, String name, String address, String city, String phone, boolean active) throws AppException {
        Center existing = getCenterById(centerId);

        ValidationUtil.validateName(name, "Center name");
        ValidationUtil.validateNotBlank(address, "Address");
        ValidationUtil.validateNotBlank(city, "City");
        if (phone != null && !phone.isBlank()) {
            ValidationUtil.validatePhone(phone);
        }

        existing.setCenterName(name.trim());
        existing.setAddress(address.trim());
        existing.setCity(city.trim());
        existing.setPhone(phone != null ? phone.trim() : null);
        existing.setActive(active);

        centerDAO.update(existing);
    }

    public void toggleActiveStatus(int centerId, boolean active) throws AppException {
        getCenterById(centerId); // Verify existence
        centerDAO.setActiveStatus(centerId, active);
    }
}
