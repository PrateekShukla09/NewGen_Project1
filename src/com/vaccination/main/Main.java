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
import com.vaccination.util.InputUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Main application entry point for the Vaccination Drive Scheduler.
 * 
 * Provides an interactive console interface adhering strictly to layered architecture:
 * all persistence and business operations are delegated to Service classes.
 * Direct SQL and DAO interactions do not exist in this class.
 */
public class Main {

    private final AuthService authService;
    private final CenterService centerService;
    private final VaccineService vaccineService;
    private final DriveService driveService;
    private final SlotService slotService;
    private final BookingService bookingService;
    private final ReportService reportService;

    private final Session currentSession;

    public Main() {
        this.authService = new AuthService();
        this.centerService = new CenterService();
        this.vaccineService = new VaccineService();
        this.driveService = new DriveService();
        this.slotService = new SlotService();
        this.bookingService = new BookingService();
        this.reportService = new ReportService();
        this.currentSession = new Session();
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        printBanner();
        checkAndPromptAdminBootstrap();

        boolean running = true;
        while (running) {
            if (!currentSession.isLoggedIn()) {
                running = handleMainMenu();
            } else if (currentSession.isAdmin()) {
                handleAdminMenu();
            } else {
                handleUserMenu();
            }
        }
        System.out.println("\nThank you for using the Vaccination Drive Scheduler. Goodbye!\n");
    }

    private void printBanner() {
        System.out.println("=========================================================");
        System.out.println("              VACCINATION DRIVE SCHEDULER                ");
        System.out.println("            Core Java + JDBC + MySQL Edition             ");
        System.out.println("=========================================================");
    }

    private void checkAndPromptAdminBootstrap() {
        try {
            if (!authService.hasAdminAccount()) {
                System.out.println("\n[NOTICE] No administrator accounts detected in the database.");
                System.out.println("Let us set up the first system administrator account.");
                String name = InputUtil.readString("Administrator Full Name");
                String email = InputUtil.readString("Administrator Email");
                String phone = InputUtil.readString("Administrator Phone");
                String pass = InputUtil.readPassword("Administrator Password (min 8 chars)");
                String confirmPass = InputUtil.readPassword("Confirm Password");

                User admin = authService.bootstrapFirstAdmin(name, email, phone, pass, confirmPass);
                System.out.println("[SUCCESS] Initial administrator account created for " + admin.getEmail());
            }
        } catch (Exception e) {
            System.err.println("[WARN] Initial bootstrap check encountered an issue: " + e.getMessage());
        }
    }

    // =========================================================================
    // 1. MAIN MENU (UNAUTHENTICATED)
    // =========================================================================
    private boolean handleMainMenu() {
        System.out.println("\n========================================");
        System.out.println("MAIN MENU");
        System.out.println("========================================");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.println("========================================");

        int choice = InputUtil.readInt("Enter your choice", 1, 3);
        switch (choice) {
            case 1 -> handleRegister();
            case 2 -> handleLogin();
            case 3 -> { return false; }
        }
        return true;
    }

