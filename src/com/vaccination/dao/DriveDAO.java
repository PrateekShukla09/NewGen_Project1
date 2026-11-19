package com.vaccination.dao;

import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Drive;
import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Vaccination Drives.
 * Performs SQL JOINs with centers and vaccines to construct fully descriptive domain models.
 */
public class DriveDAO {

    private static final String BASE_SELECT = 
        "SELECT d.drive_id, d.drive_name, d.center_id, d.vaccine_id, d.start_date, d.end_date, d.active, d.created_at, " +
        "c.center_name, c.city AS center_city, v.vaccine_name " +
        "FROM drives d " +
        "JOIN centers c ON d.center_id = c.center_id " +
        "JOIN vaccines v ON d.vaccine_id = v.vaccine_id ";

    public Drive insert(Drive drive) throws DatabaseException {
        String sql = "INSERT INTO drives (drive_name, center_id, vaccine_id, start_date, end_date, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, drive.getDriveName().trim());
            ps.setInt(2, drive.getCenterId());
            ps.setInt(3, drive.getVaccineId());
            ps.setDate(4, Date.valueOf(drive.getStartDate()));
            ps.setDate(5, Date.valueOf(drive.getEndDate()));
            ps.setBoolean(6, drive.isActive());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    drive.setDriveId(rs.getInt(1));
                }
            }
            return drive;

        } catch (SQLException e) {
            throw new DatabaseException("Error creating vaccination drive: " + e.getMessage(), e);
        }
    }

    public Optional<Drive> findById(int driveId) throws DatabaseException {
        String sql = BASE_SELECT + "WHERE d.drive_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, driveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDrive(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving drive by ID: " + e.getMessage(), e);
        }
    }

    public List<Drive> findAll() throws DatabaseException {
        String sql = BASE_SELECT + "ORDER BY d.start_date DESC";
        return executeQueryList(sql);
    }

    public List<Drive> findAllActive() throws DatabaseException {
        String sql = BASE_SELECT + "WHERE d.active = TRUE AND c.active = TRUE ORDER BY d.start_date ASC";
        return executeQueryList(sql);
    }

    public List<Drive> findUpcoming(LocalDate referenceDate) throws DatabaseException {
        String sql = BASE_SELECT + "WHERE d.start_date >= ? AND d.active = TRUE AND c.active = TRUE ORDER BY d.start_date ASC";
        return executeParamDateList(sql, referenceDate);
    }

    public List<Drive> findCurrentActive(LocalDate referenceDate) throws DatabaseException {
        String sql = BASE_SELECT + "WHERE d.start_date <= ? AND d.end_date >= ? AND d.active = TRUE AND c.active = TRUE ORDER BY d.start_date ASC";
        List<Drive> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(referenceDate));
            ps.setDate(2, Date.valueOf(referenceDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDrive(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving currently active drives: " + e.getMessage(), e);
        }
    }

    public List<Drive> findCompleted(LocalDate referenceDate) throws DatabaseException {
        String sql = BASE_SELECT + "WHERE d.end_date < ? ORDER BY d.end_date DESC";
        return executeParamDateList(sql, referenceDate);
    }

    public List<Drive> findByCity(String city) throws DatabaseException {
        String sql = BASE_SELECT + "WHERE LOWER(c.city) LIKE ? AND d.active = TRUE AND c.active = TRUE ORDER BY d.start_date ASC";
        List<Drive> list = new ArrayList<>();
        String pattern = "%" + city.trim().toLowerCase() + "%";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDrive(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error searching drives by city: " + e.getMessage(), e);
        }
    }

    public List<Drive> findByCenterId(int centerId) throws DatabaseException {
        String sql = BASE_SELECT + "WHERE d.center_id = ? ORDER BY d.start_date DESC";
        List<Drive> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDrive(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving drives by center: " + e.getMessage(), e);
        }
    }

    public void update(Drive drive) throws DatabaseException {
        String sql = "UPDATE drives SET drive_name = ?, center_id = ?, vaccine_id = ?, " +
                     "start_date = ?, end_date = ?, active = ? WHERE drive_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, drive.getDriveName().trim());
            ps.setInt(2, drive.getCenterId());
            ps.setInt(3, drive.getVaccineId());
            ps.setDate(4, Date.valueOf(drive.getStartDate()));
            ps.setDate(5, Date.valueOf(drive.getEndDate()));
            ps.setBoolean(6, drive.isActive());
            ps.setInt(7, drive.getDriveId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating drive: " + e.getMessage(), e);
        }
    }

    public void setActiveStatus(int driveId, boolean active) throws DatabaseException {
        String sql = "UPDATE drives SET active = ? WHERE drive_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, driveId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating drive active status: " + e.getMessage(), e);
        }
    }

    public boolean hasHistoricalAppointments(int driveId) throws DatabaseException {
        String sql = "SELECT 1 FROM appointments a " +
                     "JOIN slots s ON a.slot_id = s.slot_id " +
                     "WHERE s.drive_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, driveId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking drive appointment history: " + e.getMessage(), e);
        }
    }

    private List<Drive> executeQueryList(String sql) throws DatabaseException {
        List<Drive> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDrive(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error querying drives: " + e.getMessage(), e);
        }
    }

    private List<Drive> executeParamDateList(String sql, LocalDate date) throws DatabaseException {
        List<Drive> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDrive(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error querying drives by date: " + e.getMessage(), e);
        }
    }

    private Drive mapResultSetToDrive(ResultSet rs) throws SQLException {
        Drive drive = new Drive();
        drive.setDriveId(rs.getInt("drive_id"));
        drive.setDriveName(rs.getString("drive_name"));
        drive.setCenterId(rs.getInt("center_id"));
        drive.setVaccineId(rs.getInt("vaccine_id"));
        Date sDate = rs.getDate("start_date");
        if (sDate != null) drive.setStartDate(sDate.toLocalDate());
        Date eDate = rs.getDate("end_date");
        if (eDate != null) drive.setEndDate(eDate.toLocalDate());
        drive.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) drive.setCreatedAt(ts.toLocalDateTime());

        // Presentation fields
        drive.setCenterName(rs.getString("center_name"));
        drive.setCenterCity(rs.getString("center_city"));
        drive.setVaccineName(rs.getString("vaccine_name"));

        return drive;
    }
}
