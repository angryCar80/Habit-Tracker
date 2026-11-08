import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

public final class ThemeManager {

  public record ThemeOption(String name, String className, boolean dark) {
    @Override
    public String toString() {
      return name;
    }
  }

  public static final String DEFAULT_CLASS =
    "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme";
  public static final String DEFAULT_NAME = "Material Darker (Material)";

  private static final Path SETTINGS = Path.of("settings.properties");
  private static final String KEY_NAME = "theme.name";
  private static final String KEY_CLASS = "theme.class";
  private static final String KEY_DARK = "theme.dark";

  private static List<ThemeOption> themes;
  private static ThemeOption current = defaultTheme();

  private ThemeManager() {}

  public static ThemeOption defaultTheme() {
    return new ThemeOption(DEFAULT_NAME, DEFAULT_CLASS, true);
  }

  public static List<ThemeOption> themes() {
    if (themes == null) {
      List<ThemeOption> list = new ArrayList<>();
      list.add(
        new ThemeOption("Flat Light", FlatLightLaf.class.getName(), false)
      );
      list.add(new ThemeOption("Flat Dark", FlatDarkLaf.class.getName(), true));
      list.add(
        new ThemeOption(
          "IntelliJ Light",
          FlatIntelliJLaf.class.getName(),
          false
        )
      );
      list.add(
        new ThemeOption("Darcula", FlatDarculaLaf.class.getName(), true)
      );
      for (FlatAllIJThemes.FlatIJLookAndFeelInfo info : FlatAllIJThemes.INFOS)
        list.add(
          new ThemeOption(info.getName(), info.getClassName(), info.isDark())
        );
      themes = Collections.unmodifiableList(list);
    }
    return themes;
  }

  public static ThemeOption current() {
    return current;
  }

  public static ThemeOption byClassName(String className) {
    for (ThemeOption t : themes())
      if (t.className().equals(className)) return t;
    return defaultTheme();
  }

  public static ThemeOption load() {
    ThemeOption fallback = defaultTheme();
    if (!Files.exists(SETTINGS)) {
      current = fallback;
      return fallback;
    }
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(SETTINGS)) {
      p.load(in);
      String className = p.getProperty(KEY_CLASS);
      current =
        className == null || className.isBlank()
          ? fallback
          : byClassName(className);
    } catch (IOException ex) {
      current = fallback;
    }
    return current;
  }

  public static void save(ThemeOption t) {
    Properties p = new Properties();
    p.setProperty(KEY_NAME, t.name());
    p.setProperty(KEY_CLASS, t.className());
    p.setProperty(KEY_DARK, Boolean.toString(t.dark()));
    try (OutputStream out = Files.newOutputStream(SETTINGS)) {
      p.store(out, "Habit Tracker settings");
    } catch (IOException ignored) {
      // keep running with the in-memory selection
    }
    current = t;
  }

  public static boolean apply(ThemeOption t) {
    try {
      LookAndFeel laf = (LookAndFeel) Class.forName(t.className())
        .getDeclaredConstructor()
        .newInstance();
      FlatLaf.setup(laf);
      applyUiDefaults();
      FlatLaf.updateUI();
      current = t;
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  public static void applyUiDefaults() {
    UIManager.put("Component.arc", 12);
    UIManager.put("Button.arc", 12);
    UIManager.put("TextComponent.arc", 10);
    UIManager.put("Component.focusWidth", 0);
    UIManager.put("ScrollBar.thumbArc", 12);
    UIManager.put("ScrollBar.width", 10);
    UIManager.put("defaultFont", new Font("Fira Sans", Font.PLAIN, 14));
  }
}
