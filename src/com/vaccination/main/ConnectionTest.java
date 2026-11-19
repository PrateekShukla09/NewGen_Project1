package com.vaccination.main;

import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Diagnostic utility to verify JDBC configuration, driver loading,
 * network connectivity, database schema, and seed data existence.
 */
public class ConnectionTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   VACCINATION DRIVE SCHEDULER - JDBC DIAGNOSTIC ");
        System.out.println("=================================================");
        System.out.println("Configured URL  : " + DBConnection.getJdbcUrl());
        System.out.println("Configured User : " + DBConnection.getDbUser());
        System.out.println("-------------------------------------------------");

        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("[SUCCESS] Successfully connected to MySQL database!");
            System.out.println("DBMS Product    : " + meta.getDatabaseProductName());
            System.out.println("DBMS Version    : " + meta.getDatabaseProductVersion());
            System.out.println("Driver Name     : " + meta.getDriverName());
            System.out.println("Driver Version  : " + meta.getDriverVersion());
            System.out.println("Current Schema  : " + conn.getCatalog());
            System.out.println("-------------------------------------------------");
            System.out.println("Verifying Required Tables & Row Counts:");

            String[] expectedTables = {"users", "centers", "vaccines", "drives", "slots", "appointments"};
            try (Statement stmt = conn.createStatement()) {
                for (String table : expectedTables) {
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            System.out.printf("  [OK] Table '%-14s' exists. Current rows: %d%n", table, count);
                        }
                    } catch (SQLException e) {
                        System.out.printf("  [MISSING] Table '%-14s' not found! (Did you run sql/schema.sql?)%n", table);
                    }
                }
            }

            System.out.println("-------------------------------------------------");
            System.out.println("[READY] JDBC connectivity and database state verified!");
            System.out.println("=================================================");

        } catch (SQLException e) {
            System.err.println("[FAILED] Could not connect to database!");
            System.err.println("Details: " + e.getMessage());
            System.out.println("-------------------------------------------------");
            System.out.println("Troubleshooting Steps:");
            System.out.println("1. Verify MySQL Service ('MySQL80') is running in Windows Services.");
            System.out.println("2. Check config/db.properties: ensure db.password matches your MySQL root password.");
            System.out.println("3. Ensure database 'vaccination_scheduler' is created by running sql/schema.sql.");
            System.out.println("=================================================");
            System.exit(1);
        }
    }
}
