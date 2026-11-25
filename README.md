# Automated Timetable Conflict Detector

## 1. Project Overview

The **Automated Timetable Conflict Detector** is a Java-based application designed to create and manage class timetables while automatically detecting scheduling conflicts.

The application uses a graphical user interface (GUI) built with Java Swing. It helps administrators schedule classes and identify conflicts involving teachers, classrooms, and student groups.

## 2. Objectives

- Create and manage courses, teachers, rooms, and student groups.
- Schedule classes for different days and time slots.
- Automatically detect timetable conflicts.
- Prevent overlapping schedules for the same teacher, room, or student group.
- Display the timetable in an easy-to-understand interface.

## 3. Technologies Used

- **Programming Language:** Java
- **GUI Framework:** Java Swing
- **Concepts:** Object-Oriented Programming (OOP)
- **Data Storage:** ArrayList (in-memory storage)
- **IDE:** VS Code / IntelliJ IDEA / Eclipse

## 4. Features

- Dashboard displaying timetable statistics.
- Add and manage courses.
- Add teachers and classrooms.
- Manage student groups.
- Schedule classes using days and time slots.
- Automatically detect scheduling conflicts.
- Display scheduled classes in a timetable table.
- Generate conflict reports.

## 5. Project Structure

```text
AutomatedTimetableConflictDetector/
│
├── Course.java
├── Teacher.java
├── Room.java
├── StudentGroup.java
├── TimeSlot.java
├── ScheduledClass.java
├── ConflictDetector.java
├── TimetableManager.java
├── TimetableApp.java
├── Main.java
└── README.md
```

## 6. How Conflict Detection Works

The application checks whether two classes are scheduled at overlapping times.

The overlap condition is:

```java
start1 < end2 && start2 < end1
```

A conflict is reported when:

1. The same teacher is assigned to two overlapping classes.
2. The same classroom is assigned to two overlapping classes.
3. The same student group is assigned to two overlapping classes.

The application also checks that the classes are scheduled on the same day.

## 7. How to Run the Project

### Step 1: Install Java

Install JDK 17 or later.

Check whether Java is installed:

```bash
java -version
javac -version
```

### Step 2: Open the Project

Open the project folder in VS Code or any Java IDE.

### Step 3: Compile the Java Files

Open the terminal inside the project folder and execute:

```bash
javac *.java
```

### Step 4: Run the Application

Execute:

```bash
java Main
```

The graphical timetable management application will open.

## 8. Object-Oriented Programming Concepts Used

| Concept | Implementation |
|---|---|
| Classes and Objects | Course, Teacher, Room, StudentGroup |
| Encapsulation | Private data members and getter methods |
| Abstraction | Separate classes for timetable operations |
| Inheritance | Can be extended where required |
| Polymorphism | Can be extended for different scheduling strategies |
| ArrayList | Stores timetable data in memory |

## 9. Limitations

- Data is stored in memory and is lost when the application closes.
- No database connectivity is included.
- The application is designed for basic timetable management.
- Advanced automatic timetable generation is not included.

## 10. Future Enhancements

- Integrate MySQL database using JDBC.
- Add automatic timetable generation.
- Introduce user login and administrator access.
- Export timetables as PDF or Excel files.
- Add advanced scheduling optimization algorithms.

## 11. Conclusion

The Automated Timetable Conflict Detector demonstrates how Java and Object-Oriented Programming can be used to solve a practical scheduling problem. It simplifies timetable management and reduces scheduling errors by automatically detecting conflicts involving teachers, rooms, and student groups.

---

**Project Type:** Java Desktop Application  
**Developed For:** College Academic Project  
**Version:** 1.0
