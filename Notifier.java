import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

public final class Notifier {

  private static TrayIcon tray;

  private Notifier() {}

  public static void init() {
    if (tray != null || GraphicsEnvironment.isHeadless()) return;
    if (!SystemTray.isSupported()) return;

    BufferedImage image = new BufferedImage(
      16,
      16,
      BufferedImage.TYPE_INT_ARGB
    );
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON
    );
    g.setColor(new Color(0xF5A623));
    g.fillOval(2, 2, 12, 12);
    g.dispose();

    tray = new TrayIcon(image, "Habit Tracker");
    tray.setImageAutoSize(true);
    try {
      SystemTray.getSystemTray().add(tray);
    } catch (Exception ex) {
      tray = null;
    }
  }

  public static void notify(String title, String message) {
    if (tray != null) {
      tray.displayMessage(title, message, TrayIcon.MessageType.INFO);
    } else {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void shutdown() {
    if (tray == null) return;
    SystemTray.getSystemTray().remove(tray);
    tray = null;
  }
}
