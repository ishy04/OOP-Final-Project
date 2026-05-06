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

    // ---------------------------------------------------------------------
    // task crud stuff (uc-04 through uc-07)
    // ---------------------------------------------------------------------

    private void handleListTasks() {
        List<Task> tasks = taskManager.getTasksFor(currentUser.getUsername());
        printTasks("All your tasks", tasks);
    }

    private void handleCreateTask() {
        String title = prompt("Title: ");
        if (title.isEmpty()) {
            System.out.println("Title cannot be empty.");
            return;
        }
        String description = prompt("Description (optional): ");
        String categoryName = prompt("Category (default 'General'): ");
        Category category = new Category(categoryName.isEmpty() ? "General" : categoryName);
        LocalDate dueDate = readDate("Due date (YYYY-MM-DD, blank for none): ", true);
        Priority priority = Priority.fromString(prompt("Priority (HIGH/MEDIUM/LOW, default MEDIUM): "));
        TaskStatus status = TaskStatus.fromString(prompt("Status (TODO/IN_PROGRESS/COMPLETE, default TODO): "));
        Task task = taskManager.createTask(currentUser, title, description, category, dueDate, priority, status);
        System.out.println("Created task: " + task);
    }

    private void handleEditTask() {
        String id = prompt("Enter task id to edit: ");
        Task existing = taskManager.findTask(id);
        if (existing == null || !existing.getOwnerUsername().equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println("Task not found.");
            return;
        }
        System.out.println("Editing: " + existing);
        System.out.println("(Leave any field blank to keep its current value.)");
        String title = prompt("New title: ");
        String description = prompt("New description: ");
        String categoryName = prompt("New category: ");
        Category category = categoryName.isEmpty() ? null : new Category(categoryName);
        LocalDate dueDate = readDate("New due date (YYYY-MM-DD): ", true);
        String priorityInput = prompt("New priority (HIGH/MEDIUM/LOW): ");
        Priority priority = priorityInput.isEmpty() ? null : Priority.fromString(priorityInput);
        String statusInput = prompt("New status (TODO/IN_PROGRESS/COMPLETE): ");
        TaskStatus status = statusInput.isEmpty() ? null : TaskStatus.fromString(statusInput);

        boolean ok = taskManager.editTask(
                existing.getId(),
                title.isEmpty() ? null : title,
                description.isEmpty() ? null : description,
                category,
                dueDate,
                priority,
                status);
        System.out.println(ok ? "Task updated." : "Task could not be updated.");
    }

    private void handleDeleteTask() {
        String id = prompt("Enter task id to delete: ");
        Task existing = taskManager.findTask(id);
        if (existing == null || !existing.getOwnerUsername().equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println("Task not found.");
            return;
        }
        boolean ok = taskManager.deleteTask(existing.getId());
        System.out.println(ok ? "Task deleted." : "Could not delete task.");
    }

    private void handleMarkComplete() {
        String id = prompt("Enter task id to mark complete: ");
        Task existing = taskManager.findTask(id);
        if (existing == null || !existing.getOwnerUsername().equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println("Task not found.");
            return;
        }
        boolean ok = taskManager.markTaskComplete(existing.getId());
        System.out.println(ok ? "Task marked complete." : "Could not update task.");
    }

    private void handleSearchTasks() {
        String keyword = prompt("Keyword: ");
        List<Task> results = taskManager.searchTasks(currentUser.getUsername(), keyword);
        printTasks("Search results for '" + keyword + "'", results);
    }

    private void handleFilterTasks() {
        String categoryName = prompt("Category (blank for any): ");
        Category category = categoryName.isEmpty() ? null : new Category(categoryName);
        String priorityInput = prompt("Priority HIGH/MEDIUM/LOW (blank for any): ");
        Priority priority = priorityInput.isEmpty() ? null : Priority.fromString(priorityInput);
        String statusInput = prompt("Status TODO/IN_PROGRESS/COMPLETE (blank for any): ");
        TaskStatus status = statusInput.isEmpty() ? null : TaskStatus.fromString(statusInput);
        LocalDate from = readDate("Due from (YYYY-MM-DD, blank for any): ", true);
        LocalDate to = readDate("Due to (YYYY-MM-DD, blank for any): ", true);

        List<Task> results = taskManager.filterTasks(
                currentUser.getUsername(), category, priority, status, from, to);
        printTasks("Filtered tasks", results);
    }

    // ---------------------------------------------------------------------
    // admin-only stuff (uc-11, uc-12) - calls methods on the admin subclass
    // ---------------------------------------------------------------------

    private void handleManageUsers() {
        Admin admin = (Admin) currentUser;
        System.out.println();
        System.out.println("--- Manage Users ---");
        System.out.println("1) List users");
        System.out.println("2) Reset a user's password");
        System.out.println("3) Delete a user");
        System.out.println("0) Back");
        String choice = prompt("Choose: ");
        switch (choice) {
            case "1":
                System.out.println("Registered users:");
                for (User u : admin.listUsers(taskManager)) {
                    System.out.println("  - " + u);
                }
                break;
            case "2": {
                String username = prompt("Username to reset: ");
                String newPassword = prompt("New password: ");
                boolean ok = admin.resetUserPassword(taskManager, username, newPassword);
                System.out.println(ok ? "Password reset." : "User not found.");
                break;
            }
            case "3": {
                String username = prompt("Username to delete: ");
                boolean ok = admin.deleteUser(taskManager, username);
                System.out.println(ok ? "User deleted." : "User not found or cannot delete self.");
                break;
            }
            case "0": break;
            default: System.out.println("Invalid choice."); break;
        }
    }

    private void handleSystemStats() {
        Admin admin = (Admin) currentUser;
        TaskManager.SystemStats stats = admin.getSystemStats(taskManager);
        System.out.println();
        System.out.println("--- System Statistics ---");
        System.out.println(stats);
    }

    // ---------------------------------------------------------------------
    // little helpers
    // ---------------------------------------------------------------------

    private void printTasks(String title, List<Task> tasks) {
        System.out.println();
        System.out.println(title + ":");
        if (tasks.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        for (Task t : tasks) {
            System.out.println("  - " + t);
        }
    }

    private String prompt(String message) {
        System.out.print(message);
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    private LocalDate readDate(String message, boolean allowEmpty) {
        while (true) {
            String input = prompt(message);
            if (input.isEmpty()) {
                if (allowEmpty) return null;
                System.out.println("A date is required.");
                continue;
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }
}