# Vaccination Drive Scheduler (Version B — Core Java + JDBC + MySQL)

An enterprise-grade, console-based appointment scheduling and inventory coordination system built using **pure Core Java (JDK 21+)**, raw **JDBC (DriverManager, PreparedStatements, explicit transaction management)**, and **MySQL 8.x (InnoDB)**. 

Strictly developed **without Maven, Gradle, Spring Boot, Hibernate, JPA, or GUI frameworks**.

---

## 1. Project Overview & Problem Statement

Public health vaccination initiatives require coordinated scheduling across multiple physical medical centers and vaccine formulations while strictly preventing overbooking. When multiple citizens attempt to reserve the final remaining place in a time slot simultaneously, naive software architectures suffer from **Time-of-Check to Time-of-Use (TOCTOU)** race conditions, resulting in overbooking and compromised public trust.

The **Vaccination Drive Scheduler** solves this problem by enforcing:
1. **Pessimistic Row-Level Locking (`SELECT ... FOR UPDATE`)** inside explicit JDBC ACID transactions.
2. **Database-Level Conditional Uniqueness** using MySQL Virtual Generated Columns to forbid duplicate active bookings while freely supporting re-booking after cancellation.
3. **Zero-Dependency Cryptographic Security** utilizing JDK's built-in `PBKDF2WithHmacSHA256` with 16-byte random salts and constant-time verification.
4. **Historical Referential Integrity** with `ON DELETE RESTRICT` foreign keys and soft deactivation.

---

## 2. Technology Stack & Design Boundary

* **Programming Language:** Core Java (JDK 21+)
* **Database Management System:** MySQL Server 8.x
* **Storage Engine:** InnoDB (mandatory for ACID transaction rollback and row-level locking)
* **Connectivity:** JDBC via official MySQL Connector/J driver (`mysql-connector-j-9.2.0.jar`)
* **Cryptography:** Java Cryptography Architecture (`javax.crypto.SecretKeyFactory` with PBKDF2, `java.security.SecureRandom`)
* **Build / Dependency Management:** Manual Classpath Management (No Maven, No Gradle)
* **User Interface:** Interactive Command-Line Console Interface

---

## 3. Directory Layout

```
VaccinationDriveScheduler/
│
├── lib/
│   └── mysql-connector-j-9.2.0.jar      # Official MySQL JDBC Driver
│
├── config/
│   ├── db.properties.example            # Configuration template
│   └── db.properties                    # Local database credentials
│
├── sql/
│   ├── schema.sql                       # 6 InnoDB tables, foreign keys, constraints, indexes
│   ├── sample_data.sql                  # Seed data with real PBKDF2 hashes
│   └── reports.sql                      # 7 analytical administrative SQL queries
│
├── src/
│   └── com/vaccination/
│       ├── main/
│       │   ├── Main.java                # Interactive console CLI menu loop
│       │   ├── ConnectionTest.java      # JDBC diagnostic connection verification tool
│       │   ├── Phase4Test.java          # Automated unit test suite for models and utils
│       │   └── ConcurrencyBookingTest.java # Multi-threaded race-condition stress tester
│       │
│       ├── model/                       # Domain entities & POJOs
│       │   ├── User.java, Center.java, Vaccine.java, Drive.java, Slot.java, Appointment.java
│       │   ├── Session.java             # In-memory authenticated principal
│       │   └── Role.java, AppointmentStatus.java # Type-safe enums
│       │
│       ├── dao/                         # Data Access Objects (Raw PreparedStatement)
│       │   ├── UserDAO.java, CenterDAO.java, VaccineDAO.java
│       │   ├── DriveDAO.java, SlotDAO.java, AppointmentDAO.java
│       │
│       ├── service/                     # Business Logic Layer & Transaction Orchestration
│       │   ├── AuthService.java, CenterService.java, VaccineService.java
│       │   ├── DriveService.java, SlotService.java, BookingService.java, ReportService.java
│       │
│       ├── util/                        # Reusable Infrastructure Helpers
│       │   ├── DBConnection.java        # JDBC connection factory with error diagnostics
│       │   ├── PasswordUtil.java        # PBKDF2 cryptographic salt & hashing
│       │   ├── InputUtil.java           # Safe Scanner reader (immune to newline bugs)
│       │   └── ValidationUtil.java      # Regex validation for email, phone, ranges
│       │
│       └── exception/                   # Custom Checked Exception Hierarchy
│           ├── AppException.java, ValidationException.java, AuthenticationException.java
│           ├── BookingException.java, DatabaseException.java
│
├── bin/                                 # Compiled Java bytecode (.class files)
├── README.md                            # Comprehensive manual and interview guide
├── run.bat                              # Windows Command Prompt build and execution script
└── run.ps1                              # Windows PowerShell build and execution script
```

