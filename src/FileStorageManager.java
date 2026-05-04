import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * the part of the app that talks to disk.
 *
 * everyone else only sees four methods: saveusers/loadusers/savetasks/loadtasks.
 * the actual storage (one line per record, utf-8 text files in a data folder)
 * is hidden in here, so we could swap it out for a database later without
 * touching anything else.
 *
 * every i/o call is wrapped in try/catch. missing or messed up files just
 * print a warning and return empty results instead of crashing
 */
public class FileStorageManager {

    private static final String DEFAULT_DATA_DIR = "data";
    private static final String USERS_FILE = "users.txt";
    private static final String TASKS_FILE = "tasks.txt";

    private final Path dataDir;
    private final Path usersFile;
    private final Path tasksFile;

    public FileStorageManager() {
        this(DEFAULT_DATA_DIR);
    }

    public FileStorageManager(String dataDir) {
        this.dataDir = Paths.get(dataDir == null ? DEFAULT_DATA_DIR : dataDir);
        this.usersFile = this.dataDir.resolve(USERS_FILE);
        this.tasksFile = this.dataDir.resolve(TASKS_FILE);
        ensureDataDirectory();
    }

    private void ensureDataDirectory() {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException ex) {
            System.err.println("[FileStorageManager] Could not create data directory '"
                    + dataDir + "': " + ex.getMessage());
        }
    }

    public Path getUsersFile() {
        return usersFile;
    }

    public Path getTasksFile() {
        return tasksFile;
    }

    /**
     * writes out all the users. we always rewrite the whole file so deletes
     * actually stick. each user serializes itself via toStorageLine() which
     * means admins get tagged with "admin" and load back correctly.
     */
    public void saveUsers(List<User> users) {
        if (users == null) return;
        try (BufferedWriter writer = Files.newBufferedWriter(
                usersFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (User user : users) {
                if (user == null) continue;
                writer.write(user.toStorageLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            System.err.println("[FileStorageManager] Failed to save users: " + ex.getMessage());
        }
    }

    /**
     * reads users back from disk. if the file's missing or broken we just
     * print a warning and hand back an empty list instead of blowing up.
     */
    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        if (!Files.exists(usersFile)) {
            return users;
        }
        try (BufferedReader reader = Files.newBufferedReader(usersFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                User user = User.fromStorageLine(line);
                if (user != null) {
                    users.add(user);
                } else {
                    System.err.println("[FileStorageManager] Skipping malformed user line.");
                }
            }
        } catch (IOException ex) {
            System.err.println("[FileStorageManager] Failed to load users: " + ex.getMessage());
        }
        return users;
    }

    /**
     * dumps every task to disk. we rewrite the whole file each time so what's
     * on disk always matches what taskmanager has in memory - that covers the
     * "save changes right away" requirement.
     */
    public void saveTasks(List<Task> tasks) {
        if (tasks == null) return;
        try (BufferedWriter writer = Files.newBufferedWriter(
                tasksFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (Task task : tasks) {
                if (task == null) continue;
                writer.write(task.toStorageLine());
                writer.newLine();
            }
        } catch (IOException ex) {
            System.err.println("[FileStorageManager] Failed to save tasks: " + ex.getMessage());
        }
    }

    /**
     * pulls every task back off disk. skips any junk lines so even a half-
     * corrupted file still gives you something usable.
     */
    public List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        if (!Files.exists(tasksFile)) {
            return tasks;
        }
        try (BufferedReader reader = Files.newBufferedReader(tasksFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Task task = Task.fromStorageLine(line);
                if (task != null) {
                    tasks.add(task);
                } else {
                    System.err.println("[FileStorageManager] Skipping malformed task line.");
                }
            }
        } catch (IOException ex) {
            System.err.println("[FileStorageManager] Failed to load tasks: " + ex.getMessage());
        }
        return tasks;
    }
}