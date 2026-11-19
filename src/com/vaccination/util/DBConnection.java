package com.vaccination.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralized JDBC Connection Factory.
 * 
 * Manages database connection acquisition using DriverManager.
 * Reads configuration from external properties file (config/db.properties)
 * or system environment variables, preventing hardcoded credentials.
 * 
 * Note: Never creates a single global shared Connection object.
 * Each independent operation acquires a fresh connection (using try-with-resources),
 * while transactions share one Connection across participating statements.
 */
public class DBConnection {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/vaccination_scheduler?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        loadConfiguration();
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: MySQL JDBC Driver (" + DRIVER_CLASS + ") not found in classpath!");
            System.err.println("Ensure mysql-connector-j-*.jar is placed in the lib/ directory and included in -cp.");
        }
    }

    /**
     * Loads database configuration from environment variables or config/db.properties.
     */
    public static synchronized void loadConfiguration() {
        // 1. Check environment variables first (ideal for cloud/container/CI environments)
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        if (envUrl != null && !envUrl.isBlank()) {
            jdbcUrl = envUrl.trim();
            dbUser = (envUser != null) ? envUser.trim() : DEFAULT_USER;
            dbPassword = (envPass != null) ? envPass.trim() : DEFAULT_PASS;
            return;
        }

        // 2. Load from config/db.properties
        Properties props = new Properties();
        File configFile = resolveConfigFile();

        if (configFile != null && configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
                jdbcUrl = props.getProperty("db.url", DEFAULT_URL).trim();
                dbUser = props.getProperty("db.user", DEFAULT_USER).trim();
                dbPassword = props.getProperty("db.password", DEFAULT_PASS).trim();
                return;
            } catch (IOException e) {
                System.err.println("WARN: Could not read config file at " + configFile.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // 3. Fallback to default local configuration
        jdbcUrl = DEFAULT_URL;
        dbUser = DEFAULT_USER;
        dbPassword = DEFAULT_PASS;
    }

    /**
     * Locates db.properties checking common working directory paths.
     */
    private static File resolveConfigFile() {
        String[] candidatePaths = {
            "config/db.properties",
            "../config/db.properties",
            "VaccinationDriveScheduler/config/db.properties",
            System.getProperty("user.dir") + File.separator + "config" + File.separator + "db.properties"
        };

        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /**
     * Obtains a fresh database connection.
     * Callers must close the returned connection (preferably with try-with-resources).
     *
     * @return an open {@link Connection} to the MySQL database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            String diagnostic = diagnoseConnectionError(e);
            throw new SQLException("Failed to connect to MySQL database at [" + jdbcUrl + "]: " + diagnostic, e);
        }
    }

    /**
     * Analyzes SQL exception to produce user-friendly diagnostic guidance.
     */
    public static String diagnoseConnectionError(SQLException e) {
        String msg = (e.getMessage() != null) ? e.getMessage().toLowerCase() : "";
        if (msg.contains("communications link failure") || msg.contains("connection refused")) {
            return "Cannot reach MySQL server on port 3306. Verify that MySQL service ('MySQL80') is running.";
        } else if (msg.contains("access denied for user")) {
            return "Authentication failed for user '" + dbUser + "'. Check db.password in config/db.properties.";
        } else if (msg.contains("unknown database")) {
            return "Database 'vaccination_scheduler' does not exist. Run sql/schema.sql to create it.";
        }
        return e.getMessage();
    }

    // Getters for configuration inspection
    public static String getJdbcUrl() {
        return jdbcUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }
}
