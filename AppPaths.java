import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class AppPaths {

  private static final Path DIR = resolveDir();

  private AppPaths() {}

  public static Path dir() {
    return DIR;
  }

  public static Path file(String name) {
    return DIR.resolve(name);
  }

  public static void init() {
    try {
      Files.createDirectories(DIR);
      migrate();
    } catch (IOException ignored) {}
  }

  private static Path resolveDir() {
    String os = System.getProperty("os.name", "").toLowerCase();
    String home = System.getProperty("user.home", ".");

    if (os.contains("win")) {
      String appData = System.getenv("APPDATA");
      if (appData != null && !appData.isBlank())
        return Path.of(appData, "HabitTracker");
      return Path.of(home, "AppData", "Roaming", "HabitTracker");
    }
    if (os.contains("mac"))
      return Path.of(home, "Library", "Application Support", "HabitTracker");

    String xdg = System.getenv("XDG_DATA_HOME");
    if (xdg != null && !xdg.isBlank())
      return Path.of(xdg, "habit-tracker");
    return Path.of(home, ".local", "share", "habit-tracker");
  }

  private static void migrate() throws IOException {
    Path cwd = Path.of(".");
    if (DIR.toAbsolutePath().normalize().equals(cwd.toAbsolutePath().normalize()))
      return;

    for (String name : List.of(
      "habits.csv",
      "settings.properties",
      "timer.properties",
      "focus-log.csv"
    )) {
      Path from = cwd.resolve(name);
      Path to = DIR.resolve(name);
      if (Files.exists(from) && !Files.exists(to))
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    Path fromJournal = cwd.resolve("journal");
    Path toJournal = DIR.resolve("journal");
    if (Files.isDirectory(fromJournal) && !Files.exists(toJournal)) {
      Files.createDirectories(toJournal);
      try (var files = Files.list(fromJournal)) {
        for (Path file : files.toList())
          if (Files.isRegularFile(file))
            Files.copy(
              file,
              toJournal.resolve(file.getFileName()),
              StandardCopyOption.REPLACE_EXISTING
            );
      }
    }
  }
}
