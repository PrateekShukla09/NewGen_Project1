package com.vaccination.main;

import com.vaccination.exception.AppException;
import com.vaccination.model.Appointment;
import com.vaccination.model.AppointmentStatus;
import com.vaccination.model.Center;
import com.vaccination.model.Drive;
import com.vaccination.model.Role;
import com.vaccination.model.Session;
import com.vaccination.model.Slot;
import com.vaccination.model.User;
import com.vaccination.model.Vaccine;
import com.vaccination.service.AuthService;
import com.vaccination.service.BookingService;
import com.vaccination.service.CenterService;
import com.vaccination.service.DriveService;
import com.vaccination.service.ReportService;
import com.vaccination.service.SlotService;
import com.vaccination.service.VaccineService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Comprehensive automated integration tester that systematically exercises
 * every service and menu operation in the application.
 */
public class FullSystemTest {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   VACCINATION DRIVE SCHEDULER - FULL SYSTEM TEST SUITE  ");
        System.out.println("=========================================================");

        int passed = 0;
        int failed = 0;

        AuthService authService = new AuthService();
        CenterService centerService = new CenterService();
        VaccineService vaccineService = new VaccineService();
        DriveService driveService = new DriveService();
        SlotService slotService = new SlotService();
        BookingService bookingService = new BookingService();
        ReportService reportService = new ReportService();

