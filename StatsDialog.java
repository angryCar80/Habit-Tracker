import com.formdev.flatlaf.FlatLaf;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

public class StatsDialog extends JDialog {

  private static final int WEEKS = 8;
  private final List<Habit> habits;
  private final LocalDate today = LocalDate.now();

  StatsDialog(Main owner) {
    super(owner, "Statistics", true);
    this.habits = owner.habits;

    JPanel root = new JPanel(new BorderLayout(12, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    setContentPane(root);

    JLabel heading = new JLabel("Statistics");
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
    root.add(heading, BorderLayout.NORTH);

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.add(summary());
    content.add(Box.createVerticalStrut(12));
    for (Habit h : habits) content.add(habitRow(h));
    if (habits.isEmpty()) {
      JLabel empty = new JLabel("No habits yet");
      empty.setForeground(Color.GRAY);
      empty.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));
      content.add(empty);
    }
    content.add(Box.createVerticalGlue());

    JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    root.add(scroll, BorderLayout.CENTER);

    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    footer.add(close);
    root.add(footer, BorderLayout.SOUTH);

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(620, 460));
    setSize(680, 560);
    setLocationRelativeTo(owner);
  }

  private JPanel summary() {
    int doneToday = 0;
    int total = 0;
    int bestNow = 0;
    for (Habit h : habits) {
      if (h.isDone(today)) doneToday++;
      total += h.done.size();
      bestNow = Math.max(bestNow, h.streak(today));
    }
    JPanel panel = new JPanel(new GridLayout(1, 4, 8, 0));
    panel.add(card("Habits", Integer.toString(habits.size())));
    panel.add(card("Done today", doneToday + "/" + habits.size()));
    panel.add(card("Completions", Integer.toString(total)));
    panel.add(card("Best streak now", Integer.toString(bestNow)));
    return panel;
  }

  private JPanel focusSummary() {
    SessionLog.Summary s = SessionLog.summary();
    JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
    panel.add(
      card(
        "Focus Today",
        s.sessionsToday() +
          " \u00b7 " +
          SessionLog.formatMinutes(s.minutesToday())
      )
    );

    panel.add(
      card(
        "Focus total",
        s.sessionsTotal() +
          "  \u00b7  " +
          SessionLog.formatMinutes(s.minutesTotal())
      )
    );
    return panel;
  }

  private JPanel card(String title, String value) {
    JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setBorder(
      BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(divider()),
        BorderFactory.createEmptyBorder(8, 10, 8, 10)
      )
    );
    JLabel t = new JLabel(title);
    t.setForeground(Color.GRAY);
    t.setFont(t.getFont().deriveFont(Font.PLAIN, 12f));
    JLabel v = new JLabel(value);
    v.setFont(v.getFont().deriveFont(Font.BOLD, 20f));
    panel.add(t, BorderLayout.NORTH);
    panel.add(v, BorderLayout.CENTER);
    return panel;
  }

  private JPanel habitRow(Habit h) {
    JLabel name = new JLabel(h.name);
    name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));

    int[] perWeek = new int[WEEKS];
    for (int w = 0; w < WEEKS; w++) {
      LocalDate end = today.minusDays((WEEKS - 1 - w) * 7L);
      perWeek[w] = h.completionBetween(end.minusDays(6), end);
    }
    int pct = Math.round(
      (h.completionBetween(today.minusDays(27), today) * 100f) / 28f
    );

    JLabel meta = new JLabel(
      pct +
        "%  \u00b7  now " +
        h.streak(today) +
        " \u00b7 best " +
        h.bestStreak() +
        "  \u00b7  " +
        h.done.size() +
        " total"
    );
    meta.setForeground(Color.GRAY);

    JPanel text = new JPanel(new BorderLayout(0, 2));
    text.add(name, BorderLayout.NORTH);
    text.add(meta, BorderLayout.SOUTH);

    JComponent chart = new WeekBars(perWeek);

    JPanel row = new JPanel(new BorderLayout(12, 0));
    row.setBorder(
      BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, divider()),
        BorderFactory.createEmptyBorder(10, 2, 10, 2)
      )
    );
    row.add(text, BorderLayout.WEST);
    row.add(chart, BorderLayout.CENTER);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
    return row;
  }

  private static Color divider() {
    Color c = UIManager.getColor("Component.borderColor");
    if (c != null) return c;
    return FlatLaf.isLafDark() ? new Color(0x3A3A3A) : new Color(0xD8D8D8);
  }

  private static Color accent() {
    Color c = UIManager.getColor("Component.accentColor");
    if (c != null) return c;
    return new Color(0x4A6FA5);
  }

  static class WeekBars extends JComponent {

    private final int[] values;

    WeekBars(int[] values) {
      this.values = values;
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(260, 44);
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
      );
      int w = getWidth();
      int gap = 4;
      int barW = Math.max(4, (w - gap * (values.length - 1)) / values.length);
      Color accent = accent();
      Color track = FlatLaf.isLafDark()
        ? new Color(0x2E2E2E)
        : new Color(0xE6E6E6);
      int baseY = getHeight() - 6;

      for (int i = 0; i < values.length; i++) {
        int x = i * (barW + gap);
        int full = Math.round((baseY - 4) * (values[i] / 7f));
        g2.setColor(track);
        g2.fillRoundRect(x, 4, barW, baseY - 4, 6, 6);
        g2.setColor(accent);
        g2.fillRoundRect(x, baseY - full, barW, full, 6, 6);
      }
      g2.setColor(divider());
      g2.drawLine(0, baseY, w, baseY);
      g2.dispose();
    }
  }
}
