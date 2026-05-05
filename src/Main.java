import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * the cli entry point for the task management system.
 *
 * this is basically the loginui/taskui from the sequence diagrams. it just
 * reads input and hands everything off to taskmanager and filestoragemanager.
 * all the real logic lives in the domain classes, not here.
 */
public class Main {

    private final Scanner scanner = new Scanner(System.in);
    private final TaskManager taskManager;
    private User currentUser;

    public Main(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public static void main(String[] args) {
        FileStorageManager storage = new FileStorageManager();
        TaskManager manager = new TaskManager(storage);
        // make sure there's a default admin (admin/admin123) so uc-11 and
        // uc-12 work on a fresh install. won't touch existing data.
        manager.ensureDefaultAdmin("admin", "admin123");
        new Main(manager).run();
    }

    public void run() {
        printBanner();
        while (true) {
            if (currentUser == null) {
                if (!showAuthMenu()) {
                    break;
                }
            } else {
                showMainMenu();
            }
        }
        System.out.println("Goodbye!");
    }

    private void printBanner() {
        System.out.println("============================================");
        System.out.println("  Task Management System  -  CLI Interface  ");
        System.out.println("============================================");
        System.out.println("Default admin account: username 'admin', password 'admin123'");
    }