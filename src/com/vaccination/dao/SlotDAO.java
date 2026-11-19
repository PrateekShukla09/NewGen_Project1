package com.vaccination.dao;

import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Slot;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Appointment Slots.
 * Includes pessimistic locking methods (SELECT ... FOR UPDATE) for transaction safety.
 */
public class SlotDAO {

    private static final String BASE_JOIN_SELECT = 
        "SELECT s.slot_id, s.drive_id, s.slot_date, s.start_time, s.end_time, s.capacity, s.created_at, " +
        "d.drive_name, c.center_name, c.city AS center_city, v.vaccine_name, " +
        "(SELECT COUNT(*) FROM appointments a WHERE a.slot_id = s.slot_id AND a.status = 'BOOKED') AS booked_count " +
        "FROM slots s " +
        "JOIN drives d ON s.drive_id = d.drive_id " +
        "JOIN centers c ON d.center_id = c.center_id " +
        "JOIN vaccines v ON d.vaccine_id = v.vaccine_id ";

    public Slot insert(Slot slot) throws DatabaseException {
        String sql = "INSERT INTO slots (drive_id, slot_date, start_time, end_time, capacity) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, slot.getDriveId());
            ps.setDate(2, Date.valueOf(slot.getSlotDate()));
            ps.setTime(3, Time.valueOf(slot.getStartTime()));
            ps.setTime(4, Time.valueOf(slot.getEndTime()));
            ps.setInt(5, slot.getCapacity());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    slot.setSlotId(rs.getInt(1));
                }
            }
            return slot;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DatabaseException("A slot for this drive on " + slot.getSlotDate() + 
                        " starting at " + slot.getStartTime() + " already exists.", e);
            }
            throw new DatabaseException("Error creating slot: " + e.getMessage(), e);
        }
    }

    public Optional<Slot> findById(int slotId) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + "WHERE s.slot_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSlot(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error finding slot by ID: " + e.getMessage(), e);
        }
    }

    /**
     * CRITICAL FOR CONCURRENCY:
     * Locks the slot row using SELECT ... FOR UPDATE on an active transactional connection.
     * Any other concurrent transaction attempting to lock this slot will block at the DB engine.
     */
    public Optional<Slot> findByIdForUpdate(Connection conn, int slotId) throws SQLException {
        String sql = "SELECT slot_id, drive_id, slot_date, start_time, end_time, capacity, created_at " +
                     "FROM slots WHERE slot_id = ? FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Slot slot = new Slot();
                    slot.setSlotId(rs.getInt("slot_id"));
                    slot.setDriveId(rs.getInt("drive_id"));
                    slot.setSlotDate(rs.getDate("slot_date").toLocalDate());
                    slot.setStartTime(rs.getTime("start_time").toLocalTime());
                    slot.setEndTime(rs.getTime("end_time").toLocalTime());
                    slot.setCapacity(rs.getInt("capacity"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) slot.setCreatedAt(ts.toLocalDateTime());
                    return Optional.of(slot);
                }
            }
        }
        return Optional.empty();
    }

    public List<Slot> findByDriveId(int driveId) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + "WHERE s.drive_id = ? ORDER BY s.slot_date ASC, s.start_time ASC";
        List<Slot> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, driveId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSlot(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving slots for drive: " + e.getMessage(), e);
        }
    }

    public List<Slot> findAvailableSlotsByDrive(int driveId) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + 
                     "WHERE s.drive_id = ? AND d.active = TRUE AND c.active = TRUE " +
                     "AND (s.slot_date > CURRENT_DATE OR (s.slot_date = CURRENT_DATE AND s.start_time > CURRENT_TIME)) " +
                     "HAVING (s.capacity - booked_count) > 0 " +
                     "ORDER BY s.slot_date ASC, s.start_time ASC";
        List<Slot> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, driveId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSlot(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error querying available slots: " + e.getMessage(), e);
        }
    }

    public List<Slot> findByDate(LocalDate date) throws DatabaseException {
        String sql = BASE_JOIN_SELECT + "WHERE s.slot_date = ? ORDER BY s.start_time ASC";
        List<Slot> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSlot(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error querying slots by date: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a conflicting overlapping slot exists for the same physical Center on the same date.
     * Two intervals [A, B] and [C, D] overlap if: A < D AND B > C
     */
    public boolean hasOverlappingSlotAtCenter(int centerId, LocalDate date, LocalTime startTime, LocalTime endTime) throws DatabaseException {
        String sql = "SELECT 1 FROM slots s " +
                     "JOIN drives d ON s.drive_id = d.drive_id " +
                     "WHERE d.center_id = ? AND s.slot_date = ? " +
                     "AND (s.start_time < ? AND s.end_time > ?) " +
                     "LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centerId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(endTime));
            ps.setTime(4, Time.valueOf(startTime));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking slot overlap at center: " + e.getMessage(), e);
        }
    }

    public int getActiveBookingCount(int slotId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE slot_id = ? AND status = 'BOOKED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error checking slot active bookings: " + e.getMessage(), e);
        }
    }

    public void updateCapacity(int slotId, int newCapacity) throws DatabaseException {
        String sql = "UPDATE slots SET capacity = ? WHERE slot_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newCapacity);
            ps.setInt(2, slotId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating slot capacity: " + e.getMessage(), e);
        }
    }

    private Slot mapResultSetToSlot(ResultSet rs) throws SQLException {
        Slot slot = new Slot();
        slot.setSlotId(rs.getInt("slot_id"));
        slot.setDriveId(rs.getInt("drive_id"));
        Date sDate = rs.getDate("slot_date");
        if (sDate != null) slot.setSlotDate(sDate.toLocalDate());
        Time st = rs.getTime("start_time");
        if (st != null) slot.setStartTime(st.toLocalTime());
        Time et = rs.getTime("end_time");
        if (et != null) slot.setEndTime(et.toLocalTime());
        slot.setCapacity(rs.getInt("capacity"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) slot.setCreatedAt(ts.toLocalDateTime());

        // Joined fields
        slot.setDriveName(rs.getString("drive_name"));
        slot.setCenterName(rs.getString("center_name"));
        slot.setCenterCity(rs.getString("center_city"));
        slot.setVaccineName(rs.getString("vaccine_name"));
        slot.setBookedCount(rs.getInt("booked_count"));

        return slot;
    }
}
