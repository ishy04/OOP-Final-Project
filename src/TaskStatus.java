/**
 * what state a task is in.
 *
 * we use an enum so we get a fixed, type-safe list of states
 * (todo, in_progress, complete) instead of random strings.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETE;

    /**
     * forgiving parser for when the user types a status into the cli.
     * if we can't figure it out, we just default to todo.
     */
    public static TaskStatus fromString(String value) {
        if (value == null) {
            return TODO;
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_');
        try {
            return TaskStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return TODO;
        }
    }
}
