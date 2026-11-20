import java.io.IOException;
import java.util.Scanner;
import java.util.function.IntPredicate;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Admin admin = new Admin();
    private static HostelService service;

    public static void main(String[] args) {
        service = new HostelService();

        try {
            service.loadData();
        } catch (IOException | HostelService.FileDataException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Fix the file in the data folder and run the program again.");
            return;
        }

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("MAIN MENU");
            System.out.println("1. Register student");
            System.out.println("2. Admin menu");
            System.out.println("3. Student menu");
            System.out.println("0. Exit");
            int choice = readChoice();
            switch (choice) {
                case 1 -> registerStudent();
                case 2 -> showAdminMenu();
                case 3 -> showStudentMenu();
                case 0 -> running = false;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
        System.out.println("Goodbye.");
    }

    // ------------------------------------------------------------------ menus

    private static void showAdminMenu() {
        User user = admin;       // polymorphism: User reference, Admin object
        user.showProfile();

        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("ADMIN MENU");
            System.out.println("1. Add room");
            System.out.println("2. View rooms and beds");
            System.out.println("3. View students");
            System.out.println("4. Allot bed");
            System.out.println("5. Record checkout");
            System.out.println("6. View reports");
            System.out.println("0. Back");
            int choice = readChoice();
            switch (choice) {
                case 1 -> service.addRoom(readLine("Room number (e.g. A101): "));
                case 2 -> service.viewRoomsAndBeds();
                case 3 -> service.viewStudents();
                case 4 -> allotBed();
                case 5 -> recordCheckout();
                case 6 -> showReports();
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void showStudentMenu() {
        int studentId = readExistingId("Enter student ID", service::studentExists,
                "Student ID not found.");
        if (studentId == -1) {
            return;
        }
        User user = service.findStudent(studentId); // polymorphism: Student as User

        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("STUDENT MENU");
            System.out.println("1. View my profile");
            System.out.println("2. View rooms");
            System.out.println("3. View my active allotment");
            System.out.println("0. Back");
            int choice = readChoice();
            switch (choice) {
                case 1 -> user.showProfile();
                case 2 -> service.viewRoomsAndBeds();
                case 3 -> service.showActiveAllotment(studentId);
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------- actions

    private static void registerStudent() {
        String name;
        do {
            name = readLine("Name: ");
            if (!HostelService.isValidText(name)) {
                System.out.println("Name is required and cannot contain '|'.");
            }
        } while (!HostelService.isValidText(name));

        String department;
        do {
            department = readLine("Department: ");
            if (!HostelService.isValidText(department)) {
                System.out.println("Department is required and cannot contain '|'.");
            }
        } while (!HostelService.isValidText(department));

        service.registerStudent(name, department);
    }

    private static void allotBed() {
        service.viewStudents();
        int studentId = readExistingId("Student ID", service::studentExists,
                "Student ID not found.");
        if (studentId == -1) {
            return;
        }
        service.viewRoomsAndBeds();
        int roomId = readExistingId("Room ID", service::roomExists, "Room ID not found.");
        if (roomId == -1) {
            return;
        }
        service.allotBed(studentId, roomId);
    }

    private static void recordCheckout() {
        int allotmentId = readExistingId("Allotment ID", service::allotmentExists,
                "Allotment ID not found.");
        if (allotmentId == -1) {
            return;
        }
        service.checkout(allotmentId);
    }

    private static void showReports() {
        service.showReport();
        String answer = readLine("Save report? (y/n): ").trim();
        if (answer.equalsIgnoreCase("y")) {
            service.saveReport();
        }
    }

    // ------------------------------------------------------------ input helpers

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println("\nInput closed. Exiting.");
            System.exit(0);
        }
        return scanner.nextLine();
    }

    /** Returns the number typed, or -1 if the input is not a valid number. */
    private static int readChoice() {
        String text = readLine("Enter choice: ").trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Asks until an existing ID is entered. Returns -1 if the user cancels. */
    private static int readExistingId(String prompt, IntPredicate exists, String notFoundMessage) {
        while (true) {
            String text = readLine(prompt + " (-1 to cancel): ").trim();
            try {
                int id = Integer.parseInt(text);
                if (id == -1) {
                    return -1;
                }
                if (exists.test(id)) {
                    return id;
                }
                System.out.println(notFoundMessage);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }
}