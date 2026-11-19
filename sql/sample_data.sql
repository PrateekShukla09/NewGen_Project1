-- =====================================================================
-- SAMPLE DATA: Vaccination Drive Scheduler
-- Database Management System: MySQL 8.x / InnoDB
-- Project: Core Java + JDBC + MySQL (Version B)
-- =====================================================================

USE vaccination_scheduler;

-- Clean existing records in reverse dependency order
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE appointments;
TRUNCATE TABLE slots;
TRUNCATE TABLE drives;
TRUNCATE TABLE vaccines;
TRUNCATE TABLE centers;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 1. SEED USERS
-- All passwords use PBKDF2WithHmacSHA256 (65,536 iterations, 256-bit).
-- Default credentials:
--   Admin: admin@vaccination.org  / Admin@1234
--   Users: john.doe@example.com   / User@1234
--          alice.smith@example.com / User@1234
--          bob.johnson@example.com / User@1234
-- =====================================================================
INSERT INTO users (user_id, full_name, email, phone, password_hash, password_salt, role, active) VALUES
(1, 'System Administrator', 'admin@vaccination.org', '555-0100', 
 'c06b2e9f3b98079704a7442ca6240e22b98ceb2c04ccfb1b9da5de557a71a3e0', 
 '6926c99a650824c6a7b0d89d63800672', 'ADMIN', TRUE),

(2, 'John Doe', 'john.doe@example.com', '555-0101', 
 '22d1299c1828c91d19a546ede45c8dada5d7963039bcba95592ea31cc033a77e', 
 '40aaba62697761b315651d752d856462', 'USER', TRUE),

(3, 'Alice Smith', 'alice.smith@example.com', '555-0102', 
 '22d1299c1828c91d19a546ede45c8dada5d7963039bcba95592ea31cc033a77e', 
 '40aaba62697761b315651d752d856462', 'USER', TRUE),

(4, 'Bob Johnson', 'bob.johnson@example.com', '555-0103', 
 '22d1299c1828c91d19a546ede45c8dada5d7963039bcba95592ea31cc033a77e', 
 '40aaba62697761b315651d752d856462', 'USER', TRUE),

(5, 'Disabled Citizen', 'disabled.user@example.com', '555-0104', 
 '22d1299c1828c91d19a546ede45c8dada5d7963039bcba95592ea31cc033a77e', 
 '40aaba62697761b315651d752d856462', 'USER', FALSE);

-- =====================================================================
-- 2. SEED VACCINATION CENTERS
-- =====================================================================
INSERT INTO centers (center_id, center_name, address, city, phone, active) VALUES
(1, 'Metro General Hospital', '100 Healthcare Blvd, Suite A', 'Metropolis', '555-1001', TRUE),
(2, 'City Civic Community Center', '450 Municipal Plaza', 'Metropolis', '555-1002', TRUE),
(3, 'Riverside Health Center', '78 Riverfront Parkway', 'Gotham', '555-1003', TRUE),
(4, 'Westside Family Clinic', '1204 Oakridge Ave', 'Star City', '555-1004', TRUE),
(5, 'Old Central Dispensary', '33 Industrial Road', 'Gotham', '555-1005', FALSE);

-- =====================================================================
-- 3. SEED VACCINES
-- Note: Descriptions are educational/illustrative. No medical advice.
-- =====================================================================
INSERT INTO vaccines (vaccine_id, vaccine_name, manufacturer, description, active) VALUES
(1, 'Covax-Alpha', 'Aegis BioLabs', 'Fictional educational mRNA-based formulation', TRUE),
(2, 'VaxShield-Beta', 'Helix Pharma', 'Fictional educational protein-subunit formulation', TRUE),
(3, 'NuProtect-Gamma', 'Apex Therapeutics', 'Fictional educational viral-vector formulation', TRUE),
(4, 'Legacy-Vax-Old', 'Vintage Bio', 'Decommissioned sample formulation for testing', FALSE);

