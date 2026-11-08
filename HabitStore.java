import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class HabitStore {

  static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final Path file;

  HabitStore(Path file) {
    this.file = file;
  }

  List<Habit> load() throws IOException {
    List<Habit> habits = new ArrayList<>();
    if (!Files.exists(file)) return habits;
    for (String line : Files.readAllLines(file)) {
      if (line.isBlank()) continue;
      String[] parts = line.split(",", 3);
      Habit h = new Habit(parts[0]);
      if (
        parts.length > 1 && !parts[1].isBlank()
      ) for (String d : parts[1].split(";"))
        if (!d.isBlank()) h.done.add(LocalDate.parse(d, DATE));
      if (parts.length > 2 && !parts[2].isBlank()) {
        try {
          h.reminder = LocalTime.parse(parts[2].trim(), TIME);
        } catch (Exception ignored) {}
      }
      habits.add(h);
    }
    return habits;
  }

  void save(List<Habit> habits) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (Habit h : habits) {
      StringJoiner dates = new StringJoiner(";");
      h.done
        .stream()
        .sorted()
        .forEach(d -> dates.add(d.format(DATE)));
      String reminder = h.reminder == null ? "" : h.reminder.format(TIME);

      sb.append(h.name)
        .append(',')
        .append(dates)
        .append(',')
        .append(reminder)
        .append(System.lineSeparator());
    }
    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
    Files.writeString(tmp, sb.toString());
    try {
      Files.move(
        tmp,
        file,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
      );
    } catch (IOException ex) {
      Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