---

## 4. Database Schema & Entity Relationships

The relational architecture consists of 6 tables governed by InnoDB foreign keys and referential checks:

```
[users] 1 ────────────< [appointments] >──────────── 1 [slots]
                              │                           │
                              │                           v 1
[centers] 1 ──────────< [drives] >────────── 1 [vaccines]
```

### Key Relational Rules:
1. **`ON DELETE RESTRICT`:** Deleting a center, drive, or user referenced by appointments is strictly rejected. Historical records can never be orphaned or cascade-deleted.
2. **Virtual Generated Column:**
   ```sql
   active_booking_key VARCHAR(64) GENERATED ALWAYS AS (
       CASE WHEN status = 'BOOKED' THEN CONCAT(slot_id, ':', user_id) ELSE NULL END
   ) VIRTUAL,
   CONSTRAINT uq_active_slot_user UNIQUE (active_booking_key)
   ```
   Ensures at the database engine level that a user cannot have two active reservations for the same slot, while allowing unlimited re-bookings after cancellation (because `NULL` entries are permitted in `UNIQUE` indexes).

---

## 5. Setup & Execution Guide

### Prerequisites
* **Java Development Kit (JDK 21+):** Verify with `javac -version` and `java -version`.
* **MySQL Server 8.x:** Verify the MySQL service is running (`Get-Service MySQL80`).

### Step 1: Database Initialization
Open PowerShell or Command Prompt, navigate to the project root, and execute the SQL scripts using your MySQL root user:
```powershell
cd "C:\Users\hp\Desktop\JAVA Project\VaccinationDriveScheduler"
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/sample_data.sql
```

### Step 2: Configure Database Credentials
Open `config/db.properties` and enter your MySQL root password:
```properties
db.url=jdbc:mysql://localhost:3306/vaccination_scheduler?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_ACTUAL_MYSQL_PASSWORD
```

### Step 3: Verify Connection & Seed Data
Run the diagnostic connection tester:
```powershell
.\run.ps1 -TargetClass "com.vaccination.main.ConnectionTest"
```
*(Or in Command Prompt: `run.bat com.vaccination.main.ConnectionTest`)*

### Step 4: Run the Application
Launch the interactive console application:
```powershell
.\run.ps1
```
*(Or in Command Prompt: `run.bat`)*

### Default Seed Accounts (Included in `sample_data.sql`)
* **Administrator:** `admin@vaccination.org` | Password: `Admin@1234`
* **Citizen 1:** `john.doe@example.com` | Password: `User@1234`
* **Citizen 2:** `alice.smith@example.com` | Password: `User@1234`
* **Citizen 3:** `bob.johnson@example.com` | Password: `User@1234`

---

## 6. Concurrency Control & ACID Transaction Architecture

### The Race Condition (TOCTOU)
Suppose Slot #3 has a capacity of 1. User A and User B attempt to book at the exact same millisecond:
1. Thread A queries: `SELECT COUNT(*) FROM appointments WHERE slot_id = 3 AND status = 'BOOKED'` $\rightarrow$ returns 0.
2. Thread B queries: `SELECT COUNT(*) FROM appointments WHERE slot_id = 3 AND status = 'BOOKED'` $\rightarrow$ returns 0.
3. Thread A executes `INSERT` $\rightarrow$ count is now 1.
4. Thread B executes `INSERT` $\rightarrow$ count is now 2. **Overbooked!**

