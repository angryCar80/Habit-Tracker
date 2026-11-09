import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class Journal {

  static final Path DIR = Path.of("journal");
  static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  static String read(final LocalDate d) throws IOException {
    final Path file = file(d);
    if (!Files.exists(file)) return "";
    return Files.readString(file);
  }

  static void write(final LocalDate d, final String text) throws IOException {
    final Path file = file(d);

    if (text == null || text.isBlank()) {
      Files.deleteIfExists(file);
      return;
    }
    Files.createDirectories(DIR);
    final Path tmp = DIR.resolve(file.getFileName() + ".temp");
    Files.writeString(tmp, text);
    try {
      Files.move(
        tmp,
        file,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
      );
    } catch (final IOException ex) {
      Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  static List<LocalDate> entryDates() throws IOException {
    if (!Files.isDirectory(DIR)) return List.of();
    final List<LocalDate> dates = new ArrayList<>();
    try (Stream<Path> files = Files.list(DIR)) {
      files
        .map(p -> p.getFileName().toString())
        .filter(n -> n.endsWith(".txt"))
        .map(n -> n.substring(0, n.length() - 4))
        .forEach(n -> {
          try {
            dates.add(LocalDate.parse(n, DATE));
          } catch (final Exception ignored) {}
        });
    }
    dates.sort(Comparator.reverseOrder());
    return dates;
  }

  private static Path file(final LocalDate d) {
    return DIR.resolve(d.format(DATE) + ".txt");
  }

  private Journal() {}
}
