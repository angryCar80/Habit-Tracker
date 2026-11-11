import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

public class FocusTimer {

  public enum State {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
  }

  public interface Listener {
    void timerChanged();
  }

  public static String formatTime(int seconds) {
    int total = Math.max(0, seconds);
    int h = total / 3600;
    int m = (total % 3600) / 60;
    int s = total % 60;
    if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
    return String.format("%02d:%02d", m, s);
  }

  private final Timer ticker = new Timer(1000, e -> tick());
  private final List<Listener> listeners = new ArrayList<>();
  private State state = State.IDLE;
  private long durationMillis;
  private long remainingMillis;

  private long deadline;

  public FocusTimer() {
    ticker.setRepeats(true);
    durationMillis = TimerSettings.loadMinutes() * 60_000L;
    remainingMillis = durationMillis;
  }

  public State state() {
    return state;
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public int durationMinutes() {
    return (int) Math.ceil(durationMillis / 60_000.0);
  }

  public int remainingSeconds() {
    return (int) Math.ceil(remainingMillis / 1000.0);
  }

  public void setDuration(int minutes) {
    int m = clamp(minutes);
    ticker.stop();
    durationMillis = m * 60_000L;
    remainingMillis = durationMillis;
    state = State.IDLE;
    fireChanged();
  }

  public void start(int minutes) {
    int m = clamp(minutes);
    TimerSettings.saveMinutes(m);
    ticker.stop();
    durationMillis = m * 60_000L;
    remainingMillis = durationMillis;
    deadline = System.currentTimeMillis() + remainingMillis;
    state = State.RUNNING;
    ticker.start();
    fireChanged();
  }

  private int clamp(int minutes) {
    return Math.max(1, Math.min(180, minutes));
  }

  public void pause() {
    if (state != State.RUNNING) return;
    remainingMillis = Math.max(0, deadline - System.currentTimeMillis());
    ticker.stop();
    state = State.PAUSED;
    fireChanged();
  }

  public void resume() {
    if (state != State.PAUSED) return;
    deadline = System.currentTimeMillis() + remainingMillis;
    state = State.RUNNING;
    ticker.start();
    fireChanged();
  }

  public void reset() {
    ticker.stop();
    remainingMillis = durationMillis;
    state = State.IDLE;
    fireChanged();
  }

  public void shutdown() {
    ticker.stop();
  }

  private void tick() {
    long left = deadline - System.currentTimeMillis();
    if (left <= 0) {
      finish();
      return;
    }
    remainingMillis = left;
    fireChanged();
  }

  private void finish() {
    ticker.stop();
    remainingMillis = 0;
    state = State.FINISHED;
    int minutes = durationMinutes();
    try {
      SessionLog.append(minutes, LocalDateTime.now());
    } catch (Exception ignored) {}
    Notifier.notify("Focus complete", minutes + " min session logged");
    fireChanged();
  }

  private void fireChanged() {
    for (Listener listener : new ArrayList<>(listeners))
      listener.timerChanged();
  }
}
