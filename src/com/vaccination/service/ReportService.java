package com.vaccination.service;

import com.vaccination.exception.AppException;
import com.vaccination.exception.DatabaseException;
import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service executing analytical operational SQL reports using aggregate functions,
 * grouping, and multi-table joins. Formats results cleanly for console presentation.
 */
public class ReportService {

    /**
     * REPORT A: User Registration Statistics.
     */
    public void printUserReport() throws AppException {
        String sql = "SELECT role, active, COUNT(*) AS count FROM users GROUP BY role, active ORDER BY role, active DESC";
        String totalSql = "SELECT COUNT(*) FROM users WHERE role = 'USER'";

        System.out.println("\n=======================================================");
        System.out.println("              REPORT A: REGISTERED USERS               ");
        System.out.println("=======================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-15s | %-12s | %-10s%n", "ROLE", "STATUS", "COUNT");
            System.out.println("-------------------------------------------------------");
            while (rs.next()) {
                String role = rs.getString("role");
                String active = rs.getBoolean("active") ? "ACTIVE" : "DISABLED";
                int count = rs.getInt("count");
                System.out.printf("%-15s | %-12s | %-10d%n", role, active, count);
            }

            try (PreparedStatement psTotal = conn.prepareStatement(totalSql);
                 ResultSet rsTotal = psTotal.executeQuery()) {
                if (rsTotal.next()) {
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Total Registered Citizens: " + rsTotal.getInt(1));
                }
            }
            System.out.println("=======================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating user report: " + e.getMessage(), e);
        }
    }

    /**
     * REPORT B: Appointment Counts by Status.
     */
    public void printAppointmentStatusReport() throws AppException {
        String sql = "SELECT status, COUNT(*) AS total_count, " +
                     "ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM appointments), 0), 1) AS pct " +
                     "FROM appointments GROUP BY status ORDER BY total_count DESC";

        System.out.println("\n=======================================================");
        System.out.println("           REPORT B: APPOINTMENTS BY STATUS            ");
        System.out.println("=======================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-15s | %-12s | %-10s%n", "STATUS", "COUNT", "PERCENTAGE");
            System.out.println("-------------------------------------------------------");
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.printf("%-15s | %-12d | %5.1f%%%n", 
                        rs.getString("status"), rs.getInt("total_count"), rs.getDouble("pct"));
            }
            if (!hasData) {
                System.out.println("No appointment records found.");
            }
            System.out.println("=======================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating appointment status report: " + e.getMessage(), e);
        }
    }

    /**
     * REPORT C: Center-Wise Bookings (Uses LEFT JOIN to show zero-booking centers).
     */
    public void printCenterReport() throws AppException {
        String sql = "SELECT c.center_id, c.center_name, c.city, c.active, " +
                     "COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS active_reservations, " +
                     "COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed_vax, " +
                     "COUNT(a.appointment_id) AS total_all_time " +
                     "FROM centers c " +
                     "LEFT JOIN drives d ON c.center_id = d.center_id " +
                     "LEFT JOIN slots s ON d.drive_id = s.drive_id " +
                     "LEFT JOIN appointments a ON s.slot_id = a.slot_id " +
                     "GROUP BY c.center_id, c.center_name, c.city, c.active " +
                     "ORDER BY active_reservations DESC, c.center_name ASC";

        System.out.println("\n==========================================================================================");
        System.out.println("                            REPORT C: CENTER-WISE BOOKINGS                                ");
        System.out.println("==========================================================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-4s | %-28s | %-12s | %-8s | %-8s | %-9s | %-8s%n", 
                    "ID", "CENTER NAME", "CITY", "STATUS", "BOOKED", "COMPLETED", "TOTAL");
            System.out.println("------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-4d | %-28s | %-12s | %-8s | %-8d | %-9d | %-8d%n",
                        rs.getInt("center_id"),
                        truncate(rs.getString("center_name"), 28),
                        truncate(rs.getString("city"), 12),
                        rs.getBoolean("active") ? "ACTIVE" : "INACTIVE",
                        rs.getInt("active_reservations"),
                        rs.getInt("completed_vax"),
                        rs.getInt("total_all_time"));
            }
            System.out.println("==========================================================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating center report: " + e.getMessage(), e);
        }
    }

    /**
     * REPORT D: Drive-Wise Bookings.
     */
    public void printDriveReport() throws AppException {
        String sql = "SELECT d.drive_id, d.drive_name, c.center_name, v.vaccine_name, " +
                     "COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS active_cnt, " +
                     "COUNT(CASE WHEN a.status = 'CANCELLED' THEN 1 END) AS cancelled_cnt, " +
                     "COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed_cnt, " +
                     "COUNT(a.appointment_id) AS total_cnt " +
                     "FROM drives d " +
                     "JOIN centers c ON d.center_id = c.center_id " +
                     "JOIN vaccines v ON d.vaccine_id = v.vaccine_id " +
                     "LEFT JOIN slots s ON d.drive_id = s.drive_id " +
                     "LEFT JOIN appointments a ON s.slot_id = a.slot_id " +
                     "GROUP BY d.drive_id, d.drive_name, c.center_name, v.vaccine_name " +
                     "ORDER BY active_cnt DESC";

        System.out.println("\n===============================================================================================");
        System.out.println("                                REPORT D: DRIVE-WISE BOOKINGS                                  ");
        System.out.println("===============================================================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-4s | %-28s | %-22s | %-14s | %-6s | %-6s | %-6s | %-6s%n",
                    "ID", "DRIVE NAME", "CENTER", "VACCINE", "ACTIVE", "CANCL", "CMPLT", "TOTAL");
            System.out.println("-----------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-4d | %-28s | %-22s | %-14s | %-6d | %-6d | %-6d | %-6d%n",
                        rs.getInt("drive_id"),
                        truncate(rs.getString("drive_name"), 28),
                        truncate(rs.getString("center_name"), 22),
                        truncate(rs.getString("vaccine_name"), 14),
                        rs.getInt("active_cnt"),
                        rs.getInt("cancelled_cnt"),
                        rs.getInt("completed_cnt"),
                        rs.getInt("total_cnt"));
            }
            System.out.println("===============================================================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating drive report: " + e.getMessage(), e);
        }
    }

    /**
     * REPORT E: Daily Appointments.
     */
    public void printDailyReport() throws AppException {
        String sql = "SELECT s.slot_date, " +
                     "COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS booked, " +
                     "COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed, " +
                     "COUNT(CASE WHEN a.status = 'CANCELLED' THEN 1 END) AS cancelled, " +
                     "COUNT(CASE WHEN a.status = 'MISSED' THEN 1 END) AS missed, " +
                     "COUNT(a.appointment_id) AS total " +
                     "FROM slots s " +
                     "JOIN appointments a ON s.slot_id = a.slot_id " +
                     "GROUP BY s.slot_date ORDER BY s.slot_date ASC";

        System.out.println("\n=======================================================================");
        System.out.println("                     REPORT E: DAILY APPOINTMENTS                      ");
        System.out.println("=======================================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-12s | %-8s | %-10s | %-10s | %-8s | %-8s%n",
                    "DATE", "BOOKED", "COMPLETED", "CANCELLED", "MISSED", "TOTAL");
            System.out.println("-----------------------------------------------------------------------");
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.printf("%-12s | %-8d | %-10d | %-10d | %-8d | %-8d%n",
                        rs.getDate("slot_date"),
                        rs.getInt("booked"),
                        rs.getInt("completed"),
                        rs.getInt("cancelled"),
                        rs.getInt("missed"),
                        rs.getInt("total"));
            }
            if (!hasData) {
                System.out.println("No daily appointment activity found.");
            }
            System.out.println("=======================================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating daily report: " + e.getMessage(), e);
        }
    }

    /**
     * REPORT F: Live Slot Capacity & Availability.
     */
    public void printSlotAvailabilityReport() throws AppException {
        String sql = "SELECT s.slot_id, c.center_name, d.drive_name, v.vaccine_name, s.slot_date, s.start_time, s.end_time, " +
                     "s.capacity, COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) AS booked_count, " +
                     "(s.capacity - COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END)) AS remaining_cap, " +
                     "CASE " +
                     "  WHEN (s.capacity - COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END)) = 0 THEN 'FULL' " +
                     "  WHEN (s.capacity - COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END)) <= 2 THEN 'FILLING FAST' " +
                     "  ELSE 'AVAILABLE' " +
                     "END AS status_badge " +
                     "FROM slots s " +
                     "JOIN drives d ON s.drive_id = d.drive_id " +
                     "JOIN centers c ON d.center_id = c.center_id " +
                     "JOIN vaccines v ON d.vaccine_id = v.vaccine_id " +
                     "LEFT JOIN appointments a ON s.slot_id = a.slot_id " +
                     "WHERE d.active = TRUE AND c.active = TRUE " +
                     "GROUP BY s.slot_id, c.center_name, d.drive_name, v.vaccine_name, s.slot_date, s.start_time, s.end_time, s.capacity " +
                     "ORDER BY s.slot_date ASC, s.start_time ASC";

        System.out.println("\n==============================================================================================================");
        System.out.println("                                REPORT F: LIVE SLOT AVAILABILITY                                              ");
        System.out.println("==============================================================================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-4s | %-20s | %-20s | %-10s | %-11s | %-4s | %-6s | %-9s | %-12s%n",
                    "ID", "CENTER", "DRIVE", "DATE", "TIME", "CAP", "BOOKED", "REMAINING", "STATUS");
            System.out.println("--------------------------------------------------------------------------------------------------------------");
            while (rs.next()) {
                String timeRange = rs.getTime("start_time").toString().substring(0, 5) + "-" + 
                                   rs.getTime("end_time").toString().substring(0, 5);
                System.out.printf("%-4d | %-20s | %-20s | %-10s | %-11s | %-4d | %-6d | %-9d | %-12s%n",
                        rs.getInt("slot_id"),
                        truncate(rs.getString("center_name"), 20),
                        truncate(rs.getString("drive_name"), 20),
                        rs.getDate("slot_date"),
                        timeRange,
                        rs.getInt("capacity"),
                        rs.getInt("booked_count"),
                        rs.getInt("remaining_cap"),
                        rs.getString("status_badge"));
            }
            System.out.println("==============================================================================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating slot availability report: " + e.getMessage(), e);
        }
    }

    /**
     * REPORT G: Completed Immunizations Summary.
     */
    public void printCompletedImmunizationsReport() throws AppException {
        String sql = "SELECT c.center_name, d.drive_name, v.vaccine_name, " +
                     "COUNT(a.appointment_id) AS completed_total " +
                     "FROM appointments a " +
                     "JOIN slots s ON a.slot_id = s.slot_id " +
                     "JOIN drives d ON s.drive_id = d.drive_id " +
                     "JOIN centers c ON d.center_id = c.center_id " +
                     "JOIN vaccines v ON d.vaccine_id = v.vaccine_id " +
                     "WHERE a.status = 'COMPLETED' " +
                     "GROUP BY c.center_name, d.drive_name, v.vaccine_name " +
                     "ORDER BY completed_total DESC";

        System.out.println("\n=========================================================================================");
        System.out.println("                         REPORT G: COMPLETED IMMUNIZATIONS                               ");
        System.out.println("=========================================================================================");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-30s | %-30s | %-15s | %-8s%n", "CENTER", "DRIVE", "VACCINE", "COMPLETED");
            System.out.println("-----------------------------------------------------------------------------------------");
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.printf("%-30s | %-30s | %-15s | %-8d%n",
                        truncate(rs.getString("center_name"), 30),
                        truncate(rs.getString("drive_name"), 30),
                        truncate(rs.getString("vaccine_name"), 15),
                        rs.getInt("completed_total"));
            }
            if (!hasData) {
                System.out.println("No completed vaccinations recorded yet.");
            }
            System.out.println("=========================================================================================\n");

        } catch (SQLException e) {
            throw new DatabaseException("Error generating completed immunizations report: " + e.getMessage(), e);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }
}
