package com.vaccination.main;

import com.vaccination.dao.AppointmentDAO;
import com.vaccination.dao.SlotDAO;
import com.vaccination.exception.AppException;
import com.vaccination.model.Appointment;
import com.vaccination.model.Drive;
import com.vaccination.model.Slot;
import com.vaccination.service.BookingService;
import com.vaccination.service.DriveService;
import com.vaccination.service.SlotService;
import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency Test Utility.
 * 
 * Simulates high-concurrency race conditions using multi-threaded execution
 * and CountDownLatch synchronization to verify pessimistic row locking (SELECT ... FOR UPDATE).
 */
public class ConcurrencyBookingTest {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   VACCINATION DRIVE SCHEDULER - CONCURRENCY VERIFIER   ");
        System.out.println("=========================================================");

        // Test database connectivity first
        try (Connection testConn = DBConnection.getConnection()) {
            System.out.println("[INFO] Connected to MySQL. Preparing concurrency test environment...");
        } catch (Exception e) {
            System.err.println("[FAILED] Cannot connect to database. Ensure MySQL is running and db.properties has the right password.");
            System.err.println("Details: " + e.getMessage());
            return;
        }

        BookingService bookingService = new BookingService();
        SlotService slotService = new SlotService();
        DriveService driveService = new DriveService();
        SlotDAO slotDAO = new SlotDAO();

        try {
            // -----------------------------------------------------------------
            // TEST A: Two simultaneous users competing for the LAST remaining place
            // -----------------------------------------------------------------
            System.out.println("\n[TEST A] Simultaneous booking competition for a slot with Capacity = 1...");
            
            // Create a dedicated slot matching Drive 1's active calendar with capacity = 1
            Drive drive1 = driveService.getDriveById(1);
            LocalDate testDate = drive1.getStartDate();
            LocalTime startTime = LocalTime.of(16, 0);
            LocalTime endTime = LocalTime.of(17, 0);

            // Clean up any old test slot with this timing if previously created
            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM appointments WHERE slot_id IN (SELECT slot_id FROM slots WHERE drive_id = 1 AND slot_date = '" + testDate + "' AND start_time = '16:00:00')");
                stmt.executeUpdate("DELETE FROM slots WHERE drive_id = 1 AND slot_date = '" + testDate + "' AND start_time = '16:00:00'");
            } catch (Exception ignored) {}

            Slot testSlot = slotService.createSlot(1, testDate, startTime, endTime, 1);
            int slotId = testSlot.getSlotId();
            System.out.printf("[INFO] Created Test Slot #%d with capacity = 1 on %s at %s%n", slotId, testDate, startTime);

            // Set up 2 competing threads (User 2 and User 3)
            int userA = 2; // John Doe
            int userB = 3; // Alice Smith

            CountDownLatch startGate = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> outcomes = new ArrayList<>();

            Callable<Void> taskA = () -> {
                startGate.await(); // Wait for simultaneous release
                try {
                    bookingService.bookAppointment(userA, slotId);
                    successCount.incrementAndGet();
                    synchronized (outcomes) { outcomes.add("User A (" + userA + ") SUCCEEDED"); }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    synchronized (outcomes) { outcomes.add("User A (" + userA + ") REJECTED: " + e.getMessage()); }
                }
                return null;
            };

            Callable<Void> taskB = () -> {
                startGate.await(); // Wait for simultaneous release
                try {
                    bookingService.bookAppointment(userB, slotId);
                    successCount.incrementAndGet();
                    synchronized (outcomes) { outcomes.add("User B (" + userB + ") SUCCEEDED"); }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    synchronized (outcomes) { outcomes.add("User B (" + userB + ") REJECTED: " + e.getMessage()); }
                }
                return null;
            };

            Future<Void> futureA = executor.submit(taskA);
            Future<Void> futureB = executor.submit(taskB);

            // Release the gate simultaneously
            startGate.countDown();

            futureA.get(5, TimeUnit.SECONDS);
            futureB.get(5, TimeUnit.SECONDS);
            executor.shutdown();

            System.out.println("Execution Outcomes:");
            outcomes.forEach(o -> System.out.println("  -> " + o));

            int finalActiveBookings = slotDAO.getActiveBookingCount(slotId);
            System.out.printf("[RESULT] Successes: %d | Rejections: %d | Final DB Bookings: %d (Slot Capacity: 1)%n",
                    successCount.get(), failureCount.get(), finalActiveBookings);

            if (successCount.get() == 1 && failureCount.get() == 1 && finalActiveBookings == 1) {
                System.out.println(">>> [PASS] CONCURRENCY INTEGRITY VERIFIED: Overbooking was strictly prevented! <<<");
            } else {
                System.err.println(">>> [FAIL] Concurrency failure: Capacity was exceeded or multiple reservations occurred! <<<");
            }

            // -----------------------------------------------------------------
            // TEST B: Duplicate Active Booking by the SAME user
            // -----------------------------------------------------------------
            System.out.println("\n[TEST B] Duplicate active booking check for the same user...");
            try {
                // Determine who won Test A, and attempt to book the same slot again with that user
                int winnerId = outcomes.get(0).contains("User A") && outcomes.get(0).contains("SUCCEEDED") ? userA : userB;
                System.out.println("[INFO] Attempting second active booking for winning User #" + winnerId + " on Slot #" + slotId);
                bookingService.bookAppointment(winnerId, slotId);
                System.err.println(">>> [FAIL] Duplicate active booking was unexpectedly allowed! <<<");
            } catch (AppException e) {
                System.out.println("[PASS] Correctly blocked duplicate booking: " + e.getMessage());
            }

            System.out.println("\n=========================================================");
            System.out.println("           CONCURRENCY TEST SUITE COMPLETE               ");
            System.out.println("=========================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Concurrency test encountered an exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
