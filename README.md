# Habit Tracker in Java

A small desktop habit tracker to help you stay disciplined and see how you are
doing. Built with Java Swing and FlatLaf.

## Features

- Track habits with a 7-day toggle row, current streak, and best streak.
- Stats page with summary cards, 8-week completion charts, and 28-day completion.
- Daily journal with date navigation, autosave, and `Ctrl+S`.
- Focus timer with presets, custom duration, pause/resume, and session logging.
- Settings with a large FlatLaf/IntelliJ theme picker and live preview.
- Hidden habits, revealed with slash commands.
- Pin one habit to a **Focus** section until you complete it.
- Data is stored as CSV in your user data folder — no database required.

## Requirements

- JDK 17 or newer (tested with JDK 21).
- The FlatLaf jars in `lib/` (already included).
- For `.deb` packaging only: `dpkg-dev` and `fakeroot`.

## Build and Run

```bash
make          # compile and launch the app
make build    # compile only
make run      # same as make
make help     # list all targets
make clean    # remove bin/, build/, and dist/
```

Compilation uses `--release 17`. To build for a newer JDK:

```bash
make JAVA_RELEASE=21 build
```

## Runnable Jar

```bash
make jar
java -jar dist/HabitTracker.jar
```

This creates:

```text
dist/HabitTracker.jar
dist/lib/flatlaf-3.8.jar
dist/lib/flatlaf-intellij-themes-3.8.jar
```

Running the jar requires Java 17+.

## Desktop Packaging

Build a runnable app image with its own Java runtime:

```bash
make package
```

The output is under `build/installers/HabitTracker/`. On Linux, launch it with:

```bash
./build/installers/HabitTracker/bin/HabitTracker
```

Build a Debian package:

```bash
sudo apt install dpkg-dev fakeroot
make package-deb
```

`jpackage` only builds for the host operating system. Windows and macOS
installers need to be built on those systems.

## Slash Commands

Type these in the habit input field. Normal text still adds a new habit.

| Command | Action |
| --- | --- |
| `/help` | Show the command list |
| `/hidden` | Show or hide the Hidden section |
| `/hide <name>` | Hide a habit |
| `/show <name>` | Unhide a habit |
| `/pin <name>` | Pin one habit to Focus until it is completed |
| `/unpin` | Remove the current pinned habit |

Habits also have **Pin**, **Unpin**, and **Show** buttons in the list.

## Data Location

Data is stored in the OS user data folder:

- Linux: `~/.local/share/habit-tracker` (or `$XDG_DATA_HOME/habit-tracker`)
- macOS: `~/Library/Application Support/HabitTracker`
- Windows: `%APPDATA%\HabitTracker`

Files include `habits.csv`, `settings.properties`, `timer.properties`,
`focus-log.csv`, and the `journal/` folder. If old files are present in the
project folder, they are copied to the user data folder on first run.

## Roadmap

- [x] Settings and themes
- [x] Journal / notes
- [x] Statistics page
- [x] Focus timer and session log
- [x] Hidden habits
- [x] Pinned focus habit
- [x] Runnable jar and `jpackage` desktop packaging
- [ ] Groups that can hold multiple habits
- [ ] Cross-platform installers / CI releases
- [ ] Move from CSV to SQL (not planned for now)

## License

MIT License - see [LICENSE](LICENSE) for details.
