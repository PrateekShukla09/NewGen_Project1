package com.vaccination.util;

import java.io.Console;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Resilient console input utility.
 * 
 * Designed to completely prevent the infamous Scanner newline bug by always
 * consuming entire lines using nextLine() and parsing tokens explicitly.
 * Handles invalid input without crashing or throwing unhandled exceptions.
 */
public class InputUtil {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Reads a mandatory non-empty trimmed string from the console.
     */
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            if (!SCANNER.hasNextLine()) {
                throw new IllegalStateException("Input stream reached EOF or was closed.");
            }
            String input = SCANNER.nextLine();
            if (input != null && !input.trim().isEmpty()) {
                return input.trim();
            }
            System.out.println("  [!] Input cannot be empty. Please try again.");
        }
    }

    /**
     * Reads an optional string. If user presses Enter without typing, returns defaultValue.
     */
    public static String readOptionalString(String prompt, String defaultValue) {
        System.out.print(prompt + " [" + (defaultValue != null ? defaultValue : "") + "]: ");
        if (!SCANNER.hasNextLine()) {
            return defaultValue;
        }
        String input = SCANNER.nextLine();
        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }
        return input.trim();
    }

    /**
     * Reads a valid integer from the console.
     */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            if (!SCANNER.hasNextLine()) {
                throw new IllegalStateException("Input stream reached EOF or was closed.");
            }
            String line = SCANNER.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please enter an integer value.");
            }
        }
    }

    /**
     * Reads an integer within a specified range [min, max].
     */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("  [!] Value must be between %d and %d. Please try again.%n", min, max);
        }
    }

    /**
     * Reads a valid date formatted as YYYY-MM-DD.
     */
    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (YYYY-MM-DD): ");
            if (!SCANNER.hasNextLine()) {
                throw new IllegalStateException("Input stream reached EOF or was closed.");
            }
            String line = SCANNER.nextLine().trim();
            try {
                return LocalDate.parse(line, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Please use YYYY-MM-DD (e.g., 2026-11-15).");
            }
        }
    }

    /**
     * Reads a valid 24-hour time formatted as HH:mm.
     */
    public static LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt + " (HH:mm, 24-hour): ");
            if (!SCANNER.hasNextLine()) {
                throw new IllegalStateException("Input stream reached EOF or was closed.");
            }
            String line = SCANNER.nextLine().trim();
            try {
                return LocalTime.parse(line, TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid time format. Please use HH:mm (e.g., 09:30 or 14:00).");
            }
        }
    }

    /**
     * Reads a password safely from standard input, eliminating console desynchronization
     * and newline buffer traps across VS Code terminal, PowerShell, CMD, and automated scripts.
     */
    public static String readPassword(String prompt) {
        return readString(prompt);
    }

    /**
     * Reads a yes/no confirmation from the user.
     */
    public static boolean readConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            if (!SCANNER.hasNextLine()) {
                return false;
            }
            String input = SCANNER.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("  [!] Please enter 'y' for yes or 'n' for no.");
        }
    }
}
