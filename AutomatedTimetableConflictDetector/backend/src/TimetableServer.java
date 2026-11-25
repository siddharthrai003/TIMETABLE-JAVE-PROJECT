import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Java HTTP server providing REST endpoints and static frontend hosting.
 */
public class TimetableServer {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern JSON_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final HashSet<String> VALID_DAYS = createValidDays();

    private final int port;
    private final ConflictDetector detector;
    private final File frontendDir;
    private HttpServer server;

    public TimetableServer(int port, ConflictDetector detector) {
        this.port = port;
        this.detector = detector;
        this.frontendDir = findFrontendFolder();
    }

    private static HashSet<String> createValidDays() {
        HashSet<String> days = new HashSet<String>();
        days.add("Monday");
        days.add("Tuesday");
        days.add("Wednesday");
        days.add("Thursday");
        days.add("Friday");
        days.add("Saturday");
        return days;
    }

    private File findFrontendFolder() {
        String[] paths = { "frontend", "../frontend", "../../frontend" };
        for (String p : paths) {
            File dir = new File(p);
            if (dir.isDirectory() && new File(dir, "index.html").exists()) {
                return dir;
            }
        }
        return null;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Run each incoming HTTP request in a daemon thread
        server.setExecutor(new Executor() {
            @Override
            public void execute(Runnable task) {
                Thread worker = new Thread(task);
                worker.setDaemon(true);
                worker.start();
            }
        });

        server.createContext("/api/timetable", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleTimetableApi(exchange);
            }
        });

        server.createContext("/api/conflicts/faculty", new ConflictHandler("faculty"));
        server.createContext("/api/conflicts/room", new ConflictHandler("room"));
        server.createContext("/api/conflicts/section", new ConflictHandler("section"));
        server.createContext("/api/conflicts/all", new ConflictHandler("all"));

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                serveFrontendFile(exchange);
            }
        });

        server.start();
    }

    private class ConflictHandler implements HttpHandler {
        private final String scope;

        ConflictHandler(String scope) {
            this.scope = scope;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleConflictsApi(exchange, scope);
        }
    }

    private void handleTimetableApi(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && "/api/timetable".equals(path)) {
                sendTimetableList(exchange);
            } else if ("POST".equals(method) && "/api/timetable".equals(path)) {
                createTimetableEntry(exchange);
            } else if ("DELETE".equals(method) && path.startsWith("/api/timetable/")) {
                deleteTimetableEntry(exchange, path);
            } else {
                sendError(exchange, 404, "Endpoint not found.");
            }
        } catch (ConflictDetector.InvalidTimetableException | IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void sendTimetableList(HttpExchange exchange) throws IOException {
        ArrayList<Timetable> list = detector.getAllEntries();
        StringBuilder entriesJson = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) entriesJson.append(",");
            entriesJson.append(list.get(i).toJson());
        }
        entriesJson.append("]");

        String json = "{"
            + "\"success\":true,"
            + "\"count\":" + list.size() + ","
            + "\"stats\":{"
            + "\"totalEntries\":" + list.size() + ","
            + "\"facultyConflicts\":" + detector.detectFacultyConflicts().size() + ","
            + "\"roomConflicts\":" + detector.detectRoomConflicts().size() + ","
            + "\"sectionConflicts\":" + detector.detectSectionConflicts().size()
            + "},"
            + "\"entries\":" + entriesJson
            + "}";

        sendJson(exchange, 200, json);
    }

    private void createTimetableEntry(HttpExchange exchange)
            throws IOException, ConflictDetector.InvalidTimetableException {
        String body = readBody(exchange);
        HashMap<String, String> data = parseJsonFields(body);

        String day = validateDay(requireText(data, "day", "Day"));
        String subject = requireText(data, "subject", "Subject Name");
        String faculty = requireText(data, "faculty", "Faculty Name");
        String section = requireText(data, "section", "Class/Section");
        String room = requireText(data, "room", "Room Number");
        LocalTime start = parseTime(requireText(data, "startTime", "Start Time"), "Start Time");
        LocalTime end = parseTime(requireText(data, "endTime", "End Time"), "End Time");

        if (!start.isBefore(end)) {
            throw new ConflictDetector.InvalidTimetableException(
                "Invalid time range: Start time must be earlier than end time."
            );
        }

        Timetable added = detector.addEntry(day, subject, faculty, section, room,
                                            start.format(TIME_FMT), end.format(TIME_FMT));

        sendJson(exchange, 201, "{\"success\":true,\"message\":\"Timetable entry added successfully.\",\"entry\":" + added.toJson() + "}");
    }

    private void deleteTimetableEntry(HttpExchange exchange, String path)
            throws IOException, ConflictDetector.InvalidTimetableException {
        String idText = path.substring("/api/timetable/".length()).trim();
        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            throw new ConflictDetector.InvalidTimetableException("Timetable entry ID must be a valid integer.");
        }

        if (!detector.deleteEntry(id)) {
            sendError(exchange, 404, "Timetable entry with ID " + id + " was not found.");
            return;
        }
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Timetable entry #" + id + " deleted successfully.\",\"deletedId\":" + id + "}");
    }

    private void handleConflictsApi(HttpExchange exchange, String scope) throws IOException {
        if (handleCorsPreflight(exchange)) return;

        ArrayList<ConflictDetector.Conflict> conflicts;
        String label;

        if ("faculty".equals(scope)) {
            conflicts = detector.detectFacultyConflicts();
            label = "Faculty Conflicts";
        } else if ("room".equals(scope)) {
            conflicts = detector.detectRoomConflicts();
            label = "Classroom Conflicts";
        } else if ("section".equals(scope)) {
            conflicts = detector.detectSectionConflicts();
            label = "Class/Section Conflicts";
        } else {
            conflicts = detector.detectAllConflicts();
            label = "All Conflicts";
        }

        String json = "{"
            + "\"success\":true,"
            + "\"scope\":\"" + scope + "\","
            + "\"label\":\"" + label + "\","
            + "\"count\":" + conflicts.size() + ","
            + "\"totalEntries\":" + detector.getAllEntries().size() + ","
            + "\"summary\":{"
            + "\"facultyConflicts\":" + detector.detectFacultyConflicts().size() + ","
            + "\"roomConflicts\":" + detector.detectRoomConflicts().size() + ","
            + "\"sectionConflicts\":" + detector.detectSectionConflicts().size()
            + "},"
            + "\"conflicts\":" + ConflictDetector.conflictsToJsonArray(conflicts)
            + "}";

        sendJson(exchange, 200, json);
    }

    private void serveFrontendFile(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (frontendDir == null) {
            sendError(exchange, 404, "Frontend folder not found.");
            return;
        }

        String reqPath = exchange.getRequestURI().getPath();
        if ("/".equals(reqPath)) reqPath = "/index.html";

        File file = new File(frontendDir, reqPath.substring(1));
        if (!file.exists() || file.isDirectory()) {
            sendError(exchange, 404, "File not found.");
            return;
        }

        byte[] bytes = Files.readAllBytes(file.toPath());
        Headers headers = exchange.getResponseHeaders();
        addCorsHeaders(headers);
        headers.set("Content-Type", getMimeType(file.getName()));
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        try {
            os.write(bytes);
        } finally {
            os.close();
        }
    }

    private String getMimeType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=UTF-8";
        if (name.endsWith(".css")) return "text/css; charset=UTF-8";
        if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
        return "application/octet-stream";
    }

    private boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange.getResponseHeaders());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        addCorsHeaders(headers);
        headers.set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        try {
            os.write(bytes);
        } finally {
            os.close();
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{\"success\":false,\"error\":\"" + Timetable.escapeJson(message) + "\"}");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            is.close();
        }
    }

    private HashMap<String, String> parseJsonFields(String json)
            throws ConflictDetector.InvalidTimetableException {
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new ConflictDetector.InvalidTimetableException("Invalid JSON payload.");
        }
        HashMap<String, String> map = new HashMap<String, String>();
        Matcher matcher = JSON_FIELD_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    private String requireText(HashMap<String, String> map, String key, String label)
            throws ConflictDetector.InvalidTimetableException {
        String val = map.get(key);
        if (val == null || val.trim().isEmpty()) {
            throw new ConflictDetector.InvalidTimetableException(label + " is required.");
        }
        return val.trim();
    }

    private String validateDay(String day) throws ConflictDetector.InvalidTimetableException {
        for (String valid : VALID_DAYS) {
            if (valid.equalsIgnoreCase(day)) return valid;
        }
        throw new ConflictDetector.InvalidTimetableException("Day must be between Monday and Saturday.");
    }

    private LocalTime parseTime(String text, String label) throws ConflictDetector.InvalidTimetableException {
        try {
            return LocalTime.parse(text.trim(), TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new ConflictDetector.InvalidTimetableException(label + " must be in HH:mm format.");
        }
    }
}
