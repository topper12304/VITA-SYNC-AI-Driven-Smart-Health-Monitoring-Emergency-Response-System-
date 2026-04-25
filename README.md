# ⚡ VITA-SYNC — Smart Health Monitoring System

A real-time patient vital sign monitoring and triage system built in Java 17 with a JavaFX dashboard.

---

## What it does

- Monitors Heart Rate (HR) and SpO2 for multiple patients simultaneously
- Automatically detects critical conditions (bradycardia, tachycardia, hypoxemia)
- Ranks patients by health criticality using a stability scoring algorithm
- Manages hospital bed allocation
- Supports admitting and discharging patients at runtime
- Persists all data to MySQL with in-memory caching for speed
- Displays everything live in a dark-themed JavaFX dashboard

---

## Project Structure

```
src/main/java/com/vitasync/
├── Main.java                        # Entry point — launches JavaFX app
├── model/
│   ├── PatientRecord.java           # Patient data + vital history
│   ├── VitalSignReading.java        # Immutable single reading snapshot
│   ├── PatientPriority.java         # Comparable priority entry
│   └── Bed.java                     # Hospital bed (FREE / OCCUPIED)
├── simulator/
│   ├── VitalsSimulator.java         # ScheduledExecutorService per patient
│   ├── VitalSignGenerator.java      # Generates HR/SpO2 readings (Runnable)
│   ├── VitalSignListener.java       # Callback interface (sensor-agnostic)
│   └── VitalSignValidator.java      # Range + critical threshold checks
├── triage/
│   ├── TriageEngine.java            # Priority queue + stability score formula
│   └── CriticalVitalHandler.java   # Alert logging + triage escalation
├── records/
│   └── RecordManager.java          # HashMap (O(1)) + MySQL (JDBC/HikariCP)
├── management/
│   ├── BedManager.java             # Bed allocation / release
│   └── PatientManager.java         # Admit / discharge coordinator
├── exceptions/
│   ├── CriticalVitalException.java
│   ├── DatabaseSyncException.java
│   └── ResourceAllocationException.java
└── ui/
    ├── VitaSyncApp.java            # JavaFX Application class
    ├── DashboardController.java    # Full dashboard with 5 tabs
    └── AlertStore.java             # Thread-safe ObservableList for alerts

src/main/resources/
├── db.properties                   # MySQL connection config
├── schema.sql                      # Database schema
└── logback.xml                     # Logging config → logs/vitasync.log

src/test/java/com/vitasync/
├── triage/TriageEngineTest.java
├── triage/CriticalVitalHandlerTest.java
├── triage/TriageEngineProperties.java   # jqwik property-based tests
├── simulator/VitalSignValidatorTest.java
├── simulator/VitalSignGeneratorTest.java
├── simulator/VitalsSimulatorTest.java
├── model/PatientRecordTest.java
├── model/PatientPriorityTest.java
└── records/RecordManagerIntegrationTest.java  # H2 in-memory DB
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI | JavaFX 21 |
| Database | MySQL 8 + HikariCP 5 (connection pool) |
| DB Access | JDBC (PreparedStatement — SQL injection safe) |
| Concurrency | ScheduledExecutorService, ConcurrentHashMap, PriorityBlockingQueue |
| Logging | SLF4J + Logback |
| Build | Maven |
| Unit Testing | JUnit 5 |
| Mocking | Mockito 5 |
| Property Testing | jqwik 1.8.1 |
| Integration Testing | H2 in-memory database |

---

## Health Stability Score Formula

```
normalizedHR   = 1 - |HR - 75| / 75     (75 bpm is optimal)
normalizedSpO2 = SpO2 / 100
score          = (normalizedHR + normalizedSpO2) / 2
```

Score range: `0.0 – 1.0`
- `< 0.4`  → HIGH criticality (red)
- `0.4 – 0.65` → MEDIUM (yellow)
- `> 0.65` → LOW (green)

---

## Critical Thresholds

| Vital | Condition | Label |
|---|---|---|
| Heart Rate < 40 bpm | Bradycardia | CRITICAL |
| Heart Rate > 140 bpm | Tachycardia | CRITICAL |
| SpO2 < 90% | Hypoxemia | CRITICAL |

---

## Dashboard Tabs

1. **Patient Vitals** — live HR, SpO2, status, assigned bed per patient
2. **Triage Queue** — patients ranked by stability score (most critical first)
3. **Bed Management** — all 20 beds with FREE/OCCUPIED status
4. **Patient Management** — admit new patients, discharge existing ones
5. **Critical Alerts** — timestamped log of all critical vital events

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 (optional — falls back to in-memory mode if unavailable)

---

## Setup

**1. Clone the repo**
```bash
git clone <your-repo-url>
cd VITA-SYNC
```

**2. Configure database (optional)**

Edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/vitasync
db.username=root
db.password=yourpassword
```

Run the schema:
```bash
mysql -u root -p < src/main/resources/schema.sql
```

**3. Run the application**
```bash
mvn javafx:run
```

**4. Run tests**
```bash
mvn test
```

---

## Future Hardware Integration

Currently vitals are simulated via `java.util.Random`. For real deployment:

- **Sensor**: MAX30102 (HR + SpO2) connected via I2C to a Raspberry Pi
- **Bridge**: Raspberry Pi runs a Python Flask server exposing `/vitals/{patientId}`
- **Integration point**: Replace `VitalSignGenerator` with an HTTP client — `VitalSignListener` interface stays unchanged, rest of the system requires zero modification

---

## Logs

Application logs are written to `logs/vitasync.log` via Logback.

---

## Authors

VITA-SYNC — Phase 2 Complete
