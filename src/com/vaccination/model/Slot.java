package com.vaccination.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Domain entity representing an appointment time slot.
 */
public class Slot {

    private int slotId;
    private int driveId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int capacity;
    private LocalDateTime createdAt;

    // Joined presentation and calculation attributes
    private int bookedCount;
    private String driveName;
    private String centerName;
    private String centerCity;
    private String vaccineName;

    public Slot() {
    }

    public Slot(int driveId, LocalDate slotDate, LocalTime startTime, LocalTime endTime, int capacity) {
        this.driveId = driveId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
    }

    public Slot(int slotId, int driveId, LocalDate slotDate, LocalTime startTime, LocalTime endTime, 
                int capacity, LocalDateTime createdAt) {
        this.slotId = slotId;
        this.driveId = driveId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.createdAt = createdAt;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public int getDriveId() {
        return driveId;
    }

    public void setDriveId(int driveId) {
        this.driveId = driveId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(int bookedCount) {
        this.bookedCount = bookedCount;
    }

    public int getRemainingCapacity() {
        return Math.max(0, capacity - bookedCount);
    }

    public boolean isFull() {
        return getRemainingCapacity() <= 0;
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
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
        String avail = isFull() ? "[FULL]" : String.format("[%d/%d left]", getRemainingCapacity(), capacity);
        return String.format("Slot #%-4d | Date: %s | Time: %s - %s | Cap: %2d (Booked: %2d) %s",
                slotId, slotDate, startTime, endTime, capacity, bookedCount, avail);
    }
}
