package com.vaccination.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain entity representing a scheduled Vaccination Drive.
 * Associates a Center with a Vaccine over a specific date range.
 */
public class Drive {

    private int driveId;
    private String driveName;
    private int centerId;
    private int vaccineId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private LocalDateTime createdAt;

    // Joined presentation fields for display convenience
    private String centerName;
    private String centerCity;
    private String vaccineName;

    public Drive() {
        this.active = true;
    }

    public Drive(String driveName, int centerId, int vaccineId, LocalDate startDate, LocalDate endDate) {
        this.driveName = driveName;
        this.centerId = centerId;
        this.vaccineId = vaccineId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
    }

    public Drive(int driveId, String driveName, int centerId, int vaccineId, LocalDate startDate, 
                 LocalDate endDate, boolean active, LocalDateTime createdAt) {
        this.driveId = driveId;
        this.driveName = driveName;
        this.centerId = centerId;
        this.vaccineId = vaccineId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getDriveId() {
        return driveId;
    }

    public void setDriveId(int driveId) {
        this.driveId = driveId;
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
    }

    public int getCenterId() {
        return centerId;
    }

    public void setCenterId(int centerId) {
        this.centerId = centerId;
    }

    public int getVaccineId() {
        return vaccineId;
    }

    public void setVaccineId(int vaccineId) {
        this.vaccineId = vaccineId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getCenterCity() {
        return centerCity;
    }

    public void setCenterCity(String centerCity) {
        this.centerCity = centerCity;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }

    @Override
    public String toString() {
        String centerStr = (centerName != null) ? centerName + " (" + centerCity + ")" : "Center #" + centerId;
        String vaxStr = (vaccineName != null) ? vaccineName : "Vaccine #" + vaccineId;
        return String.format("[%d] %-30s | Center: %-25s | Vax: %-15s | %s to %s | %s",
                driveId, driveName, centerStr, vaxStr, startDate, endDate, (active ? "ACTIVE" : "INACTIVE"));
    }
}
