# ⚡ VITA-SYNC + VITAL-CONNECT — Integrated Smart Hospital System

A real-time patient vital sign monitoring and emergency management system built entirely in **Java 17** with a **JavaFX** dashboard. Evolved from a 3rd-semester C-based hospital management system (VITAL-CONNECT) into a full-stack Java application with login, analytics, and emergency dispatch.

---

## What it does

- **Doctor Login** — secure SHA-256 authenticated login before accessing the dashboard
- **Real-time Monitoring** — HR and SpO2 for multiple patients simultaneously (every 1.5 sec)
- **Search & Filter** — search patients by ID or name, filter by CRITICAL / WARNING / STABLE
- **Critical Vital Detection** — auto-detects bradycardia, tachycardia, hypoxemia
- **Auto Doctor Assignment** — when a patient goes CRITICAL, an available doctor is auto-assigned
- **Emergency Triage** — ranks patients by health criticality using NEWS2-based stability scoring
- **Emergency Request Management** — raise ER requests manually or automatically; track status PENDING → DISPATCHED → RESOLVED; stored in MySQL
- **Discharge Summary** — on patient discharge, generates a full summary (avg HR, SpO2, critical events, doctor, duration) in a popup window
- **Analytics Dashboard** — live hospital-wide stats: total patients, critical/stable/warning counts, free beds, ER pending, low stock alerts, top 5 critical patients
- **Bed Management** — real-time bed allocation and release
- **Doctor Management** — add doctors, view availability, assign to patients (Hash Table)
- **Inventory Management** — medicine/supply stock with add, restock, dispense; low stock warnings (BST / TreeMap)
- **Patient Admission/Discharge** — full lifecycle management with doctor assignment
- **MySQL Persistence** — all data stored in MySQL with in-memory caching for speed
- **Alert History** — timestamped log of all critical vital events

---

## Evolution from VITAL-CONNECT (3rd Sem C Project)

| VITAL-CONNECT (C)             | VITA-SYNC (Java — Current)                       |
|-------------------------------|--------------------------------------------------|
| Priority Queue (Linked List)  | `PriorityBlockingQueue<PatientPriority>`         |
| Hash Table for Doctors        | `ConcurrentHashMap<username, Doctor>`            |
| BST for Inventory             | `TreeMap<itemId, InventoryItem>`                 |
| Room Management (Array)       | `BedManager` with `ConcurrentHashMap`            |
| File Handling (`.dat`)        | MySQL 8 + HikariCP connection pool               |
| Console UI                    | JavaFX dark-themed dashboard (9 tabs)            |
| No authentication             | SHA-256 Doctor Login Screen                      |
| No emergency tracking         | Full ER Request lifecycle (DB-backed)            |
| No analytics                  | Live Analytics Dashboard with stat cards         |

---

## Project Structure

```
src/main/java/com/vitasync/
├── Main.java                            # Entry point — wires all components
├── model/
│   ├── PatientRecord.java               # Patient data + vital history
│   ├── VitalSignReading.java            # Immutable single reading snapshot
│   ├── PatientPriority.java             # Comparable priority entry
│   ├── Bed.java                         # Hospital bed (FREE / OCCUPIED)
│   ├── Doctor.java                      # Doctor with login credentials
│   ├── InventoryItem.java               # Medicine/supply item
│   └── EmergencyRequest.java            # ER request (PENDING/DISPATCHED/RESOLVED)
├── simulator/
│   ├── VitalsSimulator.java             # ScheduledExecutorService per patient
│   ├── VitalSignGenerator.java          # Generates HR/SpO2 readings (Runnable)
│   ├── VitalSignListener.java           # Callback interface (sensor-agnostic)
│   └── VitalSignValidator.java          # Range + critical threshold checks
├── triage/
│   ├── TriageEngine.java                # Priority queue + stability score formula
│   └── CriticalVitalHandler.java        # Alert logging + triage escalation
├── records/
│   └── RecordManager.java               # HashMap (O(1)) + MySQL (JDBC/HikariCP)
├── management/
│   ├── BedManager.java                  # Bed allocation / release
│   ├── PatientManager.java              # Admit / discharge coordinator
│   ├── DoctorManager.java               # Hash Table — doctor CRUD + auth
│   ├── InventoryManager.java            # TreeMap (BST) — medicine stock
│   ├── EmergencyRequestManager.java     # ER request lifecycle + DB persistence
│   └── DischargeSummaryService.java     # Generates patient discharge summary
├── exceptions/
│   ├── CriticalVitalException.java
│   ├── DatabaseSyncException.java
│   └── ResourceAllocationException.java
└── ui/
    ├── VitaSyncApp.java                 # JavaFX Application — shows Login first
    ├── LoginScreen.java                 # Doctor authentication screen
    ├── DashboardController.java         # 9-tab dashboard
    ├── VitalsChartWindow.java           # Live HR/SpO2 line chart popup
    └── AlertStore.java                  # Thread-safe ObservableList for alerts

src/main/resources/
├── db.properties                        # MySQL connection config
├── schema.sql                           # Full database schema
└── logback.xml                          # Logging → logs/vitasync.log
```

