package com.vaccination.dao;

import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Vaccine;
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
 * Data Access Object for Vaccine catalog items.
 */
public class VaccineDAO {

    public Vaccine insert(Vaccine vaccine) throws DatabaseException {
        String sql = "INSERT INTO vaccines (vaccine_name, manufacturer, description, active) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, vaccine.getVaccineName().trim());
            ps.setString(2, vaccine.getManufacturer() != null ? vaccine.getManufacturer().trim() : null);
            ps.setString(3, vaccine.getDescription() != null ? vaccine.getDescription().trim() : null);
            ps.setBoolean(4, vaccine.isActive());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    vaccine.setVaccineId(rs.getInt(1));
                }
            }
            return vaccine;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DatabaseException("A vaccine with the name '" + vaccine.getVaccineName() + "' already exists.", e);
            }
            throw new DatabaseException("Error creating vaccine record: " + e.getMessage(), e);
        }
    }

    public Optional<Vaccine> findById(int vaccineId) throws DatabaseException {
        String sql = "SELECT vaccine_id, vaccine_name, manufacturer, description, active, created_at FROM vaccines WHERE vaccine_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, vaccineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToVaccine(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving vaccine by ID: " + e.getMessage(), e);
        }
    }

    public List<Vaccine> findAll() throws DatabaseException {
        String sql = "SELECT vaccine_id, vaccine_name, manufacturer, description, active, created_at FROM vaccines ORDER BY vaccine_name ASC";
        List<Vaccine> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToVaccine(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error listing vaccines: " + e.getMessage(), e);
        }
    }

    public List<Vaccine> findAllActive() throws DatabaseException {
        String sql = "SELECT vaccine_id, vaccine_name, manufacturer, description, active, created_at FROM vaccines WHERE active = TRUE ORDER BY vaccine_name ASC";
        List<Vaccine> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToVaccine(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error listing active vaccines: " + e.getMessage(), e);
        }
    }

    public List<Vaccine> searchByName(String query) throws DatabaseException {
        String sql = "SELECT vaccine_id, vaccine_name, manufacturer, description, active, created_at FROM vaccines " +
                     "WHERE LOWER(vaccine_name) LIKE ? OR LOWER(manufacturer) LIKE ? ORDER BY vaccine_name";
        List<Vaccine> list = new ArrayList<>();
        String pattern = "%" + query.trim().toLowerCase() + "%";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToVaccine(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Error searching vaccines: " + e.getMessage(), e);
        }
    }

    public void update(Vaccine vaccine) throws DatabaseException {
        String sql = "UPDATE vaccines SET vaccine_name = ?, manufacturer = ?, description = ?, active = ? WHERE vaccine_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vaccine.getVaccineName().trim());
            ps.setString(2, vaccine.getManufacturer() != null ? vaccine.getManufacturer().trim() : null);
            ps.setString(3, vaccine.getDescription() != null ? vaccine.getDescription().trim() : null);
            ps.setBoolean(4, vaccine.isActive());
            ps.setInt(5, vaccine.getVaccineId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating vaccine: " + e.getMessage(), e);
        }
    }

    public void setActiveStatus(int vaccineId, boolean active) throws DatabaseException {
        String sql = "UPDATE vaccines SET active = ? WHERE vaccine_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, vaccineId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating vaccine active status: " + e.getMessage(), e);
        }
    }

    public boolean hasHistoricalAppointments(int vaccineId) throws DatabaseException {
        String sql = "SELECT 1 FROM appointments a " +
                     "JOIN slots s ON a.slot_id = s.slot_id " +
                     "JOIN drives d ON s.drive_id = d.drive_id " +
                     "WHERE d.vaccine_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, vaccineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking vaccine appointment history: " + e.getMessage(), e);
        }
    }

    private Vaccine mapResultSetToVaccine(ResultSet rs) throws SQLException {
        Vaccine vax = new Vaccine();
        vax.setVaccineId(rs.getInt("vaccine_id"));
        vax.setVaccineName(rs.getString("vaccine_name"));
        vax.setManufacturer(rs.getString("manufacturer"));
        vax.setDescription(rs.getString("description"));
        vax.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            vax.setCreatedAt(ts.toLocalDateTime());
        }
        return vax;
    }
}
