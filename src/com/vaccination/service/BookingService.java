package com.vaccination.service;

import com.vaccination.dao.AppointmentDAO;
import com.vaccination.dao.DriveDAO;
import com.vaccination.dao.SlotDAO;
import com.vaccination.exception.AppException;
import com.vaccination.exception.BookingException;
import com.vaccination.exception.DatabaseException;
import com.vaccination.model.Appointment;
import com.vaccination.model.AppointmentStatus;
import com.vaccination.model.Drive;
import com.vaccination.model.Slot;
import com.vaccination.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transactional orchestrator managing the appointment booking workflow,
 * pessimistic row locking (SELECT ... FOR UPDATE), duplicate active booking prevention,
 * and cancellation eligibility rules.
 */
public class BookingService {

    private final AppointmentDAO appointmentDAO;
    private final SlotDAO slotDAO;
    private final DriveDAO driveDAO;

    public BookingService() {
        this.appointmentDAO = new AppointmentDAO();
        this.slotDAO = new SlotDAO();
        this.driveDAO = new DriveDAO();
    }

    public BookingService(AppointmentDAO appointmentDAO, SlotDAO slotDAO, DriveDAO driveDAO) {
        this.appointmentDAO = appointmentDAO;
        this.slotDAO = slotDAO;
        this.driveDAO = driveDAO;
    }

    /**
     * Books an appointment within a strict ACID JDBC transaction.
     * 
     * Concurrency Safety Mechanism:
     * 1. Opens a dedicated database connection and disables auto-commit.
     * 2. Acquires an exclusive write lock (SELECT ... FOR UPDATE) on the target slot row.
     * 3. Validates that the slot exists, is in the future, and its parent drive is active.
     * 4. Recalculates current active reservations while holding the lock.
     * 5. Confirms that (activeCount < slot.capacity).
     * 6. Confirms that the user has no duplicate active reservation for this slot.
     * 7. Inserts the appointment record into the appointments table.
     * 8. Commits the transaction and releases the lock.
     * 9. On any error or rule violation, rolls back the transaction.
     */
    public Appointment bookAppointment(int userId, int slotId) throws AppException {
        Connection conn = null;
        long generatedAppointmentId = 0;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start explicit JDBC transaction

            // Step 1: Acquire exclusive row lock on the slot
            Optional<Slot> slotOpt = slotDAO.findByIdForUpdate(conn, slotId);
            if (slotOpt.isEmpty()) {
                conn.rollback();
                throw new BookingException("Appointment Slot #" + slotId + " does not exist.");
            }
            Slot slot = slotOpt.get();

            // Step 2: Ensure slot start time is in the future
            LocalDateTime slotStart = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
            if (slotStart.isBefore(LocalDateTime.now())) {
                conn.rollback();
                throw new BookingException("Cannot book an appointment for a slot that has already started or ended.");
            }

            // Step 3: Validate parent Drive is active
            Optional<Drive> driveOpt = driveDAO.findById(slot.getDriveId());
            if (driveOpt.isEmpty() || !driveOpt.get().isActive()) {
                conn.rollback();
                throw new BookingException("The vaccination drive for this slot is currently inactive.");
            }

            // Step 4: Recalculate active bookings under the lock
            int activeBookings = appointmentDAO.countActiveBookingsTransactional(conn, slotId);
            if (activeBookings >= slot.getCapacity()) {
                conn.rollback();
                throw new BookingException(String.format(
                        "Slot is fully booked (%d/%d capacity reached). Please select another time slot.",
                        activeBookings, slot.getCapacity()));
            }

            // Step 5: Duplicate active booking check under the lock
            boolean alreadyBooked = appointmentDAO.hasActiveBookingForSlotTransactional(conn, slotId, userId);
            if (alreadyBooked) {
                conn.rollback();
                throw new BookingException("You already hold an active reservation for this slot. " +
                        "Multiple active bookings for the same slot are not permitted.");
            }

            // Step 6: Insert appointment
            Appointment newAppt = new Appointment(userId, slotId);
            appointmentDAO.insertTransactional(conn, newAppt);
            generatedAppointmentId = newAppt.getAppointmentId();

            // Step 7: Commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("CRITICAL: Failed to rollback transaction: " + rollbackEx.getMessage());
                }
            }
            if (e.getErrorCode() == 1062) {
                throw new BookingException("A duplicate active booking was rejected by the database constraint.", e);
            }
            throw new DatabaseException("Transaction failed during booking: " + e.getMessage(), e);

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("WARN: Failed to close connection: " + closeEx.getMessage());
                }
            }
        }

        // Return fully hydrated appointment with joined presentation names
        return appointmentDAO.findById(generatedAppointmentId)
                .orElseThrow(() -> new BookingException("Failed to retrieve confirmed appointment details."));
    }

    /**
     * Cancels an eligible citizen appointment.
     * Releases slot capacity immediately without deleting the record.
     */
    public void cancelAppointment(long appointmentId, int requestingUserId, boolean isAdmin) throws AppException {
        Appointment appt = appointmentDAO.findById(appointmentId)
                .orElseThrow(() -> new BookingException("Appointment #" + appointmentId + " does not exist."));

        // Ownership verification (users can only cancel their own appointments)
        if (!isAdmin && appt.getUserId() != requestingUserId) {
            throw new BookingException("Access denied: You can only cancel your own appointments.");
        }

        // Status check
        if (appt.getStatus() != AppointmentStatus.BOOKED) {
            throw new BookingException("Cannot cancel appointment. Current status is already " + appt.getStatus() + ".");
        }

        // Start time verification (users cannot cancel past or ongoing appointments)
        LocalDateTime slotStart = LocalDateTime.of(appt.getSlotDate(), appt.getStartTime());
        if (!isAdmin && slotStart.isBefore(LocalDateTime.now())) {
            throw new BookingException("Cannot cancel an appointment that has already started or passed.");
        }

        appointmentDAO.updateStatus(appointmentId, AppointmentStatus.CANCELLED);
    }

    public List<Appointment> getUpcomingAppointments(int userId) throws AppException {
        return appointmentDAO.findUpcomingByUserId(userId);
    }

    public List<Appointment> getAppointmentHistory(int userId) throws AppException {
        return appointmentDAO.findByUserId(userId);
    }

    public List<Appointment> getAllAppointments(AppointmentStatus status, Integer centerId, Integer driveId, LocalDate date) throws AppException {
        return appointmentDAO.findAllFiltered(status, centerId, driveId, date);
    }

    public Appointment getAppointmentById(long appointmentId) throws AppException {
        return appointmentDAO.findById(appointmentId)
                .orElseThrow(() -> new BookingException("Appointment #" + appointmentId + " does not exist."));
    }

    /**
     * Administrative action to mark appointments COMPLETED or MISSED.
     */
    public void updateAppointmentStatus(long appointmentId, AppointmentStatus newStatus) throws AppException {
        getAppointmentById(appointmentId); // verify exists
        appointmentDAO.updateStatus(appointmentId, newStatus);
    }
}