        // ---------------------------------------------------------------------
        // 1. AUTHENTICATION MODULE TESTS
        // ---------------------------------------------------------------------
        System.out.println("\n[1/6] Testing Authentication & Session Management...");
        try {
            // Test Admin Login
            Session adminSession = authService.login("admin@vaccination.org", "Admin@1234");
            if (adminSession.isAdmin()) {
                System.out.println("  [PASS] Admin login succeeded with Role.ADMIN.");
                passed++;
            } else {
                System.err.println("  [FAIL] Admin login did not assign ADMIN role.");
                failed++;
            }

            // Test Citizen Registration with unique timestamped email
            String testEmail = "testuser." + System.currentTimeMillis() + "@example.com";
            User registered = authService.registerUser("Test Citizen", testEmail, "555-0999", "Pass#1234", "Pass#1234");
            if (registered.getUserId() > 0 && registered.getRole() == Role.USER) {
                System.out.println("  [PASS] User registration succeeded with Role.USER.");
                passed++;
            } else {
                System.err.println("  [FAIL] User registration failed.");
                failed++;
            }

            // Test Citizen Login
            Session userSession = authService.login(testEmail, "Pass#1234");
            if (userSession.isUser()) {
                System.out.println("  [PASS] Citizen login succeeded with in-memory session.");
                passed++;
            } else {
                System.err.println("  [FAIL] Citizen login failed.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("  [FAIL] Auth test exception: " + e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------------
        // 2. CENTER MANAGEMENT TESTS
        // ---------------------------------------------------------------------
        System.out.println("\n[2/6] Testing Vaccination Center Operations...");
        int createdCenterId = 0;
        try {
            Center center = centerService.addCenter("Apex Testing Clinic", "999 Silicon Way", "Metropolis", "555-8888");
            createdCenterId = center.getCenterId();
            if (createdCenterId > 0) {
                System.out.println("  [PASS] Center creation succeeded (ID #" + createdCenterId + ").");
                passed++;
            } else {
                System.err.println("  [FAIL] Center creation failed.");
                failed++;
            }

            List<Center> searchResults = centerService.searchCenters("Apex");
            if (!searchResults.isEmpty() && searchResults.get(0).getCenterName().contains("Apex")) {
                System.out.println("  [PASS] Center search by keyword verified.");
                passed++;
            } else {
                System.err.println("  [FAIL] Center search returned no matches.");
                failed++;
            }

            centerService.updateCenter(createdCenterId, "Apex Testing Clinic Updated", "999 Silicon Way", "Metropolis", "555-8888", true);
            Center updated = centerService.getCenterById(createdCenterId);
            if ("Apex Testing Clinic Updated".equals(updated.getCenterName())) {
                System.out.println("  [PASS] Center details update verified.");
                passed++;
            } else {
                System.err.println("  [FAIL] Center details update failed.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("  [FAIL] Center operations exception: " + e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------------
        // 3. VACCINE CATALOG TESTS
        // ---------------------------------------------------------------------
        System.out.println("\n[3/6] Testing Vaccine Operations...");
        int createdVaccineId = 0;
        try {
            String vName = "AutoVax-" + (System.currentTimeMillis() % 10000);
            Vaccine vaccine = vaccineService.addVaccine(vName, "RoboBio Labs", "Automated test formulation");
            createdVaccineId = vaccine.getVaccineId();
            if (createdVaccineId > 0) {
                System.out.println("  [PASS] Vaccine creation succeeded (ID #" + createdVaccineId + ").");
                passed++;
            } else {
                System.err.println("  [FAIL] Vaccine creation failed.");
                failed++;
            }

            List<Vaccine> vaxList = vaccineService.searchVaccines(vName);
            if (!vaxList.isEmpty()) {
                System.out.println("  [PASS] Vaccine catalog search verified.");
                passed++;
            } else {
                System.err.println("  [FAIL] Vaccine search returned empty.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("  [FAIL] Vaccine operations exception: " + e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------------
        // 4. DRIVE & SLOT MANAGEMENT TESTS
        // ---------------------------------------------------------------------
        System.out.println("\n[4/6] Testing Drive & Slot Operations...");
        int createdDriveId = 0;
        int createdSlotId = 0;
        try {
            LocalDate startDate = LocalDate.now().plusDays(2);
            LocalDate endDate = LocalDate.now().plusDays(30);
            Drive drive = driveService.createDrive("Spring Boost Campaign", createdCenterId, createdVaccineId, startDate, endDate);
            createdDriveId = drive.getDriveId();
            if (createdDriveId > 0) {
                System.out.println("  [PASS] Drive creation succeeded (ID #" + createdDriveId + ").");
                passed++;
            } else {
                System.err.println("  [FAIL] Drive creation failed.");
                failed++;
            }

            // Create Slot within drive date range
            LocalDate slotDate = startDate.plusDays(1);
            Slot slot = slotService.createSlot(createdDriveId, slotDate, LocalTime.of(10, 0), LocalTime.of(11, 0), 5);
            createdSlotId = slot.getSlotId();
            if (createdSlotId > 0 && slot.getCapacity() == 5) {
                System.out.println("  [PASS] Slot creation within drive calendar verified (Slot #" + createdSlotId + ").");
                passed++;
            } else {
                System.err.println("  [FAIL] Slot creation failed.");
                failed++;
            }

            // Test overlap prevention: try creating identical overlapping time at same center
            boolean overlapBlocked = false;
            try {
                slotService.createSlot(createdDriveId, slotDate, LocalTime.of(10, 30), LocalTime.of(11, 30), 5);
            } catch (AppException ae) {
                overlapBlocked = true;
            }
            if (overlapBlocked) {
                System.out.println("  [PASS] Overlapping slot at same center successfully rejected.");
                passed++;
            } else {
                System.err.println("  [FAIL] Overlapping slot was improperly allowed.");
                failed++;
            }

            // Test capacity update
            slotService.updateSlotCapacity(createdSlotId, 8);
            Slot updatedSlot = slotService.getSlotById(createdSlotId);
            if (updatedSlot.getCapacity() == 8) {
                System.out.println("  [PASS] Safe slot capacity update verified (5 -> 8).");
                passed++;
            } else {
                System.err.println("  [FAIL] Slot capacity update failed.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("  [FAIL] Drive/Slot operations exception: " + e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------------
        // 5. APPOINTMENT BOOKING & CANCELLATION TESTS
        // ---------------------------------------------------------------------
        System.out.println("\n[5/6] Testing Booking Engine & Cancellation Lifecycle...");
        long bookedApptId = 0;
        try {
            int testUserId = 2; // John Doe
            Appointment appt = bookingService.bookAppointment(testUserId, createdSlotId);
            bookedApptId = appt.getAppointmentId();

            if (bookedApptId > 0 && appt.getStatus() == AppointmentStatus.BOOKED) {
                System.out.println("  [PASS] Appointment booking succeeded (Receipt #" + bookedApptId + ").");
                passed++;
            } else {
                System.err.println("  [FAIL] Appointment booking failed.");
                failed++;
            }

            // Verify duplicate booking prevention
            boolean dupBlocked = false;
            try {
                bookingService.bookAppointment(testUserId, createdSlotId);
            } catch (AppException ae) {
                dupBlocked = true;
            }
            if (dupBlocked) {
                System.out.println("  [PASS] Duplicate active booking for same user and slot blocked.");
                passed++;
            } else {
                System.err.println("  [FAIL] Duplicate active booking was allowed!");
                failed++;
            }

            // Verify Cancellation releases capacity
            bookingService.cancelAppointment(bookedApptId, testUserId, false);
            Appointment cancelledAppt = bookingService.getAppointmentById(bookedApptId);
            if (cancelledAppt.getStatus() == AppointmentStatus.CANCELLED) {
                System.out.println("  [PASS] Appointment cancellation verified (Status: CANCELLED).");
                passed++;
            } else {
                System.err.println("  [FAIL] Cancellation status not updated.");
                failed++;
            }

            // Verify re-booking after cancellation is permitted!
            Appointment rebooked = bookingService.bookAppointment(testUserId, createdSlotId);
            if (rebooked.getAppointmentId() > 0 && rebooked.getStatus() == AppointmentStatus.BOOKED) {
                System.out.println("  [PASS] Re-booking after cancellation verified (Virtual column uniqueness verified).");
                passed++;
            } else {
                System.err.println("  [FAIL] Re-booking failed.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("  [FAIL] Booking lifecycle exception: " + e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------------
        // 6. ADMINISTRATIVE REPORTS VERIFICATION
        // ---------------------------------------------------------------------
        System.out.println("\n[6/6] Verifying All 7 Administrative SQL Reports...");
        try {
            reportService.printUserReport();
            reportService.printAppointmentStatusReport();
            reportService.printCenterReport();
            reportService.printDriveReport();
            reportService.printDailyReport();
            reportService.printSlotAvailabilityReport();
            reportService.printCompletedImmunizationsReport();
            System.out.println("  [PASS] All 7 administrative reports executed with zero SQL errors.");
            passed++;
        } catch (Exception e) {
            System.err.println("  [FAIL] Reporting error: " + e.getMessage());
            failed++;
        }

        System.out.println("\n=========================================================");
        System.out.printf("FULL SYSTEM TEST RESULTS: %d Passed, %d Failed.%n", passed, failed);
        System.out.println("=========================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
