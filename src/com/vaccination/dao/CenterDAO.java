package com.vaccination.dao;

import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Center;
import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Vaccination Centers.
 */
public class CenterDAO {

    public Center insert(Center center) throws DatabaseException {
        String sql = "INSERT INTO centers (center_name, address, city, phone, active) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, center.getCenterName().trim());
            ps.setString(2, center.getAddress().trim());
            ps.setString(3, center.getCity().trim());
            ps.setString(4, center.getPhone() != null ? center.getPhone().trim() : null);
            ps.setBoolean(5, center.isActive());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    center.setCenterId(rs.getInt(1));
                }
            }
            return center;

        } catch (SQLException e) {
            throw new DatabaseException("Error creating vaccination center: " + e.getMessage(), e);
        }
    }

    public Optional<Center> findById(int centerId) throws DatabaseException {
        String sql = "SELECT center_id, center_name, address, city, phone, active, created_at FROM centers WHERE center_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCenter(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving center by ID: " + e.getMessage(), e);
        }
    }

    public List<Center> findAll() throws DatabaseException {
        String sql = "SELECT center_id, center_name, address, city, phone, active, created_at FROM centers ORDER BY city ASC, center_name ASC";
        List<Center> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToCenter(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error listing vaccination centers: " + e.getMessage(), e);
        }
    }

    public List<Center> findAllActive() throws DatabaseException {
        String sql = "SELECT center_id, center_name, address, city, phone, active, created_at FROM centers WHERE active = TRUE ORDER BY city ASC, center_name ASC";
        List<Center> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToCenter(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error listing active centers: " + e.getMessage(), e);
        }
    }

    public List<Center> searchByNameOrCity(String query) throws DatabaseException {
        String sql = "SELECT center_id, center_name, address, city, phone, active, created_at FROM centers " +
                     "WHERE LOWER(center_name) LIKE ? OR LOWER(city) LIKE ? ORDER BY city, center_name";
        List<Center> list = new ArrayList<>();
        String pattern = "%" + query.trim().toLowerCase() + "%";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCenter(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error searching centers: " + e.getMessage(), e);
        }
    }

    public void update(Center center) throws DatabaseException {
        String sql = "UPDATE centers SET center_name = ?, address = ?, city = ?, phone = ?, active = ? WHERE center_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, center.getCenterName().trim());
            ps.setString(2, center.getAddress().trim());
            ps.setString(3, center.getCity().trim());
            ps.setString(4, center.getPhone() != null ? center.getPhone().trim() : null);
            ps.setBoolean(5, center.isActive());
            ps.setInt(6, center.getCenterId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating center: " + e.getMessage(), e);
        }
    }

    public void setActiveStatus(int centerId, boolean active) throws DatabaseException {
        String sql = "UPDATE centers SET active = ? WHERE center_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, centerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error toggling center status: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if any historical appointments depend on drives at this center.
     */
    public boolean hasHistoricalAppointments(int centerId) throws DatabaseException {
        String sql = "SELECT 1 FROM appointments a " +
                     "JOIN slots s ON a.slot_id = s.slot_id " +
                     "JOIN drives d ON s.drive_id = d.drive_id " +
                     "WHERE d.center_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking center appointment history: " + e.getMessage(), e);
        }
    }

    private Center mapResultSetToCenter(ResultSet rs) throws SQLException {
        Center center = new Center();
        center.setCenterId(rs.getInt("center_id"));
        center.setCenterName(rs.getString("center_name"));
        center.setAddress(rs.getString("address"));
        center.setCity(rs.getString("city"));
        center.setPhone(rs.getString("phone"));
        center.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            center.setCreatedAt(ts.toLocalDateTime());
        }
        return center;
    }
}
