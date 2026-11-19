package com.vaccination.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Vaccination Center.
 */
public class Center {

    private int centerId;
    private String centerName;
    private String address;
    private String city;
    private String phone;
    private boolean active;
    private LocalDateTime createdAt;

    public Center() {
        this.active = true;
    }

    public Center(String centerName, String address, String city, String phone) {
        this.centerName = centerName;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.active = true;
    }

    public Center(int centerId, String centerName, String address, String city, String phone, 
                  boolean active, LocalDateTime createdAt) {
        this.centerId = centerId;
        this.centerName = centerName;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getCenterId() {
        return centerId;
    }

    public void setCenterId(int centerId) {
        this.centerId = centerId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
        return String.format("[%d] %-30s | %-15s | %-25s | Tel: %-10s | %s",
                centerId, centerName, city, address, (phone != null ? phone : "N/A"), (active ? "ACTIVE" : "INACTIVE"));
    }
}
