import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one class entry in the college timetable.
 */
public class Timetable implements Comparable<Timetable> {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private int id;
    private String day;
    private String subject;
    private String faculty;
    private String section;
    private String room;
    private LocalTime startTime;
    private LocalTime endTime;

    public Timetable(int id, String day, String subject, String faculty,
                     String section, String room, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.day = day;
        this.subject = subject;
        this.faculty = faculty;
        this.section = section;
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getFormattedStartTime() { return startTime.format(TIME_FMT); }
    public String getFormattedEndTime() { return endTime.format(TIME_FMT); }

    public static int getDayOrder(String dayName) {
        String d = (dayName == null) ? "" : dayName.trim().toLowerCase();
        if (d.equals("monday")) return 1;
        if (d.equals("tuesday")) return 2;
        if (d.equals("wednesday")) return 3;
        if (d.equals("thursday")) return 4;
        if (d.equals("friday")) return 5;
        if (d.equals("saturday")) return 6;
        return 7;
    }

    @Override
    public int compareTo(Timetable other) {
        int dayDiff = Integer.compare(getDayOrder(this.day), getDayOrder(other.day));
        if (dayDiff != 0) return dayDiff;
        int timeDiff = this.startTime.compareTo(other.startTime);
        return (timeDiff != 0) ? timeDiff : Integer.compare(this.id, other.id);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"id\":").append(id).append(",")
          .append("\"day\":\"").append(escapeJson(day)).append("\",")
          .append("\"subject\":\"").append(escapeJson(subject)).append("\",")
          .append("\"faculty\":\"").append(escapeJson(faculty)).append("\",")
          .append("\"section\":\"").append(escapeJson(section)).append("\",")
          .append("\"room\":\"").append(escapeJson(room)).append("\",")
          .append("\"startTime\":\"").append(getFormattedStartTime()).append("\",")
          .append("\"endTime\":\"").append(getFormattedEndTime()).append("\"")
          .append("}");
        return sb.toString();
    }

    public static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
