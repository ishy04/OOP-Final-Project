import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * the thing that owns all the tasks and is the only way to touch them.
 *
 * taskmanager owns task objects - it creates them, updates them, deletes
 * them, and saves them. tasks don't really exist outside a taskmanager.
 *
 * it leans on filestoragemanager to write stuff to disk after every change.
 * we pass that in via the constructor so we could swap out storage later
 * without changing this class.
 *
 * also keeps track of users so the admin features (uc-11, uc-12) don't
 * have to deal with file stuff in the cli.
 */
public class TaskManager {

    private final List<Task> tasks;
    private final Map<String, User> users;
    private final FileStorageManager storage;

    public TaskManager(FileStorageManager storage) {
        if (storage == null) {
            throw new IllegalArgumentException("FileStorageManager cannot be null");
        }
        this.storage = storage;
        this.tasks = new ArrayList<>();
        this.users = new HashMap<>();
        loadFromStorage();
    }

    /**
     * pulls users and tasks off disk and hooks each task back up to its
     * owner. called from the constructor and reload() so the cli can
     * refresh if the files change.
     */
    private void loadFromStorage() {
        users.clear();
        tasks.clear();

        for (User user : storage.loadUsers()) {
            users.put(user.getUsername().toLowerCase(), user);
        }
        for (Task task : storage.loadTasks()) {
            tasks.add(task);
            User owner = users.get(task.getOwnerUsername().toLowerCase());
            if (owner != null) {
                owner.addTask(task);
            }
        }
    }

    public void reload() {
        loadFromStorage();
    }

    // ---------------------------------------------------------------------
    // login / user management stuff (login + admin use cases)
    // ---------------------------------------------------------------------

