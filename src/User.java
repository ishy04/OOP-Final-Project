import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * a user of the system once they're logged in.
 *
 * everything's private. we never save passwords in plain text - just a
 * salted sha-256 hash (see hashpassword below). that's the "no plain-text
 * passwords on disk" rule from the design doc.
 *
 * a user keeps a list of their tasks, but taskmanager is the one that
 * actually owns and saves them. so tasks can outlive a single login session.
 */
public class User {
    private String username;
    private String passwordHash;
    private List<Task> tasks;

    public User(String username, String passwordHash) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        this.username = username.trim();
        this.passwordHash = passwordHash;
        this.tasks = new ArrayList<>();
    }

    /**
     * shortcut for making a new user - hashes the password for you.
     * used when someone signs up.
     */
    public static User register(String username, String plainPassword) {
        return new User(username, hashPassword(plainPassword));
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * swaps in a new password hash. you have to hash it first with
     * hashpassword() yourself - we don't accept raw passwords here.
     */
    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        this.passwordHash = passwordHash;
    }

    /**
     * checks if a plain password matches what we have on file. used during
     * login (see the login sequence diagram).
     */
    public boolean authenticate(String plainPassword) {
        if (plainPassword == null) return false;
        return passwordHash.equals(hashPassword(plainPassword));
    }

    /**
     * gives back this user's tasks. don't change the list directly - use
     * addtask() / removetask() so things stay in sync.
     */
    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        if (task != null && !tasks.contains(task)) {
            tasks.add(task);
        }
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void clearTasks() {
        tasks.clear();
    }

    /**
     * the admin subclass overrides this to return true. having it as a
     * method means shared code (login, logout, dashboard) doesn't have
     * to care which kind of user it's dealing with.
     */
    public boolean isAdmin() {
        return false;
    }

    /**
     * sha-256 hash of the password with a fixed app-wide salt. nothing
     * fancy and no extra libraries, but it's enough to keep us from
     * storing passwords in plain text.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = "TMS::" + plainPassword;
            byte[] bytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available on this JVM", ex);
        }
    }

    /**
     * one-line format used by filestoragemanager:
     *   role|username|passwordhash
     * subclasses pick their own role string.
     */
    public String toStorageLine() {
        return String.join("|", role(), escape(username), passwordHash);
    }

    public static User fromStorageLine(String line) {
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) return null;
        String role = parts[0];
        String username = unescape(parts[1]);
        String hash = parts[2];
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(username, hash);
        }
        return new User(username, hash);
    }

    protected String role() {
        return "USER";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("|", "\\p");
    }

    private static String unescape(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'p': sb.append('|'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(next); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof User)) return false;
        User that = (User) other;
        return username.equalsIgnoreCase(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase());
    }

    @Override
    public String toString() {
        return username + (isAdmin() ? " (admin)" : "");
    }
}
