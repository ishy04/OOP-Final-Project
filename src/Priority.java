/**
 * the priority levels a task can have.
 *
 * an enum instead of strings or numbers, so we always get type-safe
 * values like the design doc asked for.
 */
public enum Priority {
    HIGH,
    MEDIUM,
    LOW;

    /**
     * turns a string into a priority. doesn't care about casing or
     * extra whitespace. if it's null or weird we just give back medium
     * so the cli has a sane default and doesn't crash.
     */
    public static Priority fromString(String value) {
        if (value == null) {
            return MEDIUM;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}
