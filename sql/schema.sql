-- =====================================================================
-- DATABASE SCHEMA: Vaccination Drive Scheduler
-- Database Management System: MySQL 8.x / InnoDB
-- Project: Core Java + JDBC + MySQL (Version B)
-- =====================================================================

-- WARNING: Running this script will DROP existing tables in the database
-- if they already exist, permanently removing any existing data.

CREATE DATABASE IF NOT EXISTS vaccination_scheduler
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vaccination_scheduler;

-- Disable foreign key checks temporarily to allow safe drop order if re-running
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS slots;
DROP TABLE IF EXISTS drives;
DROP TABLE IF EXISTS vaccines;
DROP TABLE IF EXISTS centers;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- TABLE 1: users
-- Holds registered citizens and administrative accounts.
-- Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes.
-- =====================================================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for authentication lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- =====================================================================
-- TABLE 2: centers
-- Represents physical hospitals, clinics, or drive-through venues.
-- =====================================================================
CREATE TABLE centers (
    center_id INT AUTO_INCREMENT PRIMARY KEY,
    center_name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for searching centers by name and city
CREATE INDEX idx_centers_city ON centers(city);
CREATE INDEX idx_centers_name ON centers(center_name);
CREATE INDEX idx_centers_active ON centers(active);

-- =====================================================================
-- TABLE 3: vaccines
-- Educational catalog of vaccines available for scheduling.
-- =====================================================================
CREATE TABLE vaccines (
    vaccine_id INT AUTO_INCREMENT PRIMARY KEY,
    vaccine_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_vaccines_name UNIQUE (vaccine_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vaccines_active ON vaccines(active);

-- =====================================================================
-- TABLE 4: drives
-- Represents an organized vaccination campaign at a specific center
-- offering a specific vaccine over a defined calendar window.
-- =====================================================================
CREATE TABLE drives (
    drive_id INT AUTO_INCREMENT PRIMARY KEY,
    drive_name VARCHAR(150) NOT NULL,
    center_id INT NOT NULL,
    vaccine_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_drives_center 
        FOREIGN KEY (center_id) REFERENCES centers(center_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_drives_vaccine 
        FOREIGN KEY (vaccine_id) REFERENCES vaccines(vaccine_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_drives_date_range CHECK (start_date <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for queries filtering drives by center, dates, and active status
CREATE INDEX idx_drives_center ON drives(center_id);
CREATE INDEX idx_drives_vaccine ON drives(vaccine_id);
CREATE INDEX idx_drives_dates ON drives(start_date, end_date);
CREATE INDEX idx_drives_active ON drives(active);

-- =====================================================================
-- TABLE 5: slots
-- Represents bookable time intervals on a specific date for a drive.
-- Capacity defines maximum permissible simultaneous active bookings.
-- =====================================================================
CREATE TABLE slots (
    slot_id INT AUTO_INCREMENT PRIMARY KEY,
    drive_id INT NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_slots_drive 
        FOREIGN KEY (drive_id) REFERENCES drives(drive_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_slots_capacity CHECK (capacity > 0),
    CONSTRAINT chk_slots_time_order CHECK (start_time < end_time),

    -- Prevent duplicate identical slot definitions within the same drive
    CONSTRAINT uq_slots_drive_time UNIQUE (drive_id, slot_date, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for slot searching, date filtering, and locking
CREATE INDEX idx_slots_drive_date ON slots(drive_id, slot_date);
CREATE INDEX idx_slots_date ON slots(slot_date);

-- =====================================================================
-- TABLE 6: appointments
-- Records citizen reservations.
-- Utilizes a Virtual Generated Column with a Unique Constraint to guarantee
-- at the database engine level that no user can hold more than one
-- active ('BOOKED') reservation for the same slot, while freely allowing
-- re-booking after cancellation.
-- =====================================================================
CREATE TABLE appointments (
    appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    slot_id INT NOT NULL,
    booking_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',

    -- Virtual Generated Column: Resolves to 'slot_id:user_id' ONLY when status is BOOKED.
    -- For 'CANCELLED', 'COMPLETED', or 'MISSED', it evaluates to NULL.
    -- In MySQL UNIQUE indexes, multiple NULL entries are permitted, but duplicate
    -- non-NULL strings are strictly rejected!
    active_booking_key VARCHAR(64) GENERATED ALWAYS AS (
        CASE WHEN status = 'BOOKED' THEN CONCAT(slot_id, ':', user_id) ELSE NULL END
    ) VIRTUAL,

    CONSTRAINT fk_appts_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_appts_slot 
        FOREIGN KEY (slot_id) REFERENCES slots(slot_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_appts_status CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED', 'MISSED')),
    CONSTRAINT uq_active_slot_user UNIQUE (active_booking_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for fast appointment searches, user history, and operational reporting
CREATE INDEX idx_appts_user ON appointments(user_id);
CREATE INDEX idx_appts_slot ON appointments(slot_id);
CREATE INDEX idx_appts_status ON appointments(status);
CREATE INDEX idx_appts_booking_time ON appointments(booking_time);
