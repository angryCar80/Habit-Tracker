import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SessionLog {

  static final Path FILE = Path.of("focus-log.csv");
  static final DateTimeFormatter STAMP = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public record Summary(
    int sessionsToday,
    int minutesToday,
    int sessionsTotal,
    int minutesTotal
  ) {}

  private SessionLog() {}

  public static void append(int minutes, LocalDateTime endedAt)
    throws IOException {
    String line =
      endedAt.format(STAMP) + "," + minutes + System.lineSeparator();
    Files.writeString(
      FILE,
      line,
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND );
  }
  public static Summary summary() {
    int sessionsToday = 0;
    int minutesToday = 0;
    int sessionsTotal = 0;
    int minutesTotal = 0;
    if (!Files.exists(FILE)) return new Summary(0, 0, 0, 0);

    LocalDate today = LocalDate.now();
    try {
      List<String> lines = Files.readAllLines(FILE);
      for (String line : lines) {
        if (line.isBlank()) continue;
        String[] parts = line.split(",", 2);
        if (parts.length < 2) continue;
        try {
          LocalDateTime when = LocalDateTime.parse(parts[0].trim(), STAMP);
          int minutes = Math.max(0, Integer.parseInt(parts[1].trim()));
          sessionsTotal++;
          minutesTotal += minutes;

          if (when.toLocalDate().equals(today)){
            sessionsToday++;
            minutesToday += minutes;
          }
        } catch (Exception ignored){}
      }
    } catch (IOException ex){
      return new Summary(0, 0, 0, 0);
    }
    return new Summary(sessionsToday, minutesToday, sessionsTotal, minutesTotal);
  }
  public static String formatMinutes(int minutes ){
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
  }
}
