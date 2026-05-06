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
        // make sure there's a default admin (admin/admin123) so uc-11 and uc-12 work on a fresh install. 
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
    
    // ---------------------------------------------------------------------
    // login/register menu (uc-01, uc-02)
    // ---------------------------------------------------------------------

    private boolean showAuthMenu() {
        System.out.println();
        System.out.println("--- Welcome ---");
        System.out.println("1) Login");
        System.out.println("2) Register");
        System.out.println("3) Exit");
        String choice = prompt("Choose an option: ");
        switch (choice) {
            case "1": handleLogin(); return true;
            case "2": handleRegister(); return true;
            case "3": return false;
            default:
                System.out.println("Invalid choice.");
                return true;
        }
    }

    private void handleRegister() {
        String username = prompt("Choose a username: ");
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        String password = prompt("Choose a password: ");
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }
        User user = taskManager.registerUser(username, password);
        if (user == null) {
            System.out.println("That username is already taken.");
        } else {
            System.out.println("Account created. You can now log in.");
        }
    }

    private void handleLogin() {
        String username = prompt("Username: ");
        String password = prompt("Password: ");
        User user = taskManager.authenticate(username, password);
        if (user == null) {
            System.out.println("Invalid username or password.");
            return;
        }
        currentUser = user;
        System.out.println("Welcome, " + user.getUsername()
                + (user.isAdmin() ? " (administrator)" : "") + "!");
    }

    private void handleLogout() {
        System.out.println("Logged out.");
        currentUser = null;
    }
    
    // ---------------------------------------------------------------------
    // main menu (uc-03 dashboard + jumps into all the other use cases)
    // ---------------------------------------------------------------------

    private void showMainMenu() {
        System.out.println();
        System.out.println("--- Main Menu (" + currentUser.getUsername() + ") ---");
        renderDashboard();
        System.out.println();
        System.out.println("1) View all my tasks");
        System.out.println("2) Create task");
        System.out.println("3) Edit task");
        System.out.println("4) Delete task");
        System.out.println("5) Mark task complete");
        System.out.println("6) Search tasks");
        System.out.println("7) Filter tasks");
        if (currentUser.isAdmin()) {
            System.out.println("8) Admin: manage users");
            System.out.println("9) Admin: system statistics");
        }
        System.out.println("0) Logout");
        String choice = prompt("Choose an option: ");
        switch (choice) {
            case "1": handleListTasks(); break;
            case "2": handleCreateTask(); break;
            case "3": handleEditTask(); break;
            case "4": handleDeleteTask(); break;
            case "5": handleMarkComplete(); break;
            case "6": handleSearchTasks(); break;
            case "7": handleFilterTasks(); break;
            case "8":
                if (currentUser.isAdmin()) handleManageUsers();
                else System.out.println("Invalid choice.");
                break;
            case "9":
                if (currentUser.isAdmin()) handleSystemStats();
                else System.out.println("Invalid choice.");
                break;
            case "0": handleLogout(); break;
            default: System.out.println("Invalid choice."); break;
        }
    }

    private void renderDashboard() {
        List<Task> active = taskManager.getActiveTasksFor(currentUser.getUsername());
        List<Task> completed = taskManager.getCompletedTasksFor(currentUser.getUsername());
        List<Task> overdue = taskManager.getOverdueTasksFor(currentUser.getUsername());
        System.out.printf("Dashboard - active: %d, completed: %d, overdue: %d%n",
                active.size(), completed.size(), overdue.size());
        if (!overdue.isEmpty()) {
            System.out.println("  ! Overdue tasks need attention:");
            for (Task t : overdue) {
                System.out.println("    - " + t);
            }
        }
    }