-- =====================================================================
-- 4. SEED VACCINATION DRIVES
-- =====================================================================
INSERT INTO drives (drive_id, drive_name, center_id, vaccine_id, start_date, end_date, active) VALUES
-- Active & Upcoming Drives
(1, 'Metropolis Winter Immunity Drive', 1, 1, '2026-11-01', '2026-11-30', TRUE),
(2, 'Gotham Civic Vaccination Campaign', 3, 2, '2026-11-05', '2026-12-15', TRUE),
(3, 'Star City Community Outreach', 4, 3, '2026-11-10', '2026-11-25', TRUE),

-- Historical Completed Drive (for testing past records and reports)
(4, 'Metropolis Early Autumn Drive', 2, 1, '2026-09-01', '2026-09-30', TRUE),

-- Deactivated Drive (for testing inactive drive booking guards)
(5, 'Cancelled Pilot Drive', 1, 3, '2026-12-01', '2026-12-10', FALSE);

-- =====================================================================
-- 5. SEED APPOINTMENT SLOTS
-- =====================================================================
INSERT INTO slots (slot_id, drive_id, slot_date, start_time, end_time, capacity) VALUES
-- Drive 1: Metropolis General Hospital (Future Slots)
(1, 1, '2026-11-05', '09:00:00', '10:00:00', 5),
(2, 1, '2026-11-05', '10:00:00', '11:00:00', 10),
(3, 1, '2026-11-05', '11:00:00', '12:00:00', 1),  -- Single capacity (ideal for concurrency testing!)
(4, 1, '2026-11-06', '14:00:00', '15:00:00', 8),

-- Drive 2: Gotham Riverside Health Center
(5, 2, '2026-11-12', '09:30:00', '10:30:00', 12),
(6, 2, '2026-11-12', '10:30:00', '11:30:00', 12),

-- Drive 3: Star City Clinic
(7, 3, '2026-11-15', '10:00:00', '11:00:00', 6),

-- Drive 4: Historical Completed Slots
(8, 4, '2026-09-15', '09:00:00', '10:00:00', 5),
(9, 4, '2026-09-15', '10:00:00', '11:00:00', 5);

-- =====================================================================
-- 6. SEED APPOINTMENTS
-- =====================================================================
INSERT INTO appointments (appointment_id, user_id, slot_id, booking_time, status) VALUES
-- Active reservations for Slot 1 (capacity 5, 2 booked -> 3 remaining)
(1001, 2, 1, '2026-10-20 10:15:00', 'BOOKED'),
(1002, 3, 1, '2026-10-21 11:30:00', 'BOOKED'),

-- Active reservation for Slot 3 (capacity 1, 1 booked -> 0 remaining, FULL!)
(1003, 4, 3, '2026-10-22 09:00:00', 'BOOKED'),

-- Cancelled reservation: User 2 booked Slot 2 and later cancelled it.
-- Notice active_booking_key is NULL, releasing capacity back to the slot.
(1004, 2, 2, '2026-10-18 14:00:00', 'CANCELLED'),

-- Historical Completed & Missed Appointments on Slot 8
(1005, 2, 8, '2026-09-10 08:30:00', 'COMPLETED'),
(1006, 3, 8, '2026-09-11 12:00:00', 'COMPLETED'),
(1007, 4, 8, '2026-09-11 15:45:00', 'MISSED');

-- Reset auto-increment sequence counters to safe thresholds beyond seed data
ALTER TABLE users AUTO_INCREMENT = 10;
ALTER TABLE centers AUTO_INCREMENT = 10;
ALTER TABLE vaccines AUTO_INCREMENT = 10;
ALTER TABLE drives AUTO_INCREMENT = 10;
ALTER TABLE slots AUTO_INCREMENT = 20;
ALTER TABLE appointments AUTO_INCREMENT = 2000;
