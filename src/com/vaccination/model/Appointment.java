package com.vaccination.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Domain entity representing an appointment reservation.
 * Includes joined presentation fields for clear display.
 */
public class Appointment {

    private long appointmentId;
    private int userId;
    private int slotId;
    private LocalDateTime bookingTime;
    private AppointmentStatus status;

    // Joined presentation fields
    private String userName;
    private String userEmail;
    private String userPhone;
    private String centerName;
    private String centerCity;
    private String centerAddress;
    private String driveName;
    private String vaccineName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;

    public Appointment() {
        this.status = AppointmentStatus.BOOKED;
    }

    public Appointment(int userId, int slotId) {
        this.userId = userId;
        this.slotId = slotId;
        this.status = AppointmentStatus.BOOKED;
        this.bookingTime = LocalDateTime.now();
    }

    public Appointment(long appointmentId, int userId, int slotId, LocalDateTime bookingTime, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.slotId = slotId;
        this.bookingTime = bookingTime;
        this.status = status;
    }

    public long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
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

    public String getCenterAddress() {
        return centerAddress;
    }

    public void setCenterAddress(String centerAddress) {
        this.centerAddress = centerAddress;
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
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

    @Override
    public String toString() {
        return String.format("Appt #%-5d | %s | %s (%s) | %s | %s %s-%s | Status: %s",
                appointmentId, (userName != null ? userName : "User #" + userId),
                (centerName != null ? centerName : "Center"),
                (centerCity != null ? centerCity : ""),
                (vaccineName != null ? vaccineName : "Vaccine"),
                slotDate, startTime, endTime, status);
    }
}