### The Solution: Pessimistic Row Locking (`SELECT ... FOR UPDATE`)
In `BookingService.java`, the entire booking operation runs in an atomic JDBC transaction:
```java
Connection conn = DBConnection.getConnection();
conn.setAutoCommit(false); // Begin explicit transaction

// 1. Lock the slot row exclusively
Optional<Slot> slot = slotDAO.findByIdForUpdate(conn, slotId);
// -> Executes: SELECT * FROM slots WHERE slot_id = ? FOR UPDATE;

// 2. Thread B blocks at the database engine until Thread A commits or rolls back

// 3. Recalculate capacity under the lock
int active = appointmentDAO.countActiveBookingsTransactional(conn, slotId);
if (active >= slot.get().getCapacity()) {
    conn.rollback();
    throw new BookingException("Slot is fully booked.");
}

// 4. Validate duplicate active booking for the user under the lock
if (appointmentDAO.hasActiveBookingForSlotTransactional(conn, slotId, userId)) {
    conn.rollback();
    throw new BookingException("You already hold an active reservation for this slot.");
}

// 5. Insert appointment
appointmentDAO.insertTransactional(conn, new Appointment(userId, slotId));

// 6. Commit transaction and release lock
conn.commit();
```

### Running the Concurrency Test Suite
Execute the multi-threaded concurrency tester which uses `CountDownLatch` to simulate two simultaneous users competing for the last available slot:
```powershell
.\run.ps1 -TargetClass "com.vaccination.main.ConcurrencyBookingTest"
```
Expected Output:
```
[TEST A] Simultaneous booking competition for a slot with Capacity = 1...
Execution Outcomes:
  -> User A (2) SUCCEEDED
  -> User B (3) REJECTED: Slot is fully booked (1/1 capacity reached).
[RESULT] Successes: 1 | Rejections: 1 | Final DB Bookings: 1 (Slot Capacity: 1)
>>> [PASS] CONCURRENCY INTEGRITY VERIFIED: Overbooking was strictly prevented! <<<
```

---

## 7. Technical Interview Preparation (Q&A)

### Q1: Why use a layered architecture instead of writing SQL in `Main.java`?
**Answer:** Layered architecture enforces the **Separation of Concerns (SoC)** principle. `Main.java` handles UI presentation and input parsing; `Service` classes contain business invariants (e.g., date ranges, capacity checks, transaction boundaries); and `DAO` classes isolate SQL persistence. If we transition the presentation layer from a console CLI to a Spring REST API or JavaFX UI, zero lines of service or DAO code need to be rewritten.

### Q2: Why avoid storing plaintext passwords or fast hashes like MD5/SHA-256?
**Answer:** Fast hashes like SHA-256 can compute billions of guesses per second on modern GPUs. We use **PBKDF2WithHmacSHA256** with **65,536 iterations** and a unique cryptographically random **16-byte salt** per user (`SecureRandom`). The iteration work factor intentionally slows down password cracking, rendering pre-computed rainbow tables and brute-force attacks computationally infeasible.

### Q3: Why not use Java `synchronized` methods instead of database row locks?
**Answer:** A Java `synchronized` block is purely local to a single JVM process. In real-world enterprise deployments where multiple application instances or microservices connect to a single MySQL database cluster, a Java lock in Instance A cannot protect against a concurrent request hitting Instance B. Database-level locking (`SELECT ... FOR UPDATE`) is the only distributed, ACID-durable solution.

### Q4: How does the system handle connection leaks?
**Answer:** Every database operation uses Java's **try-with-resources** statement (`try (Connection conn = ...)`). Even if an unexpected runtime exception occurs, the JVM guarantees that `conn.close()` is invoked, returning the socket to the operating system and preventing socket pool exhaustion.

---

## 8. Future Web Interface Visual Identity Specification

When this application is expanded into a modern web version, the UI must follow this design specification:
* **Primary Palette:** Warm Ivory (`#F9EBD7`) for surfaces/backgrounds, Emerald Green (`#046A38`) for primary actions and active navigation accents, Vermilion Red (`#F34222`) for consequential actions (cancellations/warnings).
* **Supporting Tones:** Dark Forest Green (`#123D2B`), Warm Charcoal (`#292923`) for high-contrast typography, and Soft Neutral Borders (`#D8CEBC`).
* **Design Guidelines:** Restrained 4–8px corner radii, editorial typography (DM Serif Display / Inter), accessible form controls, zero purple/neon gradients, zero fake marketing reviews, and zero synthetic AI stock photography. All business rules must remain enforced by the Core Java backend.
