import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

public class Main extends JFrame {

  static final Path DATA = Path.of("habits.csv");

  static final DateTimeFormatter DAY_LETTER = DateTimeFormatter.ofPattern(
    "EEEEE"
  );
  static final DateTimeFormatter DAY_TOOLTIP = DateTimeFormatter.ofPattern(
    "EEE, MMM d"
  );

  public static void main(String[] args) {
    ThemeManager.apply(ThemeManager.load());
    SwingUtilities.invokeLater(Main::new);
  }

  JournalDialog journalDialog;
  TimerDialog timerDialog;
  final FocusTimer focusTimer = new FocusTimer();
  final List<Habit> habits = new ArrayList<>();

  final HabitStore store = new HabitStore(DATA);
  final JPanel listPanel = new JPanel();
  final JTextField input = new JTextField();

  final JLabel status = new JLabel("Ready");
  final JLabel timerStatus = new JLabel();

  public Main() {
    super("Habit Tracker");
    Notifier.init();
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    JLabel title = new JLabel("Habit Tracker");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
    JLabel date = new JLabel(
      LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy"))
    );

    input.setCaretColor(new Color(0xE6E6E6));
    input.setSelectionColor(new Color(0x4A6FA5));
    input.setSelectedTextColor(Color.WHITE);

    JButton add = new JButton("Add Habit");
    JPanel inputRow = new JPanel(new BorderLayout(6, 0));
    inputRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

    inputRow.add(input, BorderLayout.CENTER);
    inputRow.add(add, BorderLayout.EAST);

    JButton stats = new JButton("Stats");
    stats.addActionListener(e -> new StatsDialog(this).setVisible(true));

    JButton journal = new JButton("Journal");
    journal.addActionListener(e -> {
      if (journalDialog == null || !journalDialog.isDisplayable()) {
        journalDialog = new JournalDialog(this);
      }
      journalDialog.setVisible(true);
    });

    JButton timer = new JButton("Timer");
    timer.addActionListener(e -> {
      if (timerDialog == null) timerDialog = new TimerDialog(this, focusTimer);
      timerDialog.setVisible(true);
    });

    JButton settings = new JButton("Settings");
    settings.addActionListener(e -> new SettingsDialog(this).setVisible(true));

    JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
    titleBlock.add(title, BorderLayout.NORTH);
    titleBlock.add(date, BorderLayout.SOUTH);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    actions.add(stats);
    actions.add(journal);
    actions.add(timer);
    actions.add(settings);

    JPanel topRow = new JPanel(new BorderLayout(12, 0));
    topRow.add(titleBlock, BorderLayout.WEST);
    topRow.add(actions, BorderLayout.EAST);

    JPanel header = new JPanel(new BorderLayout());
    header.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
    header.add(topRow, BorderLayout.NORTH);
    header.add(inputRow, BorderLayout.SOUTH);
    add(header, BorderLayout.NORTH);

    date.setForeground(Color.GRAY);
    date.setFont(date.getFont().deriveFont(Font.PLAIN, 12f));

    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

    JScrollPane scroll = new JScrollPane(listPanel);
    scroll.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
    add(scroll, BorderLayout.CENTER);

    JPanel statusBar = new JPanel(new BorderLayout(8, 0));
    statusBar.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    timerStatus.setForeground(Color.GRAY);
    statusBar.add(status, BorderLayout.WEST);
    statusBar.add(timerStatus, BorderLayout.EAST);
    add(statusBar, BorderLayout.SOUTH);

    focusTimer.addListener(this::updateTimerStatus);
    updateTimerStatus();

    add.addActionListener(e -> addHabit());
    input.addActionListener(e -> addHabit());

    load();
    rebuild();
    if (!status.getText().startsWith("Could not")) status.setText(
      habits.size() + " habits loaded"
    );

    setMinimumSize(new Dimension(560, 420));
    setSize(720, 520);
    setLocationRelativeTo(null);
    setVisible(true);
  }

  void updateTimerStatus() {
    FocusTimer.State state = focusTimer.state();
    if (state == FocusTimer.State.RUNNING || state == FocusTimer.State.PAUSED) {
      timerStatus.setText(
        "Focus " + FocusTimer.formatTime(focusTimer.remainingSeconds())
      );
    } else if (state == FocusTimer.State.FINISHED) {
      timerStatus.setText("Focus done");
    } else {
      timerStatus.setText("");
    }
  }

