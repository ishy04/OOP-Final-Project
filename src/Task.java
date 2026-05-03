import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * one to-do item that belongs to a user.
 *
 * all the fields are private with getters/setters. each task has one
 * category, and taskmanager is the thing that creates/deletes them.
 */
public class Task {
    private String id;
    private String title;
    private String description;
    private Category category;
    private LocalDate dueDate;
    private Priority priority;
    private TaskStatus status;
    private String ownerUsername;

    public Task(String id,
                String title,
                String description,
                Category category,
                LocalDate dueDate,
                Priority priority,
                TaskStatus status,
                String ownerUsername) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Task id cannot be empty");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        if (ownerUsername == null || ownerUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Task must have an owner");
        }
        this.id = id;
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.category = category == null ? new Category("General") : category;
        this.dueDate = dueDate;
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.status = status == null ? TaskStatus.TODO : status;
        this.ownerUsername = ownerUsername.trim();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category == null ? new Category("General") : category;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority == null ? Priority.MEDIUM : priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status == null ? TaskStatus.TODO : status;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    /**
     * flips the task to complete. uc-07 is just an extension of uc-05
     * (edit task) in the design doc, so this is on purpose just a thin
     * wrapper around setstatus().
     */
    public void markComplete() {
        this.status = TaskStatus.COMPLETE;
    }

    /**
     * a task is overdue if it has a due date that's already passed and
     * it's not done yet.
     */
    public boolean isOverdue() {
        if (dueDate == null || status == TaskStatus.COMPLETE) {
            return false;
        }
        return dueDate.isBefore(LocalDate.now());
    }

    public boolean isComplete() {
        return status == TaskStatus.COMPLETE;
    }

    /**
     * true if the title or description contains the keyword (case doesn't
     * matter). used by taskmanager.searchtasks().
     */
    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String needle = keyword.toLowerCase();
        return title.toLowerCase().contains(needle)
                || description.toLowerCase().contains(needle);
    }

    /**
     * turns the task into one line of text for filestoragemanager.
     *
     * format: id|owner|title|description|category|duedate|priority|status
     * we escape any pipes or newlines in the user's text so the line
     * doesn't get mangled.
     */
    public String toStorageLine() {
        return String.join("|",
                escape(id),
                escape(ownerUsername),
                escape(title),
                escape(description),
                escape(category.getName()),
                dueDate == null ? "" : dueDate.toString(),
                priority.name(),
                status.name());
    }

    /**
     * the opposite of tostorageline(). returns null if the line's busted
     * so we can just skip it instead of bailing on the whole file.
     */
    public static Task fromStorageLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 8) {
            return null;
        }
        try {
            String id = unescape(parts[0]);
            String owner = unescape(parts[1]);
            String title = unescape(parts[2]);
            String description = unescape(parts[3]);
            Category category = new Category(unescape(parts[4]));
            LocalDate dueDate = parts[5].isEmpty() ? null : LocalDate.parse(parts[5]);
            Priority priority = Priority.fromString(parts[6]);
            TaskStatus status = TaskStatus.fromString(parts[7]);
            return new Task(id, title, description, category, dueDate, priority, status, owner);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", "\\n")
                .replace("\r", "");
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
                    case 'n': sb.append('\n'); break;
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
        if (!(other instanceof Task)) return false;
        Task that = (Task) other;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String due = dueDate == null ? "no due date" : dueDate.toString();
        String overdueTag = isOverdue() ? "  [OVERDUE]" : "";
        return String.format("[%s] %s | %s | due: %s | priority: %s | status: %s%s",
                id, title, category.getName(), due, priority, status, overdueTag);
    }
}
