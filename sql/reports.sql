-- =====================================================================
-- ADMINISTRATIVE & OPERATIONAL REPORTS
-- Database Management System: MySQL 8.x / InnoDB
-- Project: Core Java + JDBC + MySQL (Version B)
-- =====================================================================

USE vaccination_scheduler;

-- ---------------------------------------------------------------------
-- REPORT A: Total Registered Users
-- Displays total citizen registrations and administrative accounts.
-- ---------------------------------------------------------------------
SELECT 
    role,
    active,
    COUNT(*) AS total_users
FROM users
GROUP BY role, active
ORDER BY role, active DESC;

-- Overall summary count
SELECT 
    COUNT(*) AS total_registered_citizens
FROM users
WHERE role = 'USER';


-- ---------------------------------------------------------------------
-- REPORT B: Total Appointments Grouped by Status
-- Shows the distribution of current and historical appointments.
-- ---------------------------------------------------------------------
SELECT 
    status,
    COUNT(*) AS total_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM appointments), 1) AS percentage
FROM appointments
GROUP BY status
ORDER BY total_count DESC;


-- ---------------------------------------------------------------------
-- REPORT C: Center-Wise Bookings
-- Demonstrates LEFT JOIN to ensure centers with ZERO bookings
-- are still displayed in administrative audits.
-- ---------------------------------------------------------------------
SELECT 
    c.center_id,
    c.center_name,
    c.city,
    c.active AS is_center_active,
    COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS active_reservations,
    COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed_vaccinations,
    COUNT(a.appointment_id) AS total_all_time_appointments
FROM centers c
LEFT JOIN drives d ON c.center_id = d.center_id
LEFT JOIN slots s ON d.drive_id = s.drive_id
LEFT JOIN appointments a ON s.slot_id = a.slot_id
GROUP BY c.center_id, c.center_name, c.city, c.active
ORDER BY active_reservations DESC, c.center_name ASC;


-- ---------------------------------------------------------------------
-- REPORT D: Drive-Wise Bookings
-- Shows booking counts and vaccine assignments for each drive.
-- ---------------------------------------------------------------------
SELECT 
    d.drive_id,
    d.drive_name,
    c.center_name,
    v.vaccine_name,
    d.start_date,
    d.end_date,
    d.active AS is_drive_active,
    COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS active_bookings,
    COUNT(CASE WHEN a.status = 'CANCELLED' THEN 1 END) AS cancelled_bookings,
    COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed_bookings,
    COUNT(a.appointment_id) AS total_bookings
FROM drives d
JOIN centers c ON d.center_id = c.center_id
JOIN vaccines v ON d.vaccine_id = v.vaccine_id
LEFT JOIN slots s ON d.drive_id = s.drive_id
LEFT JOIN appointments a ON s.slot_id = a.slot_id
GROUP BY d.drive_id, d.drive_name, c.center_name, v.vaccine_name, d.start_date, d.end_date, d.active
ORDER BY d.start_date DESC;


-- ---------------------------------------------------------------------
-- REPORT E: Daily Appointment Report
-- Aggregates appointments by scheduled slot date.
-- ---------------------------------------------------------------------
SELECT 
    s.slot_date,
    COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS active_scheduled,
    COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed,
    COUNT(CASE WHEN a.status = 'CANCELLED' THEN 1 END) AS cancelled,
    COUNT(CASE WHEN a.status = 'MISSED' THEN 1 END) AS missed,
    COUNT(a.appointment_id) AS total_daily_appointments
FROM slots s
JOIN appointments a ON s.slot_id = a.slot_id
GROUP BY s.slot_date
ORDER BY s.slot_date ASC;


-- ---------------------------------------------------------------------
-- REPORT F: Live Slot Availability Report
-- Crucial operational query showing real-time capacity and remaining slots.
-- ---------------------------------------------------------------------
SELECT 
    s.slot_id,
    c.center_name,
    c.city,
    d.drive_name,
    v.vaccine_name,
    s.slot_date,
    s.start_time,
    s.end_time,
    s.capacity,
    COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS booked_count,
    (s.capacity - COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END)) AS remaining_capacity,
    CASE 
        WHEN (s.capacity - COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END)) = 0 THEN 'FULL'
        WHEN (s.capacity - COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END)) <= 2 THEN 'FILLING FAST'
        ELSE 'AVAILABLE'
    END AS availability_status
FROM slots s
JOIN drives d ON s.drive_id = d.drive_id
JOIN centers c ON d.center_id = c.center_id
JOIN vaccines v ON d.vaccine_id = v.vaccine_id
LEFT JOIN appointments a ON s.slot_id = a.slot_id
WHERE d.active = TRUE AND c.active = TRUE
GROUP BY s.slot_id, c.center_name, c.city, d.drive_name, v.vaccine_name, s.slot_date, s.start_time, s.end_time, s.capacity
ORDER BY s.slot_date ASC, s.start_time ASC;


-- ---------------------------------------------------------------------
-- REPORT G: Completed Vaccinations by Drive & Center
-- Summarizes successful citizen immunizations for public health reporting.
-- ---------------------------------------------------------------------
SELECT 
    c.center_name,
    d.drive_name,
    v.vaccine_name,
    COUNT(a.appointment_id) AS total_completed_immunizations
FROM appointments a
JOIN slots s ON a.slot_id = s.slot_id
JOIN drives d ON s.drive_id = d.drive_id
JOIN centers c ON d.center_id = c.center_id
JOIN vaccines v ON d.vaccine_id = v.vaccine_id
WHERE a.status = 'COMPLETED'
GROUP BY c.center_name, d.drive_name, v.vaccine_name
ORDER BY total_completed_immunizations DESC;
