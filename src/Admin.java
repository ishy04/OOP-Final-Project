import java.util.List;

/**
 * an admin user that can do extra stuff regular users can't.
 *
 * extends user so login/logout/dashboard code can just treat admins
 * like normal users. we override isAdmin() so the cli can check it
 * without doing instanceof everywhere.
 *
 * handles uc-11 (manage user accounts) and uc-12 (system stats).
 */
public class Admin extends User {

    public Admin(String username, String passwordHash) {
        super(username, passwordHash);
    }

    /**
     * shortcut for making a new admin, just like user.register but for admins.
     */
    public static Admin registerAdmin(String username, String plainPassword) {
        return new Admin(username, User.hashPassword(plainPassword));
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    @Override
    protected String role() {
        return "ADMIN";
    }

    /**
     * uc-11: nukes a user account and all their tasks. the actual work
     * happens in taskmanager since it owns the data.
     */
    public boolean deleteUser(TaskManager manager, String username) {
        if (manager == null || username == null) return false;
        if (username.equalsIgnoreCase(getUsername())) {
            return false;
        }
        return manager.deleteUser(username);
    }

    /**
     * uc-11: resets someone's password. handy if they forgot it.
     */
    public boolean resetUserPassword(TaskManager manager, String username, String newPlainPassword) {
        if (manager == null || username == null || newPlainPassword == null) return false;
        return manager.resetPassword(username, newPlainPassword);
    }

    /**
     * uc-11: gives back every user so the admin can see who's registered.
     */
    public List<User> listUsers(TaskManager manager) {
        return manager.getAllUsers();
    }

    /**
     * uc-12: grabs the numbers shown on the admin dashboard.
     */
    public TaskManager.SystemStats getSystemStats(TaskManager manager) {
        return manager.computeSystemStats();
    }
}