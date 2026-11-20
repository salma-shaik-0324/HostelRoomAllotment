import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HostelService {
    public static final String STUDENTS_FILE = "students.txt";
    public static final String ROOMS_FILE = "rooms.txt";
    public static final String ALLOTMENTS_FILE = "allotments.txt";
    public static final String REPORT_FILE = "report.txt";

    private final Path dataDir = Paths.get("data");

    /** Thrown when a line in a data file is invalid. */
    public static class FileDataException extends Exception {
        public FileDataException(String message) {
            super(message);
        }
    }

    /** Text fields must not be blank and must not contain | or line breaks. */
    public static boolean isValidText(String text) {
        return text != null
                && !text.trim().isEmpty()
                && text.indexOf('|') < 0
                && text.indexOf('\n') < 0
                && text.indexOf('\r') < 0;
    }

    public void prepareFolder() throws IOException {
        Files.createDirectories(dataDir);
    }

    // ---------------------------------------------------------------- loading

    public ArrayList<Student> loadStudents() throws IOException, FileDataException {
        ArrayList<Student> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        List<String> lines = readLines(STUDENTS_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            int lineNo = i + 1;
            String[] f = split(line, 3, STUDENTS_FILE, lineNo);
            int id = parseId(f[0], STUDENTS_FILE, lineNo);
            if (!ids.add(id)) {
                throw error(STUDENTS_FILE, lineNo, "duplicate ID " + id);
            }
            if (!isValidText(f[1]) || !isValidText(f[2])) {
                throw error(STUDENTS_FILE, lineNo, "name and department are required");
            }
            list.add(new Student(id, f[1].trim(), f[2].trim()));
        }
        return list;
    }

    public ArrayList<Room> loadRooms() throws IOException, FileDataException {
        ArrayList<Room> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> numbers = new HashSet<>();
        List<String> lines = readLines(ROOMS_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            int lineNo = i + 1;
            String[] f = split(line, 2, ROOMS_FILE, lineNo);
            int id = parseId(f[0], ROOMS_FILE, lineNo);
            if (!ids.add(id)) {
                throw error(ROOMS_FILE, lineNo, "duplicate ID " + id);
            }
            if (!isValidText(f[1])) {
                throw error(ROOMS_FILE, lineNo, "room number is required");
            }
            String number = f[1].trim().toUpperCase(Locale.ROOT);
            if (!numbers.add(number)) {
                throw error(ROOMS_FILE, lineNo, "duplicate room number " + number);
            }
            list.add(new Room(id, number));
        }
        return list;
    }

    public ArrayList<Allotment> loadAllotments(List<Student> students, List<Room> rooms)
            throws IOException, FileDataException {
        Set<Integer> studentIds = new HashSet<>();
        for (Student s : students) {
            studentIds.add(s.getId());
        }
        Set<Integer> roomIds = new HashSet<>();
        for (Room r : rooms) {
            roomIds.add(r.getId());
        }

        ArrayList<Allotment> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<Integer> activeStudents = new HashSet<>();
        Set<String> activeBeds = new HashSet<>();
        List<String> lines = readLines(ALLOTMENTS_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            int lineNo = i + 1;
            String[] f = split(line, 5, ALLOTMENTS_FILE, lineNo);
            int id = parseId(f[0], ALLOTMENTS_FILE, lineNo);
            int studentId = parseId(f[1], ALLOTMENTS_FILE, lineNo);
            int roomId = parseId(f[2], ALLOTMENTS_FILE, lineNo);
            int bed = parseId(f[3], ALLOTMENTS_FILE, lineNo);

            if (!ids.add(id)) {
                throw error(ALLOTMENTS_FILE, lineNo, "duplicate ID " + id);
            }
            if (!studentIds.contains(studentId)) {
                throw error(ALLOTMENTS_FILE, lineNo, "unknown student ID " + studentId);
            }
            if (!roomIds.contains(roomId)) {
                throw error(ALLOTMENTS_FILE, lineNo, "unknown room ID " + roomId);
            }
            if (bed < 1 || bed > Room.BEDS_PER_ROOM) {
                throw error(ALLOTMENTS_FILE, lineNo, "bed number must be 1 to " + Room.BEDS_PER_ROOM);
            }

            Allotment.AllotmentStatus status;
            try {
                status = Allotment.AllotmentStatus.valueOf(f[4].trim());
            } catch (IllegalArgumentException e) {
                throw error(ALLOTMENTS_FILE, lineNo, "invalid status '" + f[4] + "'");
            }

            if (status == Allotment.AllotmentStatus.ACTIVE) {
                if (!activeStudents.add(studentId)) {
                    throw error(ALLOTMENTS_FILE, lineNo,
                            "student " + studentId + " has more than one ACTIVE allotment");
                }
                if (!activeBeds.add(roomId + "-" + bed)) {
                    throw error(ALLOTMENTS_FILE, lineNo,
                            "room " + roomId + " bed " + bed + " has more than one ACTIVE allotment");
                }
            }
            list.add(new Allotment(id, studentId, roomId, bed, status));
        }
        return list;
    }

    // ----------------------------------------------------------------- saving

    public void saveStudents(List<Student> students) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Student s : students) {
            lines.add(s.getId() + "|" + s.getName() + "|" + s.getDepartment());
        }
        writeFile(STUDENTS_FILE, lines);
    }

    public void saveRooms(List<Room> rooms) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Room r : rooms) {
            lines.add(r.getId() + "|" + r.getRoomNumber());
        }
        writeFile(ROOMS_FILE, lines);
    }

    public void saveAllotments(List<Allotment> allotments) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Allotment a : allotments) {
            lines.add(a.getId() + "|" + a.getStudentId() + "|" + a.getRoomId()
                    + "|" + a.getBedNumber() + "|" + a.getStatus());
        }
        writeFile(ALLOTMENTS_FILE, lines);
    }

    public void saveReport(List<String> reportLines) throws IOException {
        writeFile(REPORT_FILE, reportLines);
    }

    // ---------------------------------------------------------------- helpers

    /** Writes to a temporary file first, then replaces the original. */
    private void writeFile(String fileName, List<String> lines) throws IOException {
        Files.createDirectories(dataDir);
        Path target = dataDir.resolve(fileName);
        Path temp = dataDir.resolve(fileName + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private List<String> readLines(String fileName) throws IOException {
        Path path = dataDir.resolve(fileName);
        List<String> lines = new ArrayList<>();
        if (!Files.exists(path)) {
            return lines; // first run: empty list
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IOException("Cannot read data/" + fileName + ": " + e.getMessage(), e);
        }
        return lines;
    }

    private String[] split(String line, int expected, String file, int lineNo)
            throws FileDataException {
        String[] fields = line.split("\\|", -1);
        if (fields.length != expected) {
            throw error(file, lineNo, "expected " + expected + " fields but found " + fields.length);
        }
        return fields;
    }

    private int parseId(String text, String file, int lineNo) throws FileDataException {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 1) {
                throw error(file, lineNo, "number must be 1 or more: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw error(file, lineNo, "not a valid number: '" + text + "'");
        }
    }

    private FileDataException error(String file, int lineNo, String message) {
        return new FileDataException("data/" + file + " line " + lineNo + ": " + message);
    }

    // ===== Hostel actions =====

    private ArrayList<Student> students = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<>();
    private ArrayList<Allotment> allotments = new ArrayList<>();
    private HashSet<String> roomNumbers = new HashSet<>();

    private int nextStudentId = 1;
    private int nextRoomId = 1;
    private int nextAllotmentId = 1;

    // ---------------------------------------------------------------- startup

    /** Loads all files into temporary lists; keeps them only if everything is valid. */
    public void loadData() throws IOException, FileDataException {
        Files.createDirectories(dataDir);
        ArrayList<Student> loadedStudents = loadStudents();
        ArrayList<Room> loadedRooms = loadRooms();
        ArrayList<Allotment> loadedAllotments =
                loadAllotments(loadedStudents, loadedRooms);

        students = loadedStudents;
        rooms = loadedRooms;
        allotments = loadedAllotments;

        roomNumbers = new HashSet<>();
        int maxStudent = 0;
        int maxRoom = 0;
        int maxAllotment = 0;
        for (Student s : students) {
            maxStudent = Math.max(maxStudent, s.getId());
        }
        for (Room r : rooms) {
            roomNumbers.add(r.getRoomNumber());
            maxRoom = Math.max(maxRoom, r.getId());
        }
        for (Allotment a : allotments) {
            maxAllotment = Math.max(maxAllotment, a.getId());
        }
        nextStudentId = maxStudent + 1;
        nextRoomId = maxRoom + 1;
        nextAllotmentId = maxAllotment + 1;
    }

    // ---------------------------------------------------------------- lookups

    public Student findStudent(int id) {
        for (Student s : students) {
            if (s.getId() == id) {
                return s;
            }
        }
        return null;
    }

    public Room findRoom(int id) {
        for (Room r : rooms) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    public boolean studentExists(int id) {
        return findStudent(id) != null;
    }

    public boolean roomExists(int id) {
        return findRoom(id) != null;
    }

    private Allotment findAllotment(int id) {
        for (Allotment a : allotments) {
            if (a.getId() == id) {
                return a;
            }
        }
        return null;
    }

    public boolean allotmentExists(int id) {
        return findAllotment(id) != null;
    }

    private Allotment findActiveAllotmentForStudent(int studentId) {
        for (Allotment a : allotments) {
            if (a.getStudentId() == studentId && a.getStatus() == Allotment.AllotmentStatus.ACTIVE) {
                return a;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ student/room

    public boolean registerStudent(String name, String department) {
        if (!isValidText(name) || !isValidText(department)) {
            System.out.println("Name and department are required and cannot contain '|'.");
            return false;
        }
        Student student = new Student(nextStudentId, name.trim(), department.trim());
        students.add(student);
        try {
            saveStudents(students);
        } catch (IOException e) {
            students.remove(student);
            System.out.println("Could not save student: " + e.getMessage());
            return false;
        }
        nextStudentId++;
        System.out.println("Student " + student.getId() + " registered. Your student ID is "
                + student.getId() + ".");
        return true;
    }

    public boolean addRoom(String input) {
        if (!isValidText(input)) {
            System.out.println("Room number is required and cannot contain '|'.");
            return false;
        }
        String number = input.trim().toUpperCase(Locale.ROOT);
        if (roomNumbers.contains(number)) {
            System.out.println("Room " + number + " already exists.");
            return false;
        }
        Room room = new Room(nextRoomId, number);
        rooms.add(room);
        try {
            saveRooms(rooms);
        } catch (IOException e) {
            rooms.remove(room);
            System.out.println("Could not save room: " + e.getMessage());
            return false;
        }
        roomNumbers.add(number); // add only after the record is saved
        nextRoomId++;
        System.out.println("Room " + room.getId() + " saved. Beds: " + Room.BEDS_PER_ROOM);
        return true;
    }

    /** Builds the bed array from ACTIVE allotments. 0 = empty, otherwise student ID. */
    public int[] getBedOccupants(int roomId) {
        int[] occupants = new int[Room.BEDS_PER_ROOM];
        for (Allotment allotment : allotments) {
            if (allotment.getRoomId() == roomId
                    && allotment.getStatus() == Allotment.AllotmentStatus.ACTIVE) {
                occupants[allotment.getBedNumber() - 1] = allotment.getStudentId();
            }
        }
        return occupants;
    }

    // ------------------------------------------------------ allotment/checkout

    public boolean allotBed(int studentId, int roomId) {
        if (findStudent(studentId) == null) {
            System.out.println("Student ID not found.");
            return false;
        }
        if (findRoom(roomId) == null) {
            System.out.println("Room ID not found.");
            return false;
        }
        if (findActiveAllotmentForStudent(studentId) != null) {
            System.out.println("Student " + studentId + " already has an ACTIVE allotment.");
            return false;
        }

        int[] occupants = getBedOccupants(roomId);
        int bedNumber = 0;
        for (int i = 0; i < occupants.length; i++) {
            if (occupants[i] == 0) {
                bedNumber = i + 1;
                break;
            }
        }
        if (bedNumber == 0) {
            System.out.println("Room is full. All four beds are occupied.");
            return false;
        }

        Allotment allotment = new Allotment(nextAllotmentId, studentId, roomId,
                bedNumber, Allotment.AllotmentStatus.ACTIVE);
        allotments.add(allotment);
        try {
            saveAllotments(allotments);
        } catch (IOException e) {
            allotments.remove(allotment);
            System.out.println("Could not save allotment: " + e.getMessage());
            return false;
        }
        nextAllotmentId++;
        System.out.println("Allotment " + allotment.getId() + " saved. Bed: " + bedNumber);
        return true;
    }

    public boolean checkout(int allotmentId) {
        Allotment allotment = findAllotment(allotmentId);
        if (allotment == null) {
            System.out.println("Allotment ID not found.");
            return false;
        }
        if (allotment.getStatus() == Allotment.AllotmentStatus.CHECKED_OUT) {
            System.out.println("Allotment " + allotmentId + " is already checked out.");
            return false;
        }

        allotment.checkout();
        try {
            saveAllotments(allotments);
        } catch (IOException e) {
            // cancel the change in memory by replacing it with an ACTIVE copy
            int index = allotments.indexOf(allotment);
            allotments.set(index, new Allotment(allotment.getId(), allotment.getStudentId(),
                    allotment.getRoomId(), allotment.getBedNumber(), Allotment.AllotmentStatus.ACTIVE));
            System.out.println("Could not save checkout: " + e.getMessage());
            return false;
        }
        System.out.println("Checkout saved.");
        Room room = findRoom(allotment.getRoomId());
        System.out.println("Room " + room.getRoomNumber() + " bed occupants: "
                + Arrays.toString(getBedOccupants(room.getId())));
        return true;
    }

    // ---------------------------------------------------------------- display

    public void viewRoomsAndBeds() {
        if (rooms.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Room room : rooms) {
            room.showDetails();
            int[] occupants = getBedOccupants(room.getId());
            int free = 0;
            for (int occupant : occupants) {
                if (occupant == 0) {
                    free++;
                }
            }
            System.out.println("   Bed occupants: " + Arrays.toString(occupants)
                    + " | Free beds: " + free);
        }
    }

    public void viewStudents() {
        if (students.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Student s : students) {
            s.showProfile();
        }
    }

    public void showActiveAllotment(int studentId) {
        Allotment allotment = findActiveAllotmentForStudent(studentId);
        if (allotment == null) {
            System.out.println("No records found.");
            return;
        }
        Room room = findRoom(allotment.getRoomId());
        System.out.println("Allotment ID: " + allotment.getId()
                + " | Room: " + room.getRoomNumber()
                + " | Bed: " + allotment.getBedNumber()
                + " | " + allotment.getStatus());
    }

    // ---------------------------------------------------------------- reports

    public List<String> buildReport() {
        int roomCount = rooms.size();
        int totalBeds = roomCount * Room.BEDS_PER_ROOM;
        int occupiedBeds = 0;
        for (Room room : rooms) {
            for (int occupant : getBedOccupants(room.getId())) {
                if (occupant != 0) {
                    occupiedBeds++;
                }
            }
        }
        int freeBeds = totalBeds - occupiedBeds;

        List<String> lines = new ArrayList<>();
        lines.add("Hostel Occupancy Report");
        lines.add("Rooms: " + roomCount);
        lines.add("Total beds: " + totalBeds);
        lines.add("Occupied beds: " + occupiedBeds);
        lines.add("Free beds: " + freeBeds);

        List<String> active = new ArrayList<>();
        for (Allotment a : allotments) {
            if (a.getStatus() == Allotment.AllotmentStatus.ACTIVE) {
                Student s = findStudent(a.getStudentId());
                Room r = findRoom(a.getRoomId());
                active.add("  Student " + s.getId() + " " + s.getName()
                        + " - Room " + r.getRoomNumber() + ", Bed " + a.getBedNumber());
            }
        }
        lines.add("Students with active allotment: " + active.size());
        lines.addAll(active);
        return lines;
    }

    public void showReport() {
        for (String line : buildReport()) {
            System.out.println(line);
        }
    }

    public boolean saveReport() {
        try {
            saveReport(buildReport());
            System.out.println("Report saved to data/" + REPORT_FILE);
            return true;
        } catch (IOException e) {
            System.out.println("Could not save report: " + e.getMessage());
            return false;
        }
    }
}