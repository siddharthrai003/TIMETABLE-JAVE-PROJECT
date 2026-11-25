import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Stores timetable entries in an ArrayList and detects
 * Faculty, Classroom, and Section conflicts.
 */
public class ConflictDetector {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Custom checked exception for invalid timetable inputs. */
    public static class InvalidTimetableException extends Exception {
        public InvalidTimetableException(String message) {
            super(message);
        }
    }

    /** Holds details of one conflict between two timetable entries. */
    public static class Conflict {
        private final String conflictType;
        private final String day;
        private final String involved;
        private final Timetable firstEntry;
        private final Timetable secondEntry;
        private final String timeOverlap;
        private final String explanation;

        public Conflict(String conflictType, String day, String involved,
                        Timetable firstEntry, Timetable secondEntry,
                        String timeOverlap, String explanation) {
            this.conflictType = conflictType;
            this.day = day;
            this.involved = involved;
            this.firstEntry = firstEntry;
            this.secondEntry = secondEntry;
            this.timeOverlap = timeOverlap;
            this.explanation = explanation;
        }

        public String getDay() { return day; }
        public String getTimeOverlap() { return timeOverlap; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"conflictType\":\"").append(Timetable.escapeJson(conflictType)).append("\",")
              .append("\"day\":\"").append(Timetable.escapeJson(day)).append("\",")
              .append("\"involved\":\"").append(Timetable.escapeJson(involved)).append("\",")
              .append("\"firstEntry\":").append(firstEntry.toJson()).append(",")
              .append("\"secondEntry\":").append(secondEntry.toJson()).append(",")
              .append("\"timeOverlap\":\"").append(Timetable.escapeJson(timeOverlap)).append("\",")
              .append("\"explanation\":\"").append(Timetable.escapeJson(explanation)).append("\"")
              .append("}");
            return sb.toString();
        }
    }

    /** Sorts conflicts by day order and overlapping time. */
    public static class ConflictComparator implements Comparator<Conflict> {
        @Override
        public int compare(Conflict a, Conflict b) {
            int dayDiff = Integer.compare(Timetable.getDayOrder(a.getDay()), Timetable.getDayOrder(b.getDay()));
            return (dayDiff != 0) ? dayDiff : a.getTimeOverlap().compareTo(b.getTimeOverlap());
        }
    }

    private final ArrayList<Timetable> entries = new ArrayList<Timetable>();
    private int nextId = 1;

    /** Preloads sample classes showing faculty, room, and section conflicts. */
    public synchronized void loadSampleData() throws InvalidTimetableException {
        entries.clear();
        nextId = 1;

        addEntry("Monday", "Java", "Mr. Sharma", "CSE-A", "R101", "09:00", "10:00");
        addEntry("Monday", "DBMS", "Mr. Verma", "CSE-A", "R102", "10:00", "11:00");
        addEntry("Monday", "Python", "Mr. Sharma", "CSE-B", "R103", "09:30", "10:30");
        addEntry("Monday", "Operating Systems", "Mr. Kumar", "CSE-B", "R103", "10:00", "11:00");
        addEntry("Monday", "Computer Networks", "Ms. Priya", "CSE-A", "R104", "10:30", "11:30");
        addEntry("Tuesday", "Data Structures", "Dr. Mehta", "CSE-A", "R101", "09:00", "10:00");
        addEntry("Wednesday", "Software Engineering", "Ms. Nair", "CSE-B", "R102", "11:00", "12:00");
    }

    public synchronized ArrayList<Timetable> getAllEntries() {
        return new ArrayList<Timetable>(entries);
    }

    public synchronized Timetable addEntry(String day, String subject, String faculty,
                                           String section, String room,
                                           String startStr, String endStr) throws InvalidTimetableException {
        LocalTime start = LocalTime.parse(startStr, TIME_FMT);
        LocalTime end = LocalTime.parse(endStr, TIME_FMT);

        if (!start.isBefore(end)) {
            throw new InvalidTimetableException("Invalid time range: Start time must be earlier than end time.");
        }

        Timetable item = new Timetable(nextId++, day.trim(), subject.trim(), faculty.trim(),
                                       section.trim(), room.trim(), start, end);
        entries.add(item);
        return item;
    }

    public synchronized boolean deleteEntry(int id) {
        Iterator<Timetable> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().getId() == id) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public synchronized ArrayList<Conflict> detectFacultyConflicts() {
        return findConflicts("Faculty Conflict");
    }

    public synchronized ArrayList<Conflict> detectRoomConflicts() {
        return findConflicts("Classroom Conflict");
    }

    public synchronized ArrayList<Conflict> detectSectionConflicts() {
        return findConflicts("Class/Section Conflict");
    }

    public synchronized ArrayList<Conflict> detectAllConflicts() {
        ArrayList<Conflict> all = new ArrayList<Conflict>();
        all.addAll(detectFacultyConflicts());
        all.addAll(detectRoomConflicts());
        all.addAll(detectSectionConflicts());
        Collections.sort(all, new ConflictComparator());
        return all;
    }

    private ArrayList<Conflict> findConflicts(String type) {
        ArrayList<Conflict> result = new ArrayList<Conflict>();
        HashSet<String> seenPairs = new HashSet<String>();

        for (int i = 0; i < entries.size(); i++) {
            Timetable e1 = entries.get(i);
            for (int j = i + 1; j < entries.size(); j++) {
                Timetable e2 = entries.get(j);

                // Compare only entries on the same day
                if (!e1.getDay().equalsIgnoreCase(e2.getDay())) {
                    continue;
                }

                String res1 = getResource(e1, type);
                String res2 = getResource(e2, type);
                if (!res1.equalsIgnoreCase(res2)) {
                    continue;
                }

                // Overlap check: start1 < end2 && start2 < end1 (back-to-back is allowed)
                boolean overlaps = e1.getStartTime().isBefore(e2.getEndTime())
                                && e2.getStartTime().isBefore(e1.getEndTime());

                if (overlaps) {
                    String pairKey = type + ":" + Math.min(e1.getId(), e2.getId()) + "-" + Math.max(e1.getId(), e2.getId());
                    if (seenPairs.add(pairKey)) {
                        String overlapWindow = getOverlapWindow(e1, e2);
                        String explanation = res1 + " is double-booked on " + e1.getDay()
                            + " between " + e1.getSubject() + " (" + e1.getFormattedStartTime() + "-" + e1.getFormattedEndTime() + ")"
                            + " and " + e2.getSubject() + " (" + e2.getFormattedStartTime() + "-" + e2.getFormattedEndTime() + ")"
                            + ", overlapping from " + overlapWindow + ".";
                        result.add(new Conflict(type, e1.getDay(), res1, e1, e2, overlapWindow, explanation));
                    }
                }
            }
        }
        Collections.sort(result, new ConflictComparator());
        return result;
    }

    private String getResource(Timetable entry, String type) {
        if ("Faculty Conflict".equals(type)) return entry.getFaculty();
        if ("Classroom Conflict".equals(type)) return entry.getRoom();
        return entry.getSection();
    }

    private String getOverlapWindow(Timetable e1, Timetable e2) {
        LocalTime start = e1.getStartTime().isAfter(e2.getStartTime()) ? e1.getStartTime() : e2.getStartTime();
        LocalTime end = e1.getEndTime().isBefore(e2.getEndTime()) ? e1.getEndTime() : e2.getEndTime();
        return start.format(TIME_FMT) + " - " + end.format(TIME_FMT);
    }

    public static String conflictsToJsonArray(ArrayList<Conflict> conflicts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < conflicts.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(conflicts.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }
}