---

## Dashboard Tabs

| Tab | Description |
|-----|-------------|
| 📋 Live Monitor | Real-time HR, SpO2, status, bed per patient. Search/filter bar. Graph + ER Request buttons |
| 🚨 Emergency Triage | Patients ranked by stability score (most critical first) with NEWS2 risk |
| 🛏 Bed Manager | All 20 beds with FREE/OCCUPIED status |
| 👤 Admissions | Admit new patients with optional doctor assignment; discharge with summary popup |
| 🩺 Doctors | View all doctors, availability, assigned patient; add new doctors |
| 💊 Inventory | Medicine/supply stock sorted by ID; add, restock, dispense; low stock highlighted |
| 🚑 ER Requests | All emergency requests with status tracking; dispatch and resolve actions |
| 📊 Analytics | Live stat cards, top 5 critical patients, low stock alerts |
| ⚠ Alert History | Timestamped log of all critical vital events |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| UI | JavaFX 21 |
| Database | MySQL 8 + HikariCP 5 |
| DB Access | JDBC (PreparedStatement — SQL injection safe) |
| Concurrency | ScheduledExecutorService, ConcurrentHashMap, PriorityBlockingQueue |
| Data Structures | TreeMap (BST), ConcurrentHashMap (Hash Table), PriorityBlockingQueue, FilteredList |
| Auth | SHA-256 password hashing |
| Logging | SLF4J + Logback |
| Build | Maven |
| Unit Testing | JUnit 5, Mockito 5, jqwik (property-based) |
| Integration Testing | H2 in-memory database |

---

## Health Stability Score Formula

```
normalizedHR   = 1 - |HR - 75| / 75     (75 bpm is optimal)
normalizedSpO2 = SpO2 / 100
score          = (normalizedHR + normalizedSpO2) / 2
```

Score range: `0.0 – 1.0` — lower = more critical

---

## Critical Thresholds (NEWS2-based)

| Vital | Condition | Label |
|-------|-----------|-------|
| Heart Rate < 40 bpm | Bradycardia | CRITICAL |
| Heart Rate > 140 bpm | Tachycardia | CRITICAL |
| SpO2 < 90% | Hypoxemia | CRITICAL |

---

## Database Schema

| Table | Purpose |
|-------|---------|
| `patients` | Core patient data with current vitals |
| `vital_sign_history` | Every reading stored for analytics/graphs |
| `beds` | Bed allocation tracking |
| `emergency_requests` | ER request lifecycle — PENDING/DISPATCHED/RESOLVED |
| `doctors` | Doctor credentials and availability |
| `inventory` | Medicine and supply stock |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 (optional — falls back to in-memory mode)

---

## Setup & Run

**1. Clone and enter project**
```bash
git clone <your-repo-url>
cd VITA-SYNC
```

**2. Run the database schema (PowerShell)**
```powershell
Get-Content src/main/resources/schema.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"yourpassword"
```

**3. Configure DB credentials**

Edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/hospital_db
db.username=root
db.password=yourpassword
```

**4. Run the application**
```bash
mvn javafx:run
```

**5. Login with default credentials**

| Username | Password | Specialization |
|----------|----------|----------------|
| pramod | pramod123 | Emergency Medicine |
| aman | aman123 | Cardiology |
| mohit | mohit123 | Pulmonology |
| ananya | ananya123 | Neurology |
| diwakar | diwakar123 | General Surgery |

---

## Key Workflows

**Auto Critical Response:**
When a patient's vitals go CRITICAL → system simultaneously:
1. Logs alert to Alert History tab
2. Auto-assigns an available doctor
3. Creates an ER request in the database (status: PENDING)
4. Updates triage priority queue

**Manual ER Request:**
Click "🚑 Request ER" on any WARNING/CRITICAL patient in Live Monitor → creates a DB-backed request visible in the ER Requests tab.

**Discharge Flow:**
Enter patient ID in Admissions tab → click Discharge → system generates a full summary popup showing avg HR, SpO2, critical event count, assigned doctor, and stay duration — all computed from the `vital_sign_history` table.

---

## Logs

Application logs are written to `logs/vitasync.log` via Logback.

---

## Authors

VITA-SYNC — Evolved from VITAL-CONNECT (3rd Sem C Project) → Full Java Hospital System
