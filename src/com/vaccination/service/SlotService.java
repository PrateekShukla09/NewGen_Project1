package com.vaccination.service;

import com.vaccination.dao.DriveDAO;
import com.vaccination.dao.SlotDAO;
import com.vaccination.exception.AppException;
import com.vaccination.exception.ValidationException;
import com.vaccination.model.Drive;
import com.vaccination.model.Slot;
import com.vaccination.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service managing appointment slots, capacity constraints,
 * date-window invariants, and center-level overlap prevention.
 */
public class SlotService {

    private final SlotDAO slotDAO;
    private final DriveDAO driveDAO;

    public SlotService() {
        this.slotDAO = new SlotDAO();
        this.driveDAO = new DriveDAO();
    }

    public SlotService(SlotDAO slotDAO, DriveDAO driveDAO) {
        this.slotDAO = slotDAO;
        this.driveDAO = driveDAO;
    }

    public Slot createSlot(int driveId, LocalDate slotDate, LocalTime startTime, 
                           LocalTime endTime, int capacity) throws AppException {
        // 1. Basic Bounds Validation
        ValidationUtil.validateCapacity(capacity);
        ValidationUtil.validateTimeRange(startTime, endTime);

        if (slotDate == null) {
            throw new ValidationException("Slot date cannot be null.");
        }

        // 2. Prevent creating past slots
        if (slotDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Cannot schedule a slot in the past (" + slotDate + ").");
        }
        if (slotDate.isEqual(LocalDate.now()) && startTime.isBefore(LocalTime.now())) {
            throw new ValidationException("Cannot schedule a slot that starts in the past today (" + startTime + ").");
        }

        // 3. Verify Drive Invariants
        Drive drive = driveDAO.findById(driveId)
                .orElseThrow(() -> new ValidationException("Vaccination Drive #" + driveId + " does not exist."));

        if (!drive.isActive()) {
            throw new ValidationException("Cannot create slots for inactive drive: '" + drive.getDriveName() + "'.");
        }

        // 4. Enforce Drive Date Bounds: slot_date must fall within [drive.startDate, drive.endDate]
        if (slotDate.isBefore(drive.getStartDate()) || slotDate.isAfter(drive.getEndDate())) {
            throw new ValidationException(String.format(
                    "Slot date (%s) falls outside drive date range [%s to %s].",
                    slotDate, drive.getStartDate(), drive.getEndDate()));
        }

        // 5. Overlap Prevention at the Center
        boolean overlaps = slotDAO.hasOverlappingSlotAtCenter(drive.getCenterId(), slotDate, startTime, endTime);
        if (overlaps) {
            throw new ValidationException(String.format(
                    "A conflicting slot already exists at this center on %s overlapping the interval %s - %s.",
                    slotDate, startTime, endTime));
        }

        Slot slot = new Slot(driveId, slotDate, startTime, endTime, capacity);
        return slotDAO.insert(slot);
    }

    public List<Slot> getSlotsByDrive(int driveId) throws AppException {
        return slotDAO.findByDriveId(driveId);
    }

    public List<Slot> getAvailableSlotsByDrive(int driveId) throws AppException {
        return slotDAO.findAvailableSlotsByDrive(driveId);
    }

    public List<Slot> getSlotsByDate(LocalDate date) throws AppException {
        if (date == null) throw new ValidationException("Date cannot be null.");
        return slotDAO.findByDate(date);
    }

    public Slot getSlotById(int slotId) throws AppException {
        return slotDAO.findById(slotId)
                .orElseThrow(() -> new ValidationException("Appointment Slot #" + slotId + " does not exist."));
    }

    /**
     * Safely updates slot capacity.
     * Prevents reducing capacity below the number of already confirmed active bookings!
     */
    public void updateSlotCapacity(int slotId, int newCapacity) throws AppException {
        ValidationUtil.validateCapacity(newCapacity);
        Slot slot = getSlotById(slotId);

        int activeBookings = slotDAO.getActiveBookingCount(slotId);
        if (newCapacity < activeBookings) {
            throw new ValidationException(String.format(
                    "Cannot reduce capacity to %d. There are currently %d active citizen reservations for this slot.",
                    newCapacity, activeBookings));
        }

        slotDAO.updateCapacity(slotId, newCapacity);
    }
}
