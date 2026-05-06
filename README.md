# Task Management System

A Java-based CLI task management system built.

## Authors

- Aaqila Patel (amp10098)
- Isha Gopal (ig2324)

## Requirements

- Java JDK 17 or higher

## Build and Run

From the project root:

```bash
javac -d out src/*.java
java -cp out Main
```

A `data/` directory is created on first run to persist users and tasks.

## Default Admin Account

A default admin account is seeded on first run:

- Username: `admin`
- Password: `admin123`

## Features

- Register, log in, and log out
- Dashboard summarizing active, completed, and overdue tasks
- Create, edit, delete, and mark tasks complete
- Search tasks by keyword and filter by category, priority, status, or due date
- Admin-only user management and system-wide statistics
