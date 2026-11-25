# Automated Timetable Conflict Detector

A full-stack college-level web application designed to manage academic timetables and automatically detect scheduling conflicts for **faculty members**, **classrooms**, and **classes/sections**.

The frontend is built with clean **HTML5, CSS3, and Vanilla JavaScript**, while all business logic, in-memory storage (`ArrayList`), REST API routing, and conflict detection run inside a **Core Java backend** using `com.sun.net.httpserver.HttpServer`.

---

## Project Structure

```text
AutomatedTimetableConflictDetector/
│
├── backend/
│   └── src/
│       ├── Main.java
│       ├── Timetable.java
│       ├── ConflictDetector.java
│       └── TimetableServer.java
│
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── script.js
│
└── README.md
```

---

## Technology Stack

- **Frontend:** HTML5, CSS3 (Apple-inspired responsive dashboard), Vanilla JavaScript (`fetch()` API)
- **Backend:** Core Java (OOP, `java.time.LocalTime`, `com.sun.net.httpserver.HttpServer`)
- **Storage:** In-memory `ArrayList<Timetable>` (no database required)

---

## Backend Startup Instructions

1. Ensure **Java JDK 17 or higher** (tested on JDK 26) is installed:
   ```bash
   java -version
   javac -version
   ```
2. Navigate to the `backend/src` directory:
   ```bash
   cd AutomatedTimetableConflictDetector/backend/src
   ```
3. Compile all Java files:
   ```bash
   javac -d ../out *.java
   ```
4. Start the Java backend server:
   ```bash
   java -cp ../out Main
   ```
5. You will see a startup banner confirming the server is running at `http://localhost:8080`.

---

## Frontend Startup Instructions

Because `TimetableServer.java` includes CORS headers **and** serves static files from `frontend/`, you can launch the UI in either of two ways:

1. **Recommended (served directly by Java):**
   Open your browser and visit:
   ```text
   http://localhost:8080
   ```
2. **Direct file open:**
   With the Java backend running on port `8080`, open `AutomatedTimetableConflictDetector/frontend/index.html` in any modern web browser.

---

## Sample Test Data

On startup, `Main.java` loads the following entries via `ConflictDetector.loadSampleData()`:

| ID | Day | Subject | Faculty | Section | Room | Start Time | End Time | Conflict Demonstrated |
|----|-----|---------|---------|---------|------|------------|----------|-----------------------|
| 1 | Monday | Java | Mr. Sharma | CSE-A | R101 | 09:00 | 10:00 | Faculty conflict with #3 |
| 2 | Monday | DBMS | Mr. Verma | CSE-A | R102 | 10:00 | 11:00 | Valid back-to-back with #1; Section conflict with #5 |
| 3 | Monday | Python | Mr. Sharma | CSE-B | R103 | 09:30 | 10:30 | Faculty conflict with #1; Room & Section conflict with #4 |
| 4 | Monday | Operating Systems | Mr. Kumar | CSE-B | R103 | 10:00 | 11:00 | Room & Section conflict with #3 |
| 5 | Monday | Computer Networks | Ms. Priya | CSE-A | R104 | 10:30 | 11:30 | Section conflict with #2 (`CSE-A`) |
| 6 | Tuesday | Data Structures | Dr. Mehta | CSE-A | R101 | 09:00 | 10:00 | Conflict-free |
| 7 | Wednesday | Software Engineering | Ms. Nair | CSE-B | R102 | 11:00 | 12:00 | Conflict-free |

---

## Conflict Detection Algorithm Explanation

All conflict detection executes inside `ConflictDetector.java`:

1. **Nested Loop Pairwise Comparison (`j = i + 1`):**
   Every timetable entry `i` is compared only with subsequent entries `j > i`. This guarantees each pair is inspected once, avoiding self-comparisons and duplicate conflict reports.
2. **Same-Day Filter:**
   Two classes on different days cannot collide, so entries with different `day` values are skipped immediately.
3. **Resource Matching:**
   - **Faculty Conflict:** `entry1.getFaculty().equalsIgnoreCase(entry2.getFaculty())`
   - **Classroom Conflict:** `entry1.getRoom().equalsIgnoreCase(entry2.getRoom())`
   - **Class/Section Conflict:** `entry1.getSection().equalsIgnoreCase(entry2.getSection())`
4. **Time-Overlap Formula (`LocalTime`):**
   ```java
   start1.isBefore(end2) && start2.isBefore(end1)
   ```
   Using strict `isBefore()` ensures that **back-to-back classes** (for example, `09:00–10:00` and `10:00–11:00`) evaluate to `false` and are **not** flagged as conflicts.

---

## REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/timetable` | Returns all timetable entries and dashboard statistics |
| `POST` | `/api/timetable` | Validates input and adds a new timetable entry |
| `DELETE` | `/api/timetable/{id}` | Deletes a timetable entry by its ID |
| `GET` | `/api/conflicts/faculty` | Detects all faculty scheduling conflicts |
| `GET` | `/api/conflicts/room` | Detects all classroom double-bookings |
| `GET` | `/api/conflicts/section` | Detects all class/section overlaps |
| `GET` | `/api/conflicts/all` | Returns all faculty, classroom, and section conflicts |
