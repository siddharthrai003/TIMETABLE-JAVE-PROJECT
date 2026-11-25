import java.io.IOException;

/**
 * Entry point for the Automated Timetable Conflict Detector.
 */
public class Main {

    public static void main(String[] args) {
        int port = 8080;
        ConflictDetector detector = new ConflictDetector();

        try {
            detector.loadSampleData();
            TimetableServer server = new TimetableServer(port, detector);
            server.start();

            System.out.println("====================================================");
            System.out.println("  Automated Timetable Conflict Detector is RUNNING  ");
            System.out.println("  Open in browser: http://localhost:" + port);
            System.out.println("====================================================");
        } catch (ConflictDetector.InvalidTimetableException | IOException e) {
            System.err.println("Startup error: " + e.getMessage());
        }
    }
}
