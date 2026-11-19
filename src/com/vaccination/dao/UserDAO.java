package com.vaccination.dao;

import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Role;
import com.vaccination.model.User;
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
 * Data Access Object for User accounts.
 * Manages SQL persistence using prepared statements and connection management.
 */
public class UserDAO {

    /**
     * Inserts a new user record into the database.
     * Sets the generated user_id on the user object.
     */
    public User insert(User user) throws DatabaseException {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, password_salt, role, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPhone().trim());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getPasswordSalt());
            ps.setString(6, user.getRole().name());
            ps.setBoolean(7, user.isActive());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Failed to insert user, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
            }
            return user;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry for UNIQUE key
                throw new DatabaseException("A user with this email address is already registered.", e);
            }
            throw new DatabaseException("Error inserting user: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a user by email address (case-insensitive).
     */
    public Optional<User> findByEmail(String email) throws DatabaseException {
        String sql = "SELECT user_id, full_name, email, phone, password_hash, password_salt, role, active, created_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by email: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a user by their unique primary key ID.
     */
    public Optional<User> findById(int userId) throws DatabaseException {
        String sql = "SELECT user_id, full_name, email, phone, password_hash, password_salt, role, active, created_at " +
                     "FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by ID: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if an email address is already registered.
     */
    public boolean emailExists(String email) throws DatabaseException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking email existence: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the total number of administrator accounts in the system.
     * Used for the first-time admin bootstrap workflow.
     */
    public int countAdmins() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error counting admin users: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all users (for administrative reports/audits).
     */
    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT user_id, full_name, email, phone, password_hash, password_salt, role, active, created_at " +
                     "FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all users: " + e.getMessage(), e);
        }
    }

    /**
     * Updates account active status (activation/deactivation).
     */
    public void setActiveStatus(int userId, boolean active) throws DatabaseException {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating user active status: " + e.getMessage(), e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPasswordSalt(rs.getString("password_salt"));
        user.setRole(Role.fromString(rs.getString("role")));
        user.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
}