  void addHabit() {
    String name = input.getText().trim().replace(",", " ").replace(";", " ");
    if (name.isEmpty()) {
      status.setText("Enter a habit name");
      return;
    }
    for (Habit h : habits)
      if (h.name.equalsIgnoreCase(name)) {
        status.setText("Already exists");
        return;
      }
    habits.add(new Habit(name));
    input.setText("");
    rebuild();
    save("Added " + name);
  }

  void renameHabit(Habit h) {
    JTextField field = new JTextField(h.name);
    field.setCaretColor(new Color(0xE6E6E6));
    field.setSelectionColor(new Color(0x4A6FA5));
    field.setSelectedTextColor(Color.WHITE);
    field.selectAll();
    SwingUtilities.invokeLater(() -> {
      field.requestFocusInWindow();
      field.selectAll();
    });

    Object[] options = { "Rename", "Cancel" };
    int result = JOptionPane.showOptionDialog(
      this,
      field,
      "Edit Habit",
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.PLAIN_MESSAGE,
      null,
      options,
      options[0]
    );
    if (result != 0) return;

    String name = field.getText();
    name = name.trim().replace(",", " ").replace(";", " ");
    if (name.isEmpty()) {
      status.setText("Name cannot be empty");
      return;
    }
    for (Habit other : habits)
      if (other != h && other.name.equalsIgnoreCase(name)) {
        status.setText("Already exists");
        return;
      }

    h.name = name;
    rebuild();
    save("Renamed to " + name);
  }

  void rebuild() {
    listPanel.removeAll();
    LocalDate today = LocalDate.now();

    for (Habit h : habits) {
      JLabel nameLabel = new JLabel(h.name);
      nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 15f));

      JPanel week = new JPanel(new GridLayout(1, 7, 4, 0));
      for (int i = 6; i >= 0; i--) {
        LocalDate d = today.minusDays(i);
        JToggleButton b = new JToggleButton(d.format(DAY_LETTER));
        b.setMargin(new Insets(2, 2, 2, 2));
        b.setPreferredSize(new Dimension(30, 28));
        b.setSelected(h.isDone(d));
        b.setToolTipText(d.format(DAY_TOOLTIP));
        if (d.equals(today)) b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.addActionListener(e -> {
          h.toggle(d);
          save("Updated " + h.name);
          rebuild();
        });
        week.add(b);
      }
      JPanel left = new JPanel(new BorderLayout(0, 4));
      left.add(nameLabel, BorderLayout.NORTH);
      left.add(week, BorderLayout.CENTER);
      int now = h.streak(today);

      JLabel streak = new JLabel(
        "now " + now + "\u00b7 best " + h.bestStreak()
      );
      streak.setForeground(now > 0 ? new Color(0xF5A623) : new Color(0x7A7D85));
      JButton edit = new JButton("Edit");
      edit.addActionListener(e -> renameHabit(h));

      JButton del = new JButton("Delete");
      del.addActionListener(e -> {
        int ok = JOptionPane.showConfirmDialog(
          this,
          "Delete \"" + h.name + "\"?",
          "Confirm",
          JOptionPane.OK_CANCEL_OPTION
        );
        if (ok == JOptionPane.OK_OPTION) {
          habits.remove(h);
          rebuild();
          save("Deleted " + h.name);
        }
      });

      JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
      right.add(streak);
      right.add(edit);
      right.add(del);

      JPanel row = new JPanel(new BorderLayout(8, 0));
      row.setBorder(
        BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3A3A3A)),
          BorderFactory.createEmptyBorder(8, 4, 8, 4)
        )
      );
      row.add(left, BorderLayout.CENTER);
      row.add(right, BorderLayout.EAST);
      row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
      listPanel.add(row);
    }

    if (habits.isEmpty()) {
      JLabel empty = new JLabel("No habits yet \u2014 add one above");
      empty.setForeground(Color.GRAY);
      empty.setBorder(BorderFactory.createEmptyBorder(16, 4, 0, 0));
      listPanel.add(empty);
    }

    listPanel.add(Box.createVerticalGlue());
    listPanel.revalidate();
    listPanel.repaint();
  }

  void load() {
    habits.clear();
    try {
      habits.addAll(store.load());
    } catch (Exception ex) {
      status.setText("Could not load: " + ex.getMessage());
    }
  }

  void save(String msg) {
    try {
      store.save(habits);
      status.setText(msg + "  \u2022  " + habits.size() + " habits");
    } catch (IOException ex) {
      status.setText("Save failed: " + ex.getMessage());
    }
  }
}
