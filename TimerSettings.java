import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TimerSettings {

  static final Path FILE = Path.of("timer.properties");
  static final String KEY_MINUTES = "duration.minutes";
  static final int DEFAULT_MINUTES = 25;

  private TimerSettings() {}

  public static int loadMinutes() {
    if (!Files.exists(FILE)) return DEFAULT_MINUTES;
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(FILE)) {
      p.load(in);
      int minutes = Integer.parseInt(p.getProperty(KEY_MINUTES, "25").trim());
      return Math.max(1, Math.min(180, minutes));
    } catch (Exception ex) {
      return DEFAULT_MINUTES;
    }
  }

  public static void saveMinutes(int minutes) {
    Properties p = new Properties();
    p.setProperty(
      KEY_MINUTES,
      Integer.toString(Math.max(1, Math.min(180, minutes)))
    );
    try (OutputStream out = Files.newOutputStream(FILE)) {
      p.store(out, "Habit Tracker timer");
    } catch (IOException ignored) {}
  }
}