    private void handleRegister() {
        System.out.println("\n--- CITIZEN REGISTRATION ---");
        try {
            String name = InputUtil.readString("Full Name");
            String email = InputUtil.readString("Email Address");
            String phone = InputUtil.readString("Phone Number");
            String password = InputUtil.readPassword("Password (min 8 characters)");
            String confirmPass = InputUtil.readPassword("Confirm Password");

            User user = authService.registerUser(name, email, phone, password, confirmPass);
            System.out.println("\n[SUCCESS] Registration successful! Welcome, " + user.getFullName() + ".");
            System.out.println("You may now log in with your credentials.");
        } catch (AppException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void handleLogin() {
        System.out.println("\n--- LOGIN ---");
        try {
            String email = InputUtil.readString("Email Address");
            String password = InputUtil.readPassword("Password");

            Session session = authService.login(email, password);
            currentSession.copyFrom(session);
            System.out.printf("%n[SUCCESS] Login successful! Welcome %s (%s)%n", session.getFullName(), session.getRole());
        } catch (AppException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. USER MENU
    // =========================================================================
    private void handleUserMenu() {
        System.out.println("\n========================================");
        System.out.println("USER MENU - " + currentSession.getFullName());
        System.out.println("========================================");
        System.out.println("1. View Vaccination Centers");
        System.out.println("2. Browse Vaccination Drives");
        System.out.println("3. Search Drives by City");
        System.out.println("4. View Available Slots for a Drive");
        System.out.println("5. Book Appointment");
        System.out.println("6. View My Upcoming Appointments");
        System.out.println("7. View Appointment History");
        System.out.println("8. Cancel Appointment");
        System.out.println("9. Logout");
        System.out.println("========================================");

        int choice = InputUtil.readInt("Enter your choice", 1, 9);
        try {
            switch (choice) {
                case 1 -> viewActiveCenters();
                case 2 -> browseDrives();
                case 3 -> searchDrivesByCity();
                case 4 -> viewAvailableSlots();
                case 5 -> handleBooking();
                case 6 -> viewUpcomingAppointments();
                case 7 -> viewAppointmentHistory();
                case 8 -> handleCancelAppointment();
                case 9 -> handleLogout();
            }
        } catch (AppException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void viewActiveCenters() throws AppException {
        System.out.println("\n--- ACTIVE VACCINATION CENTERS ---");
        List<Center> centers = centerService.getActiveCenters();
        if (centers.isEmpty()) {
            System.out.println("No active vaccination centers found.");
            return;
        }
        centers.forEach(System.out::println);
    }

    private void browseDrives() throws AppException {
        System.out.println("\n--- ACTIVE VACCINATION DRIVES ---");
        List<Drive> drives = driveService.getActiveDrives();
        if (drives.isEmpty()) {
            System.out.println("No active drives scheduled at this time.");
            return;
        }
        drives.forEach(System.out::println);
    }

    private void searchDrivesByCity() throws AppException {
        String city = InputUtil.readString("Enter City Name to search");
        List<Drive> drives = driveService.getDrivesByCity(city);
        if (drives.isEmpty()) {
            System.out.println("No active drives found in city: '" + city + "'.");
            return;
        }
        System.out.println("\n--- DRIVES IN " + city.toUpperCase() + " ---");
        drives.forEach(System.out::println);
    }

    private void viewAvailableSlots() throws AppException {
        System.out.println("\n--- VIEW SLOTS BY DRIVE ---");
        List<Drive> activeDrives = driveService.getActiveDrives();
        if (activeDrives.isEmpty()) {
            System.out.println("No active vaccination drives found.");
            return;
        }
        System.out.println("Active Drives:");
        activeDrives.forEach(d -> System.out.printf("  [%d] %-30s | %s (%s)%n", 
                d.getDriveId(), d.getDriveName(), d.getCenterName(), d.getCenterCity()));

        int driveId = InputUtil.readInt("Enter Drive ID (or 0 to cancel)");
        if (driveId == 0) return;
        List<Slot> slots = slotService.getAvailableSlotsByDrive(driveId);
        if (slots.isEmpty()) {
            System.out.println("No available slots found for Drive #" + driveId + " or all slots are fully booked.");
            return;
        }
        System.out.println("\n--- AVAILABLE SLOTS FOR DRIVE #" + driveId + " ---");
        slots.forEach(System.out::println);
    }

    private void handleBooking() throws AppException {
        System.out.println("\n--- BOOK AN APPOINTMENT ---");
        List<Drive> activeDrives = driveService.getActiveDrives();
        if (activeDrives.isEmpty()) {
            System.out.println("No active vaccination drives currently available.");
            return;
        }

        System.out.println("Available Drives:");
        activeDrives.forEach(d -> System.out.printf("  [%d] %-30s | %s (%s)%n", 
                d.getDriveId(), d.getDriveName(), d.getCenterName(), d.getCenterCity()));

        int driveId = 0;
        List<Slot> slots = null;

        while (true) {
            driveId = InputUtil.readInt("Enter Drive ID to view its slots (or 0 to cancel)");
            if (driveId == 0) {
                System.out.println("Booking cancelled.");
                return;
            }
            slots = slotService.getAvailableSlotsByDrive(driveId);
            if (slots.isEmpty()) {
                System.out.printf("[!] Drive #%d currently has no available future slots. Please choose another Drive ID.%n", driveId);
            } else {
                break;
            }
        }

        System.out.println("\n--- AVAILABLE SLOTS FOR DRIVE #" + driveId + " ---");
        slots.forEach(System.out::println);

        int slotId = InputUtil.readInt("\nEnter Slot ID you wish to reserve (or 0 to cancel)");
        if (slotId == 0) {
            System.out.println("Booking cancelled.");
            return;
        }

        final int selectedSlotId = slotId;
        boolean slotValid = slots.stream().anyMatch(s -> s.getSlotId() == selectedSlotId);
        if (!slotValid) {
            System.err.println("[ERROR] Slot #" + slotId + " is not an available slot for Drive #" + driveId + ".");
            return;
        }

        boolean confirm = InputUtil.readConfirmation("Confirm reservation for Slot #" + slotId + "?");
        if (!confirm) {
            System.out.println("Booking cancelled by user.");
            return;
        }

        Appointment appt = bookingService.bookAppointment(currentSession.getUserId(), slotId);

        System.out.println("\n=======================================================");
        System.out.println("             BOOKING CONFIRMATION RECEIPT              ");
        System.out.println("=======================================================");
        System.out.println("Appointment ID : " + appt.getAppointmentId());
        System.out.println("Citizen Name   : " + appt.getUserName());
        System.out.println("Center Name    : " + appt.getCenterName() + " (" + appt.getCenterCity() + ")");
        System.out.println("Center Address : " + appt.getCenterAddress());
        System.out.println("Drive Name     : " + appt.getDriveName());
        System.out.println("Vaccine Name   : " + appt.getVaccineName());
        System.out.println("Scheduled Date : " + appt.getSlotDate());
        System.out.println("Time Interval  : " + appt.getStartTime() + " - " + appt.getEndTime());
        System.out.println("Booking Time   : " + appt.getBookingTime());
        System.out.println("Status         : " + appt.getStatus());
        System.out.println("=======================================================");
        System.out.println("[SUCCESS] Appointment successfully reserved!\n");
    }

    private void viewUpcomingAppointments() throws AppException {
        System.out.println("\n--- MY UPCOMING APPOINTMENTS ---");
        List<Appointment> appts = bookingService.getUpcomingAppointments(currentSession.getUserId());
        if (appts.isEmpty()) {
            System.out.println("You have no upcoming appointments.");
            return;
        }
        appts.forEach(System.out::println);
    }

    private void viewAppointmentHistory() throws AppException {
        System.out.println("\n--- ALL APPOINTMENT HISTORY ---");
        List<Appointment> appts = bookingService.getAppointmentHistory(currentSession.getUserId());
        if (appts.isEmpty()) {
            System.out.println("No appointment history found.");
            return;
        }
        appts.forEach(System.out::println);
    }

    private void handleCancelAppointment() throws AppException {
        System.out.println("\n--- CANCEL APPOINTMENT ---");
        List<Appointment> upcoming = bookingService.getUpcomingAppointments(currentSession.getUserId());
        if (upcoming.isEmpty()) {
            System.out.println("You have no active appointments eligible for cancellation.");
            return;
        }

        System.out.println("Your Active Appointments:");
        upcoming.forEach(System.out::println);

        long apptId = (long) InputUtil.readInt("\nEnter Appointment ID to cancel (or 0 to go back)");
        if (apptId == 0) {
            System.out.println("Cancellation aborted.");
            return;
        }

        final long targetApptId = apptId;
        boolean apptValid = upcoming.stream().anyMatch(a -> a.getAppointmentId() == targetApptId);
        if (!apptValid) {
            System.err.println("[ERROR] Appointment #" + apptId + " was not found in your list of active appointments.");
            return;
        }

        boolean confirm = InputUtil.readConfirmation("Are you sure you want to cancel Appointment #" + apptId + "?");
        if (!confirm) {
            System.out.println("Cancellation aborted.");
            return;
        }

        bookingService.cancelAppointment(apptId, currentSession.getUserId(), false);
        System.out.println("[SUCCESS] Appointment #" + apptId + " has been cancelled. Capacity has been released.");
    }

    private void handleLogout() {
        System.out.println("\nLogging out " + currentSession.getEmail() + "...");
        currentSession.clear();
        System.out.println("[SUCCESS] Session cleared.");
    }

    // =========================================================================
    // 3. ADMIN MENU
    // =========================================================================
    private void handleAdminMenu() {
        System.out.println("\n========================================");
        System.out.println("ADMINISTRATIVE DASHBOARD");
        System.out.println("========================================");
        System.out.println("1. Manage Vaccination Centers");
        System.out.println("2. Manage Vaccines");
        System.out.println("3. Manage Vaccination Drives");
        System.out.println("4. Manage Appointment Slots");
        System.out.println("5. View All Appointments");
        System.out.println("6. Search Appointments by ID");
        System.out.println("7. Update Appointment Status");
        System.out.println("8. View Administrative Reports");
        System.out.println("9. Logout");
        System.out.println("========================================");

        int choice = InputUtil.readInt("Enter your choice", 1, 9);
        switch (choice) {
            case 1 -> handleManageCenters();
            case 2 -> handleManageVaccines();
            case 3 -> handleManageDrives();
            case 4 -> handleManageSlots();
            case 5 -> handleViewAllAppointments();
            case 6 -> handleSearchAppointmentById();
            case 7 -> handleUpdateAppointmentStatus();
            case 8 -> handleReportsSubmenu();
            case 9 -> handleLogout();
        }
    }

    // --- Center Management ---
    private void handleManageCenters() {
        boolean loop = true;
        while (loop) {
            System.out.println("\n--- MANAGE CENTERS ---");
            System.out.println("1. Add New Center");
            System.out.println("2. List All Centers");
            System.out.println("3. Search Centers by Name or City");
            System.out.println("4. Update Center Details");
            System.out.println("5. Toggle Center Active Status");
            System.out.println("6. Return to Admin Menu");

            int choice = InputUtil.readInt("Select option", 1, 6);
            try {
                switch (choice) {
                    case 1 -> {
                        String name = InputUtil.readString("Center Name");
                        String address = InputUtil.readString("Street Address");
                        String city = InputUtil.readString("City");
                        String phone = InputUtil.readString("Phone Number");
                        Center c = centerService.addCenter(name, address, city, phone);
                        System.out.println("[SUCCESS] Center created: " + c);
                    }
                    case 2 -> {
                        List<Center> centers = centerService.getAllCenters();
                        if (centers.isEmpty()) {
                            System.out.println("No centers found.");
                        } else {
                            centers.forEach(System.out::println);
                        }
                    }
                    case 3 -> {
                        String query = InputUtil.readString("Search keyword");
                        List<Center> results = centerService.searchCenters(query);
                        if (results.isEmpty()) {
                            System.out.println("No matching centers found for keyword: " + query);
                        } else {
                            results.forEach(System.out::println);
                        }
                    }
                    case 4 -> {
                        List<Center> centers = centerService.getAllCenters();
                        System.out.println("\nExisting Centers:");
                        centers.forEach(c -> System.out.printf("  [%d] %-30s | %s | %s%n",
                                c.getCenterId(), c.getCenterName(), c.getCity(), (c.isActive() ? "ACTIVE" : "INACTIVE")));
                        int id = InputUtil.readInt("Center ID to update (or 0 to cancel)");
                        if (id == 0) break;
                        Center existing = centerService.getCenterById(id);
                        System.out.println("Editing: " + existing);
                        String name = InputUtil.readOptionalString("New Center Name", existing.getCenterName());
                        String addr = InputUtil.readOptionalString("New Address", existing.getAddress());
                        String city = InputUtil.readOptionalString("New City", existing.getCity());
                        String phone = InputUtil.readOptionalString("New Phone", existing.getPhone());
                        boolean active = InputUtil.readConfirmation("Is Center Active?");
                        centerService.updateCenter(id, name, addr, city, phone, active);
                        System.out.println("[SUCCESS] Center #" + id + " updated.");
                    }
                    case 5 -> {
                        List<Center> centers = centerService.getAllCenters();
                        System.out.println("\nExisting Centers:");
                        centers.forEach(c -> System.out.printf("  [%d] %-30s | Status: %s%n",
                                c.getCenterId(), c.getCenterName(), (c.isActive() ? "ACTIVE" : "INACTIVE")));
                        int id = InputUtil.readInt("Center ID to toggle status (or 0 to cancel)");
                        if (id == 0) break;
                        Center existing = centerService.getCenterById(id);
                        boolean newStatus = !existing.isActive();
                        boolean confirm = InputUtil.readConfirmation("Set status for Center #" + id + " to " + (newStatus ? "ACTIVE" : "INACTIVE") + "?");
                        if (!confirm) break;
                        centerService.toggleActiveStatus(id, newStatus);
                        System.out.println("[SUCCESS] Center #" + id + " status set to: " + (newStatus ? "ACTIVE" : "INACTIVE"));
                    }
                    case 6 -> loop = false;
                }
            } catch (AppException e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
    }

    // --- Vaccine Management ---
    private void handleManageVaccines() {
        boolean loop = true;
        while (loop) {
            System.out.println("\n--- MANAGE VACCINES ---");
            System.out.println("1. Add New Vaccine Record");
            System.out.println("2. List All Vaccines");
            System.out.println("3. Search Vaccines");
            System.out.println("4. Update Vaccine Details");
            System.out.println("5. Toggle Vaccine Active Status");
            System.out.println("6. Return to Admin Menu");

            int choice = InputUtil.readInt("Select option", 1, 6);
            try {
                switch (choice) {
                    case 1 -> {
                        String name = InputUtil.readString("Vaccine Name");
                        String mfr = InputUtil.readString("Manufacturer");
                        String desc = InputUtil.readString("Description");
                        Vaccine v = vaccineService.addVaccine(name, mfr, desc);
                        System.out.println("[SUCCESS] Vaccine record created: " + v);
                    }
                    case 2 -> {
                        List<Vaccine> list = vaccineService.getAllVaccines();
                        if (list.isEmpty()) {
                            System.out.println("No vaccines found.");
                        } else {
                            list.forEach(System.out::println);
                        }
                    }
                    case 3 -> {
                        String query = InputUtil.readString("Search keyword");
                        List<Vaccine> results = vaccineService.searchVaccines(query);
                        if (results.isEmpty()) {
                            System.out.println("No matching vaccines found for keyword: " + query);
                        } else {
                            results.forEach(System.out::println);
                        }
                    }
                    case 4 -> {
                        List<Vaccine> list = vaccineService.getAllVaccines();
                        System.out.println("\nExisting Vaccines:");
                        list.forEach(v -> System.out.printf("  [%d] %-25s | Mfr: %-20s | Status: %s%n",
                                v.getVaccineId(), v.getVaccineName(), (v.getManufacturer() != null ? v.getManufacturer() : "N/A"), (v.isActive() ? "ACTIVE" : "INACTIVE")));
                        int id = InputUtil.readInt("Vaccine ID to update (or 0 to cancel)");
                        if (id == 0) break;
                        Vaccine existing = vaccineService.getVaccineById(id);
                        String name = InputUtil.readOptionalString("Vaccine Name", existing.getVaccineName());
                        String mfr = InputUtil.readOptionalString("Manufacturer", existing.getManufacturer());
                        String desc = InputUtil.readOptionalString("Description", existing.getDescription());
                        boolean active = InputUtil.readConfirmation("Is Vaccine Active?");
                        vaccineService.updateVaccine(id, name, mfr, desc, active);
                        System.out.println("[SUCCESS] Vaccine #" + id + " updated.");
                    }
                    case 5 -> {
                        List<Vaccine> list = vaccineService.getAllVaccines();
                        System.out.println("\nExisting Vaccines:");
                        list.forEach(v -> System.out.printf("  [%d] %-25s | Status: %s%n",
                                v.getVaccineId(), v.getVaccineName(), (v.isActive() ? "ACTIVE" : "INACTIVE")));
                        int id = InputUtil.readInt("Vaccine ID to toggle status (or 0 to cancel)");
                        if (id == 0) break;
                        Vaccine existing = vaccineService.getVaccineById(id);
                        boolean newStatus = !existing.isActive();
                        boolean confirm = InputUtil.readConfirmation("Set status for Vaccine #" + id + " to " + (newStatus ? "ACTIVE" : "INACTIVE") + "?");
                        if (!confirm) break;
                        vaccineService.toggleActiveStatus(id, newStatus);
                        System.out.println("[SUCCESS] Vaccine #" + id + " status set to: " + (newStatus ? "ACTIVE" : "INACTIVE"));
                    }
                    case 6 -> loop = false;
                }
            } catch (AppException e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
    }

    // --- Drive Management ---
    private void handleManageDrives() {
        boolean loop = true;
        while (loop) {
            System.out.println("\n--- MANAGE VACCINATION DRIVES ---");
            System.out.println("1. Create New Drive");
            System.out.println("2. List All Drives");
            System.out.println("3. View Upcoming Drives");
            System.out.println("4. View Currently Active Drives");
            System.out.println("5. View Completed Drives");
            System.out.println("6. Update Drive Details");
            System.out.println("7. Toggle Drive Active Status");
            System.out.println("8. Return to Admin Menu");

            int choice = InputUtil.readInt("Select option", 1, 8);
            try {
                switch (choice) {
                    case 1 -> {
                        String name = InputUtil.readString("Drive Name");

                        System.out.println("Active Centers:");
                        centerService.getActiveCenters().forEach(c -> System.out.printf("  [%d] %s (%s)%n", c.getCenterId(), c.getCenterName(), c.getCity()));
                        int centerId = InputUtil.readInt("Center ID");

                        System.out.println("Active Vaccines:");
                        vaccineService.getActiveVaccines().forEach(v -> System.out.printf("  [%d] %s (%s)%n", v.getVaccineId(), v.getVaccineName(), (v.getManufacturer() != null ? v.getManufacturer() : "N/A")));
                        int vaccineId = InputUtil.readInt("Vaccine ID");

                        LocalDate start = InputUtil.readDate("Start Date");
                        LocalDate end = InputUtil.readDate("End Date");
                        Drive d = driveService.createDrive(name, centerId, vaccineId, start, end);
                        System.out.println("[SUCCESS] Drive created: " + d);
                    }
                    case 2 -> {
                        List<Drive> drives = driveService.getAllDrives();
                        if (drives.isEmpty()) System.out.println("No drives found.");
                        else drives.forEach(System.out::println);
                    }
                    case 3 -> {
                        List<Drive> drives = driveService.getUpcomingDrives();
                        if (drives.isEmpty()) System.out.println("No upcoming drives found.");
                        else drives.forEach(System.out::println);
                    }
                    case 4 -> {
                        List<Drive> drives = driveService.getCurrentActiveDrives();
                        if (drives.isEmpty()) System.out.println("No currently active drives found.");
                        else drives.forEach(System.out::println);
                    }
                    case 5 -> {
                        List<Drive> drives = driveService.getCompletedDrives();
                        if (drives.isEmpty()) System.out.println("No completed drives found.");
                        else drives.forEach(System.out::println);
                    }
                    case 6 -> {
                        List<Drive> all = driveService.getAllDrives();
                        System.out.println("\nExisting Drives:");
                        all.forEach(d -> System.out.printf("  [%d] %-30s | Center #%d | Vax #%d | %s to %s | %s%n",
                                d.getDriveId(), d.getDriveName(), d.getCenterId(), d.getVaccineId(), d.getStartDate(), d.getEndDate(), (d.isActive() ? "ACTIVE" : "INACTIVE")));
                        int id = InputUtil.readInt("Drive ID to update (or 0 to cancel)");
                        if (id == 0) break;
                        Drive existing = driveService.getDriveById(id);
                        String name = InputUtil.readOptionalString("Drive Name", existing.getDriveName());

                        System.out.println("Active Centers:");
                        centerService.getActiveCenters().forEach(c -> System.out.printf("  [%d] %s (%s)%n", c.getCenterId(), c.getCenterName(), c.getCity()));
                        int cId = InputUtil.readInt("Center ID [" + existing.getCenterId() + "]");

                        System.out.println("Active Vaccines:");
                        vaccineService.getActiveVaccines().forEach(v -> System.out.printf("  [%d] %s%n", v.getVaccineId(), v.getVaccineName()));
                        int vId = InputUtil.readInt("Vaccine ID [" + existing.getVaccineId() + "]");

                        LocalDate start = InputUtil.readDate("Start Date (current: " + existing.getStartDate() + ")");
                        LocalDate end = InputUtil.readDate("End Date (current: " + existing.getEndDate() + ")");
                        boolean active = InputUtil.readConfirmation("Is Drive Active?");
                        driveService.updateDrive(id, name, cId, vId, start, end, active);
                        System.out.println("[SUCCESS] Drive #" + id + " updated.");
                    }
                    case 7 -> {
                        List<Drive> all = driveService.getAllDrives();
                        System.out.println("\nExisting Drives:");
                        all.forEach(d -> System.out.printf("  [%d] %-30s | Status: %s%n",
                                d.getDriveId(), d.getDriveName(), (d.isActive() ? "ACTIVE" : "INACTIVE")));
                        int id = InputUtil.readInt("Drive ID to toggle status (or 0 to cancel)");
                        if (id == 0) break;
                        Drive existing = driveService.getDriveById(id);
                        boolean newStatus = !existing.isActive();
                        boolean confirm = InputUtil.readConfirmation("Set status for Drive #" + id + " to " + (newStatus ? "ACTIVE" : "INACTIVE") + "?");
                        if (!confirm) break;
                        driveService.toggleActiveStatus(id, newStatus);
                        System.out.println("[SUCCESS] Drive #" + id + " status set to: " + (newStatus ? "ACTIVE" : "INACTIVE"));
                    }
                    case 8 -> loop = false;
                }
            } catch (AppException e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
    }

    // --- Slot Management ---
    private void handleManageSlots() {
        boolean loop = true;
        while (loop) {
            System.out.println("\n--- MANAGE APPOINTMENT SLOTS ---");
            System.out.println("1. Create New Slot for Drive");
            System.out.println("2. View Slots by Drive");
            System.out.println("3. View Slots by Date");
            System.out.println("4. Update Slot Capacity");
            System.out.println("5. Return to Admin Menu");

            int choice = InputUtil.readInt("Select option", 1, 5);
            try {
                switch (choice) {
                    case 1 -> {
                        System.out.println("Active Drives:");
                        driveService.getActiveDrives().forEach(d -> System.out.printf("  [%d] %-30s | Valid Dates: %s to %s%n", 
                                d.getDriveId(), d.getDriveName(), d.getStartDate(), d.getEndDate()));
                        int driveId = InputUtil.readInt("Drive ID (or 0 to cancel)");
                        if (driveId == 0) break;
                        LocalDate date = InputUtil.readDate("Slot Date");
                        LocalTime start = InputUtil.readTime("Start Time (HH:mm)");
                        LocalTime end = InputUtil.readTime("End Time (HH:mm)");
                        int capacity = InputUtil.readInt("Slot Capacity (min 1)");
                        Slot slot = slotService.createSlot(driveId, date, start, end, capacity);
                        System.out.println("[SUCCESS] Slot created: " + slot);
                    }
                    case 2 -> {
                        List<Drive> all = driveService.getAllDrives();
                        System.out.println("Drives:");
                        all.forEach(d -> System.out.printf("  [%d] %-30s (%s)%n", d.getDriveId(), d.getDriveName(), (d.isActive() ? "ACTIVE" : "INACTIVE")));
                        int driveId = InputUtil.readInt("Drive ID (or 0 to cancel)");
                        if (driveId == 0) break;
                        List<Slot> slots = slotService.getSlotsByDrive(driveId);
                        if (slots.isEmpty()) {
                            System.out.println("No slots scheduled for Drive #" + driveId + ".");
                        } else {
                            System.out.println("\nSlots for Drive #" + driveId + ":");
                            slots.forEach(System.out::println);
                        }
                    }
                    case 3 -> {
                        LocalDate date = InputUtil.readDate("Slot Date");
                        List<Slot> slots = slotService.getSlotsByDate(date);
                        if (slots.isEmpty()) {
                            System.out.println("No slots scheduled on " + date + ".");
                        } else {
                            System.out.println("\nSlots scheduled on " + date + ":");
                            slots.forEach(System.out::println);
                        }
                    }
                    case 4 -> {
                        int driveId = InputUtil.readInt("Enter Drive ID to view slots (or 0 to enter Slot ID directly)");
                        if (driveId > 0) {
                            List<Slot> driveSlots = slotService.getSlotsByDrive(driveId);
                            if (driveSlots.isEmpty()) {
                                System.out.println("No slots found for Drive #" + driveId);
                            } else {
                                System.out.println("\nSlots for Drive #" + driveId + ":");
                                driveSlots.forEach(System.out::println);
                            }
                        }
                        int slotId = InputUtil.readInt("Slot ID to update (or 0 to cancel)");
                        if (slotId == 0) break;
                        int newCap = InputUtil.readInt("New Capacity");
                        slotService.updateSlotCapacity(slotId, newCap);
                        System.out.println("[SUCCESS] Slot #" + slotId + " capacity updated to " + newCap);
                    }
                    case 5 -> loop = false;
                }
            } catch (AppException e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
    }

    // --- Appointment Oversight ---
    private void handleViewAllAppointments() {
        System.out.println("\n--- VIEW APPOINTMENTS ---");
        System.out.println("Status Filter Options: 0. ALL  1. BOOKED  2. CANCELLED  3. COMPLETED  4. MISSED");
        int statusOpt = InputUtil.readInt("Select Status Filter (0-4)", 0, 4);
        AppointmentStatus status = switch (statusOpt) {
            case 1 -> AppointmentStatus.BOOKED;
            case 2 -> AppointmentStatus.CANCELLED;
            case 3 -> AppointmentStatus.COMPLETED;
            case 4 -> AppointmentStatus.MISSED;
            default -> null;
        };

        int centerId = InputUtil.readInt("Center ID filter (0 for all centers)");
        int driveId = InputUtil.readInt("Drive ID filter (0 for all drives)");

        try {
            List<Appointment> list = bookingService.getAllAppointments(
                    status, 
                    (centerId > 0 ? centerId : null), 
                    (driveId > 0 ? driveId : null), 
                    null
            );
            if (list.isEmpty()) {
                System.out.println("No matching appointment records found.");
                return;
            }
            System.out.printf("%nFound %d matching appointment(s):%n", list.size());
            list.forEach(System.out::println);
        } catch (AppException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void handleSearchAppointmentById() {
        long id = (long) InputUtil.readInt("Enter Appointment ID (or 0 to cancel)");
        if (id == 0) return;
        try {
            Appointment appt = bookingService.getAppointmentById(id);
            System.out.println("\n=======================================================");
            System.out.println("               APPOINTMENT RECORD DETAILS              ");
            System.out.println("=======================================================");
            System.out.println("ID        : " + appt.getAppointmentId());
            System.out.println("Citizen   : " + appt.getUserName() + " (" + appt.getUserEmail() + ", " + appt.getUserPhone() + ")");
            System.out.println("Center    : " + appt.getCenterName() + " (" + appt.getCenterCity() + ")");
            System.out.println("Address   : " + appt.getCenterAddress());
            System.out.println("Drive     : " + appt.getDriveName());
            System.out.println("Vaccine   : " + appt.getVaccineName());
            System.out.println("Schedule  : " + appt.getSlotDate() + " | " + appt.getStartTime() + " - " + appt.getEndTime());
            System.out.println("Booked At : " + appt.getBookingTime());
            System.out.println("Status    : " + appt.getStatus());
            System.out.println("=======================================================");
        } catch (AppException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void handleUpdateAppointmentStatus() {
        long id = (long) InputUtil.readInt("Enter Appointment ID to update (or 0 to cancel)");
        if (id == 0) return;
        try {
            Appointment appt = bookingService.getAppointmentById(id);
            System.out.println("\nSelected Appointment: " + appt);
            System.out.println("Current Status: " + appt.getStatus());
            System.out.println("Available Statuses: 1. COMPLETED  2. MISSED  3. CANCELLED  0. Cancel");
            int opt = InputUtil.readInt("Select new status (0-3)", 0, 3);
            if (opt == 0) {
                System.out.println("Status update aborted.");
                return;
            }
            AppointmentStatus newStatus = switch (opt) {
                case 1 -> AppointmentStatus.COMPLETED;
                case 2 -> AppointmentStatus.MISSED;
                case 3 -> AppointmentStatus.CANCELLED;
                default -> null;
            };

            boolean confirm = InputUtil.readConfirmation("Confirm status change for Appointment #" + id + " to " + newStatus + "?");
            if (!confirm) {
                System.out.println("Status update aborted.");
                return;
            }

            bookingService.updateAppointmentStatus(id, newStatus);
            System.out.println("[SUCCESS] Appointment #" + id + " status updated to " + newStatus);
        } catch (AppException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    // --- Reports Submenu ---
    private void handleReportsSubmenu() {
        boolean repLoop = true;
        while (repLoop) {
            System.out.println("\n========================================");
            System.out.println("ADMINISTRATIVE REPORTS MENU");
            System.out.println("========================================");
            System.out.println("1. Report A: Total Registered Users");
            System.out.println("2. Report B: Total Appointments by Status");
            System.out.println("3. Report C: Center-Wise Bookings");
            System.out.println("4. Report D: Drive-Wise Bookings");
            System.out.println("5. Report E: Daily Appointment Report");
            System.out.println("6. Report F: Live Slot Availability Report");
            System.out.println("7. Report G: Completed Vaccinations Summary");
            System.out.println("8. Return to Admin Menu");
            System.out.println("========================================");

            int choice = InputUtil.readInt("Select report to run", 1, 8);
            try {
                switch (choice) {
                    case 1 -> reportService.printUserReport();
                    case 2 -> reportService.printAppointmentStatusReport();
                    case 3 -> reportService.printCenterReport();
                    case 4 -> reportService.printDriveReport();
                    case 5 -> reportService.printDailyReport();
                    case 6 -> reportService.printSlotAvailabilityReport();
                    case 7 -> reportService.printCompletedImmunizationsReport();
                    case 8 -> repLoop = false;
                }
            } catch (AppException e) {
                System.err.println("[ERROR] Report generation failed: " + e.getMessage());
            }
        }
    }
}
