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
 * print a warning and return empty results instead of crashing - that's
 * what the design doc asked for.
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
}
