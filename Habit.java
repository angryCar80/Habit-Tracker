import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Habit {

  String name;
  final Set<LocalDate> done = new HashSet<>();
  LocalTime reminder;
  LocalDate lastNotified;

  Habit(String name) {
    this.name = name;
  }

  boolean isDone(LocalDate d) {
    return done.contains(d);
  }

  void toggle(LocalDate d) {
    if (!done.remove(d)) done.add(d);
  }

  int streak(LocalDate today) {
    int s = 0;
    LocalDate d = done.contains(today) ? today : today.minusDays(1);
    while (done.contains(d)) {
      s++;
      d = d.minusDays(1);
    }
    return s;
  }

  int bestStreak() {
    if (done.isEmpty()) return 0;
    List<LocalDate> days = new ArrayList<>(done);
    Collections.sort(days);
    int best = 1,
      cur = 1;
    for (int i = 1; i < days.size(); i++) {
      cur = days.get(i).equals(days.get(i - 1).plusDays(1)) ? cur + 1 : 1;
      best = Math.max(best, cur);
    }
    return best;
  }

  int completionBetween(LocalDate from, LocalDate to) {
    int n = 0;
    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) if (
      done.contains(d)
    ) n++;
    return n;
  }
}
