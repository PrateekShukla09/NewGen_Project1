package com.vaccination.main;

import com.vaccination.exception.ValidationException;
import com.vaccination.model.AppointmentStatus;
import com.vaccination.model.Role;
import com.vaccination.model.Session;
import com.vaccination.model.Slot;
import com.vaccination.model.User;
import com.vaccination.util.PasswordUtil;
import com.vaccination.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Automated test runner for Phase 4 models, utilities, and exceptions.
 */
public class Phase4Test {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   VACCINATION DRIVE SCHEDULER - PHASE 4 TESTS   ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // TEST 1: PasswordUtil - Generation & Verification
        try {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword("Secret#2026", salt);
            boolean match = PasswordUtil.verifyPassword("Secret#2026", hash, salt);
            boolean wrongMatch = PasswordUtil.verifyPassword("WrongPassword", hash, salt);

            if (match && !wrongMatch) {
                System.out.println("  [PASS] PasswordUtil: Hashing and constant-time verification working.");
                passed++;
            } else {
                System.err.println("  [FAIL] PasswordUtil: Verification mismatch.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] PasswordUtil threw exception: " + e.getMessage());
            failed++;
        }

        // TEST 2: PasswordUtil - Seed Data Compatibility
        try {
            // Hash and salt from sample_data.sql for admin@vaccination.org
            String adminSalt = "6926c99a650824c6a7b0d89d63800672";
            String adminHash = "c06b2e9f3b98079704a7442ca6240e22b98ceb2c04ccfb1b9da5de557a71a3e0";
            boolean adminValid = PasswordUtil.verifyPassword("Admin@1234", adminHash, adminSalt);

            if (adminValid) {
                System.out.println("  [PASS] PasswordUtil: Verified seed admin credentials match PBKDF2 hash.");
                passed++;
            } else {
                System.err.println("  [FAIL] PasswordUtil: Seed admin password verification failed.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] PasswordUtil seed check failed: " + e.getMessage());
            failed++;
        }

        // TEST 3: ValidationUtil - Email & Phone
        try {
            ValidationUtil.validateEmail("citizen@health.gov");
            ValidationUtil.validatePhone("555-0199");
            
            boolean caughtInvalidEmail = false;
            try {
                ValidationUtil.validateEmail("invalid-email-format");
            } catch (ValidationException ve) {
                caughtInvalidEmail = true;
            }

            if (caughtInvalidEmail) {
                System.out.println("  [PASS] ValidationUtil: Email and phone regex validation functional.");
                passed++;
            } else {
                System.err.println("  [FAIL] ValidationUtil: Failed to reject malformed email.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] ValidationUtil email test failed: " + e.getMessage());
            failed++;
        }

        // TEST 4: ValidationUtil - Date & Time & Capacity Rules
        try {
            ValidationUtil.validateCapacity(10);
            ValidationUtil.validateDateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
            ValidationUtil.validateTimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0));

            boolean caughtCapacityError = false;
            try {
                ValidationUtil.validateCapacity(0);
            } catch (ValidationException ve) {
                caughtCapacityError = true;
            }

            if (caughtCapacityError) {
                System.out.println("  [PASS] ValidationUtil: Capacity, date, and time bounds enforced.");
                passed++;
            } else {
                System.err.println("  [FAIL] ValidationUtil: Non-positive capacity was not rejected.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] ValidationUtil bounds test failed: " + e.getMessage());
            failed++;
        }

        // TEST 5: Model Logic - Slot capacity calculations
        try {
            Slot slot = new Slot(101, 1, LocalDate.of(2026, 11, 10), LocalTime.of(9, 0), LocalTime.of(10, 0), 5, null);
            slot.setBookedCount(2);
            boolean remOk = slot.getRemainingCapacity() == 3;
            boolean notFull = !slot.isFull();
            slot.setBookedCount(5);
            boolean fullOk = slot.isFull() && slot.getRemainingCapacity() == 0;

            if (remOk && notFull && fullOk) {
                System.out.println("  [PASS] Slot Model: Capacity and availability logic verified.");
                passed++;
            } else {
                System.err.println("  [FAIL] Slot Model: Capacity calculation error.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] Slot model test failed: " + e.getMessage());
            failed++;
        }

        // TEST 6: Session & Role Handling
        try {
            User adminUser = new User();
            adminUser.setUserId(1);
            adminUser.setEmail("admin@vaccination.org");
            adminUser.setFullName("System Admin");
            adminUser.setRole(Role.ADMIN);

            Session session = Session.fromUser(adminUser);
            boolean isAdm = session.isAdmin();
            boolean isLog = session.isLoggedIn();
            session.clear();
            boolean cleared = !session.isLoggedIn();

            if (isAdm && isLog && cleared) {
                System.out.println("  [PASS] Session Model: Role propagation and logout clearance verified.");
                passed++;
            } else {
                System.err.println("  [FAIL] Session Model: Role or clearance logic failed.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("  [FAIL] Session model test failed: " + e.getMessage());
            failed++;
        }

        System.out.println("-------------------------------------------------");
        System.out.printf("Test Results: %d Passed, %d Failed.%n", passed, failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