    /**
     * uc-01: signs up a new regular user. returns null if the name's taken.
     */
    public User registerUser(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty()) return null;
        if (plainPassword == null || plainPassword.isEmpty()) return null;
        String key = username.trim().toLowerCase();
        if (users.containsKey(key)) {
            return null;
        }
        User user = User.register(username.trim(), plainPassword);
        users.put(key, user);
        storage.saveUsers(getAllUsers());
        return user;
    }

    /**
     * makes sure there's at least one admin around so a fresh install
     * is actually usable.
     */
    public Admin ensureDefaultAdmin(String username, String plainPassword) {
        String key = username.trim().toLowerCase();
        User existing = users.get(key);
        if (existing instanceof Admin) {
            return (Admin) existing;
        }
        if (existing != null) {
            return null;
        }
        Admin admin = Admin.registerAdmin(username, plainPassword);
        users.put(key, admin);
        storage.saveUsers(getAllUsers());
        return admin;
    }

    /**
     * uc-02: checks if a username/password combo is valid. matches the
     * login sequence diagram - the cli asks us, we ask the user object
     * to actually verify the password.
     */
    public User authenticate(String username, String plainPassword) {
        if (username == null || plainPassword == null) return null;
        User user = users.get(username.trim().toLowerCase());
        if (user == null) return null;
        return user.authenticate(plainPassword) ? user : null;
    }

    public User findUser(String username) {
        if (username == null) return null;
        return users.get(username.trim().toLowerCase());
    }

    public List<User> getAllUsers() {
        List<User> sorted = new ArrayList<>(users.values());
        sorted.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    /**
     * admin only (uc-11): kills a user account and all their tasks.
     */
    public boolean deleteUser(String username) {
        if (username == null) return false;
        String key = username.trim().toLowerCase();
        User removed = users.remove(key);
        if (removed == null) {
            return false;
        }
        tasks.removeIf(t -> t.getOwnerUsername().equalsIgnoreCase(username));
        storage.saveUsers(getAllUsers());
        storage.saveTasks(tasks);
        return true;
    }

    /**
     * admin only (uc-11): change someone else's password for them.
     */
    public boolean resetPassword(String username, String newPlainPassword) {
        if (username == null || newPlainPassword == null) return false;
        User user = users.get(username.trim().toLowerCase());
        if (user == null) return false;
        user.setPasswordHash(User.hashPassword(newPlainPassword));
        storage.saveUsers(getAllUsers());
        return true;
    }

    // ---------------------------------------------------------------------
    // task crud (uc-04, uc-05, uc-06, uc-07)
    // ---------------------------------------------------------------------

    /**
     * uc-04: makes a new task for someone, saves it right away and adds
     * it to that user's task list.
     */
    public Task createTask(User owner,
                           String title,
                           String description,
                           Category category,
                           LocalDate dueDate,
                           Priority priority,
                           TaskStatus status) {
        if (owner == null) {
            throw new IllegalArgumentException("Task must have an owner");
        }
        Task task = new Task(
                UUID.randomUUID().toString().substring(0, 8),
                title,
                description,
                category,
                dueDate,
                priority,
                status,
                owner.getUsername());
        tasks.add(task);
        owner.addTask(task);
        storage.saveTasks(tasks);
        return task;
    }

    /**
     * uc-05: edit an existing task. pass null for any field you don't
     * want to touch. returns false if the id doesn't exist.
     */
    public boolean editTask(String taskId,
                            String title,
                            String description,
                            Category category,
                            LocalDate dueDate,
                            Priority priority,
                            TaskStatus status) {
        Task task = findTask(taskId);
        if (task == null) return false;
        if (title != null && !title.trim().isEmpty()) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (category != null) task.setCategory(category);
        if (dueDate != null) task.setDueDate(dueDate);
        if (priority != null) task.setPriority(priority);
        if (status != null) task.setStatus(status);
        storage.saveTasks(tasks);
        return true;
    }

    /**
     * uc-06: deletes a task for good - removes it from both the main list
     * and the owner's list.
     */
    public boolean deleteTask(String taskId) {
        Task task = findTask(taskId);
        if (task == null) return false;
        tasks.remove(task);
        User owner = users.get(task.getOwnerUsername().toLowerCase());
        if (owner != null) {
            owner.removeTask(task);
        }
        storage.saveTasks(tasks);
        return true;
    }

    /**
     * uc-07: just an extension of uc-05. flips a task to complete on the
     * task object itself and saves.
     */
    public boolean markTaskComplete(String taskId) {
        Task task = findTask(taskId);
        if (task == null) return false;
        task.markComplete();
        storage.saveTasks(tasks);
        return true;
    }

    public Task findTask(String taskId) {
        if (taskId == null) return null;
        for (Task task : tasks) {
            if (task.getId().equalsIgnoreCase(taskId.trim())) {
                return task;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // dashboard, search, filter (uc-03, uc-08, uc-09)
    // ---------------------------------------------------------------------

    public List<Task> getTasksFor(String username) {
        if (username == null) return new ArrayList<>();
        return tasks.stream()
                .filter(t -> t.getOwnerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public List<Task> getActiveTasksFor(String username) {
        return getTasksFor(username).stream()
                .filter(t -> !t.isComplete())
                .collect(Collectors.toList());
    }

    public List<Task> getCompletedTasksFor(String username) {
        return getTasksFor(username).stream()
                .filter(Task::isComplete)
                .collect(Collectors.toList());
    }

    public List<Task> getOverdueTasksFor(String username) {
        return getTasksFor(username).stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toList());
    }

    /**
     * uc-08: keyword search, limited to one user's tasks.
     */
    public List<Task> searchTasks(String username, String keyword) {
        return getTasksFor(username).stream()
                .filter(t -> t.matchesKeyword(keyword))
                .collect(Collectors.toList());
    }

    /**
     * uc-09: filter a user's tasks by category, priority, status, or
     * due-date range. pass null for anything you don't want to filter on.
     */
    public List<Task> filterTasks(String username,
                                  Category category,
                                  Priority priority,
                                  TaskStatus status,
                                  LocalDate dueFrom,
                                  LocalDate dueTo) {
        return getTasksFor(username).stream()
                .filter(t -> category == null || t.getCategory().equals(category))
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> dueFrom == null || (t.getDueDate() != null && !t.getDueDate().isBefore(dueFrom)))
                .filter(t -> dueTo == null || (t.getDueDate() != null && !t.getDueDate().isAfter(dueTo)))
                .collect(Collectors.toList());
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    // ---------------------------------------------------------------------
    // admin stats (uc-12)
    // ---------------------------------------------------------------------

    public SystemStats computeSystemStats() {
        int totalUsers = users.size();
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream().filter(Task::isComplete).count();
        int overdueTasks = (int) tasks.stream().filter(Task::isOverdue).count();
        int activeTasks = totalTasks - completedTasks;
        return new SystemStats(totalUsers, totalTasks, activeTasks, completedTasks, overdueTasks);
    }

    /**
     * just a little immutable bundle of numbers returned by computeSystemStats().
     * stuck in here as a nested class so we don't have to add another top-level
     * file that isn't in the design doc.
     */
    public static final class SystemStats {
        private final int totalUsers;
        private final int totalTasks;
        private final int activeTasks;
        private final int completedTasks;
        private final int overdueTasks;

        public SystemStats(int totalUsers, int totalTasks, int activeTasks,
                           int completedTasks, int overdueTasks) {
            this.totalUsers = totalUsers;
            this.totalTasks = totalTasks;
            this.activeTasks = activeTasks;
            this.completedTasks = completedTasks;
            this.overdueTasks = overdueTasks;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getTotalTasks() { return totalTasks; }
        public int getActiveTasks() { return activeTasks; }
        public int getCompletedTasks() { return completedTasks; }
        public int getOverdueTasks() { return overdueTasks; }

        @Override
        public String toString() {
            return String.format(
                    "Users: %d | Tasks: %d (active: %d, completed: %d, overdue: %d)",
                    totalUsers, totalTasks, activeTasks, completedTasks, overdueTasks);
        }
    }
}
