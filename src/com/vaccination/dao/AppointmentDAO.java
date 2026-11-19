package com.vaccination.dao;

import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Appointment;
import com.vaccination.model.AppointmentStatus;
import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Citizen Appointments.
 * Provides transactional queries designed for concurrency-controlled execution.
 */
public class AppointmentDAO {

    private static final String BASE_JOIN_SELECT = 
        "SELECT a.appointment_id, a.user_id, a.slot_id, a.booking_time, a.status, " +
        "u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone, " +
        "s.slot_date, s.start_time, s.end_time, " +
        "c.center_name, c.city AS center_city, c.address AS center_address, " +
        "d.drive_name, v.vaccine_name " +
        "FROM appointments a " +
        "JOIN users u ON a.user_id = u.user_id " +
        "JOIN slots s ON a.slot_id = s.slot_id " +
        "JOIN drives d ON s.drive_id = d.drive_id " +
        "JOIN centers c ON d.center_id = c.center_id " +
        "JOIN vaccines v ON d.vaccine_id = v.vaccine_id ";

    /**
     * Inserts an appointment inside an active transaction.
     * Uses the caller-supplied transactional Connection.
     */
    public Appointment insertTransactional(Connection conn, Appointment appointment) throws SQLException {
        String sql = "INSERT INTO appointments (user_id, slot_id, booking_time, status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appointment.getUserId());
            ps.setInt(2, appointment.getSlotId());
            ps.setTimestamp(3, Timestamp.valueOf(appointment.getBookingTime()));
            ps.setString(4, appointment.getStatus().name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    appointment.setAppointmentId(rs.getLong(1));
                }
            }
            return appointment;
        }
    }

    /**
     * Checks inside the transactional lock whether the user already holds an active booking for this slot.
     */
    public boolean hasActiveBookingForSlotTransactional(Connection conn, int slotId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM appointments WHERE slot_id = ? AND user_id = ? AND status = 'BOOKED'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Recalculates the active bookings count for a slot inside the transactional row lock.
     */
    public int countActiveBookingsTransactional(Connection conn, int slotId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE slot_id = ? AND status = 'BOOKED'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        }
    }

    public Optional<Appointment> findById(long appointmentId) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + "WHERE a.appointment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAppointment(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving appointment details: " + e.getMessage(), e);
        }
    }

    public List<Appointment> findByUserId(int userId) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + "WHERE a.user_id = ? ORDER BY s.slot_date DESC, s.start_time DESC";
        List<Appointment> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user appointment history: " + e.getMessage(), e);
        }
    }

    public List<Appointment> findUpcomingByUserId(int userId) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + 
                     "WHERE a.user_id = ? AND a.status = 'BOOKED' " +
                     "AND (s.slot_date > CURRENT_DATE OR (s.slot_date = CURRENT_DATE AND s.start_time > CURRENT_TIME)) " +
                     "ORDER BY s.slot_date ASC, s.start_time ASC";
        List<Appointment> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving upcoming appointments: " + e.getMessage(), e);
        }
    }

    public List<Appointment> findAllFiltered(AppointmentStatus status, Integer centerId, Integer driveId, LocalDate date) throws DatabaseException {
        StringBuilder sql = new StringBuilder(BASE_JOIN_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append("AND a.status = ? ");
            params.add(status.name());
        }
        if (centerId != null && centerId > 0) {
            sql.append("AND d.center_id = ? ");
            params.add(centerId);
        }
        if (driveId != null && driveId > 0) {
            sql.append("AND d.drive_id = ? ");
            params.add(driveId);
        }
        if (date != null) {
            sql.append("AND s.slot_date = ? ");
            params.add(Date.valueOf(date));
        }

        sql.append("ORDER BY s.slot_date DESC, s.start_time DESC LIMIT 100");
        List<Appointment> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error filtering appointments: " + e.getMessage(), e);
        }
    }

    public void updateStatus(long appointmentId, AppointmentStatus status) throws DatabaseException {
        String sql = "UPDATE appointments SET status = ? WHERE appointment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, appointmentId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating appointment status: " + e.getMessage(), e);
        }
    }

    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        Appointment appt = new Appointment();
        appt.setAppointmentId(rs.getLong("appointment_id"));
        appt.setUserId(rs.getInt("user_id"));
        appt.setSlotId(rs.getInt("slot_id"));
        Timestamp bTime = rs.getTimestamp("booking_time");
        if (bTime != null) appt.setBookingTime(bTime.toLocalDateTime());
        appt.setStatus(AppointmentStatus.fromString(rs.getString("status")));

        // Presentation joined details
        appt.setUserName(rs.getString("user_name"));
        appt.setUserEmail(rs.getString("user_email"));
        appt.setUserPhone(rs.getString("user_phone"));
        Date sDate = rs.getDate("slot_date");
        if (sDate != null) appt.setSlotDate(sDate.toLocalDate());
        Time st = rs.getTime("start_time");
        if (st != null) appt.setStartTime(st.toLocalTime());
        Time et = rs.getTime("end_time");
        if (et != null) appt.setEndTime(et.toLocalTime());
        appt.setCenterName(rs.getString("center_name"));
        appt.setCenterCity(rs.getString("center_city"));
        appt.setCenterAddress(rs.getString("center_address"));
        appt.setDriveName(rs.getString("drive_name"));
        appt.setVaccineName(rs.getString("vaccine_name"));

        return appt;
    }
}
