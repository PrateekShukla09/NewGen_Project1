package com.vaccination.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing an educational vaccine catalog item.
 */
public class Vaccine {

    private int vaccineId;
    private String vaccineName;
    private String manufacturer;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    public Vaccine() {
        this.active = true;
    }

    public Vaccine(String vaccineName, String manufacturer, String description) {
        this.vaccineName = vaccineName;
        this.manufacturer = manufacturer;
        this.description = description;
        this.active = true;
    }

    public Vaccine(int vaccineId, String vaccineName, String manufacturer, String description, 
                   boolean active, LocalDateTime createdAt) {
        this.vaccineId = vaccineId;
        this.vaccineName = vaccineName;
        this.manufacturer = manufacturer;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getVaccineId() {
        return vaccineId;
    }

    public void setVaccineId(int vaccineId) {
        this.vaccineId = vaccineId;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%d] %-20s | Mfr: %-18s | %-35s | %s",
                vaccineId, vaccineName, (manufacturer != null ? manufacturer : "N/A"), 
                (description != null ? description : ""), (active ? "ACTIVE" : "INACTIVE"));
    }
}
