import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Main extends JFrame {

  static final Path DATA = AppPaths.file("habits.csv");

  static final DateTimeFormatter DAY_LETTER = DateTimeFormatter.ofPattern(
    "EEEEE"
  );
  static final DateTimeFormatter DAY_TOOLTIP = DateTimeFormatter.ofPattern(
    "EEE, MMM d"
  );

  boolean showHidden;
  final java.util.Set<String> collapsedGroups = new java.util.HashSet<>();

  final JComboBox<String> groupBox = new JComboBox<>();

  public static void main(String[] args) {
    AppPaths.init();
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
    groupBox.setEditable(true);
    groupBox.setPreferredSize(new Dimension(150, 28));
    groupBox.setMaximumRowCount(12);
    groupBox.setToolTipText("Group for the new habit");
    JPanel inputActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    inputActions.add(groupBox);
    inputActions.add(add);
    JPanel inputRow = new JPanel(new BorderLayout(6, 0));
    inputRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

    inputRow.add(input, BorderLayout.CENTER);
    inputRow.add(inputActions, BorderLayout.EAST);

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

    load();
    refreshGroupBox();
    rebuild();
    if (!status.getText().startsWith("Could not")) status.setText(
      habits.size() + " habits loaded"
    );

    add.addActionListener(e -> submitInput());
    input.addActionListener(e -> submitInput());

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

  void editHabit(Habit h) {
    HabitDialog dialog = new HabitDialog(this, h, groupNames());
    if (!dialog.showDialog()) return;

    String name = dialog
      .habitName()
      .trim()
      .replace(",", " ")
      .replace(";", " ");
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
    h.group = canonicalGroup(dialog.groupName());
    h.hidden = dialog.hiddenSelected();
    refreshGroupBox();
    rebuild();
    save("Updated " + h.name);
  }

  void rebuild() {
    listPanel.removeAll();
    LocalDate today = LocalDate.now();

    boolean pinsChanged = false;
    for (Habit h : habits)
      if (h.pinned && h.isDone(today)) {
        h.pinned = false;
        pinsChanged = true;
      }
    if (pinsChanged) try {
      store.save(habits);
    } catch (IOException ignored) {}

    Habit pinned = null;
    for (Habit h : habits)
      if (h.pinned) {
        pinned = h;
        break;
      }

    if (pinned != null) {
      listPanel.add(sectionHeader("Focus", new Color(0x4A6FA5)));
      listPanel.add(habitRow(pinned, today));
    }

    int visible = 0;
    int hidden = 0;
    for (Habit h : habits) {
      if (h.pinned) continue;
      if (h.hidden) hidden++;
      else visible++;
    }

    Comparator<String> groupOrder = Comparator
      .comparing((String g) -> g.isBlank())
      .thenComparing(String.CASE_INSENSITIVE_ORDER);
    Map<String, List<Habit>> groups = new TreeMap<>(groupOrder);
    for (Habit h : habits) {
      if (h.hidden || h.pinned) continue;
      String group = h.group == null ? "" : h.group;
      groups.computeIfAbsent(group, k -> new ArrayList<>()).add(h);
    }

    boolean hasNamedGroups = false;
    for (String group : groups.keySet())
      if (!group.isBlank()) {
        hasNamedGroups = true;
        break;
      }

    for (Map.Entry<String, List<Habit>> entry : groups.entrySet()) {
      String group = entry.getKey();
      List<Habit> members = entry.getValue();
      boolean showHeader = !group.isBlank() || hasNamedGroups;

      if (showHeader) {
        listPanel.add(groupHeader(group, members.size()));
        if (!collapsedGroups.contains(group)) {
          for (Habit h : members) listPanel.add(habitRow(h, today));
        }
      } else {
        for (Habit h : members) listPanel.add(habitRow(h, today));
      }
    }

    if (showHidden && hidden > 0) {
      listPanel.add(sectionHeader("Hidden (" + hidden + ")", Color.GRAY));
      for (Habit h : habits)
        if (h.hidden && !h.pinned) listPanel.add(habitRow(h, today));
    }

    if (pinned == null && visible == 0 && (!showHidden || hidden == 0)) {
      JLabel empty = new JLabel(
        habits.isEmpty()
          ? "No habits yet \u2014 add one above"
          : "No visible habits \u2014 type /hidden to show hidden"
      );
      empty.setForeground(Color.GRAY);
      empty.setBorder(BorderFactory.createEmptyBorder(16, 4, 0, 0));
      listPanel.add(empty);
    }

    listPanel.add(Box.createVerticalGlue());
    listPanel.revalidate();
    listPanel.repaint();
  }

  String selectedGroup() {
    Object value = groupBox.isEditable()
      ? groupBox.getEditor().getItem()
      : groupBox.getSelectedItem();
    if (value == null) value = groupBox.getSelectedItem();
    return canonicalGroup(value == null ? "" : value.toString());
  }

  String canonicalGroup(String raw) {
    String name = raw == null
      ? ""
      : raw
        .trim()
        .replace(",", " ")
        .replace("\n", " ")
        .replace("\r", " ");
    if (name.isBlank() || name.equalsIgnoreCase("Ungrouped")) return "";
    for (Habit h : habits)
      if (
        h.group != null && !h.group.isBlank() && h.group.equalsIgnoreCase(name)
      ) return h.group;
    return name;
  }

  String groupLabel(String group) {
    return group == null || group.isBlank() ? "Ungrouped" : group;
  }

  List<String> groupNames() {
    TreeSet<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (Habit h : habits)
      if (h.group != null && !h.group.isBlank()) groups.add(h.group);
    return new ArrayList<>(groups);
  }

  JPanel sectionHeader(String text, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
    label.setForeground(color);

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
    panel.add(label, BorderLayout.WEST);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
    return panel;
  }

  JPanel groupHeader(String group, int count) {
    boolean collapsed = collapsedGroups.contains(group);
    String text =
      (collapsed ? "\u25B8 " : "\u25BE ") +
      groupLabel(group) +
      " (" +
      count +
      ")";

    JButton toggle = new JButton(text);
    toggle.setFont(toggle.getFont().deriveFont(Font.BOLD, 13f));
    toggle.setForeground(new Color(0x4A6FA5));
    toggle.setBorderPainted(false);
    toggle.setContentAreaFilled(false);
    toggle.setFocusPainted(false);
    toggle.setMargin(new Insets(0, 0, 0, 0));
    toggle.setHorizontalAlignment(SwingConstants.LEFT);
    toggle.addActionListener(e -> toggleGroup(group));

    JPanel panel = new JPanel(new BorderLayout(4, 0));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
    panel.add(toggle, BorderLayout.WEST);

    if (!group.isBlank()) {
      JButton options = new JButton("...");
      options.setMargin(new Insets(0, 6, 0, 6));
      options.setFocusable(false);
      options.setToolTipText("Group options");
      options.addActionListener(e -> showGroupMenu(options, group));
      panel.add(options, BorderLayout.EAST);
    }

    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    return panel;
  }

  void toggleGroup(String group) {
    if (!collapsedGroups.remove(group)) collapsedGroups.add(group);
    rebuild();
  }

  void showGroupMenu(Component invoker, String group) {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem rename = new JMenuItem("Rename group");
    rename.addActionListener(e -> renameGroup(group));
    JMenuItem delete = new JMenuItem("Delete group");
    delete.addActionListener(e -> deleteGroup(group));
    menu.add(rename);
    menu.add(delete);
    menu.show(invoker, 0, invoker.getHeight());
  }

  void renameGroup(String group) {
    String raw = JOptionPane.showInputDialog(this, "Rename group", group);
    if (raw == null) return;

    String target = canonicalGroup(raw);
    if (target.equals(group)) return;

    boolean wasCollapsed = collapsedGroups.remove(group);
    for (Habit h : habits)
      if (h.group != null && h.group.equalsIgnoreCase(group)) h.group = target;
    if (wasCollapsed && !target.isBlank()) collapsedGroups.add(target);

    refreshGroupBox();
    groupBox.setSelectedItem(target.isBlank() ? "Ungrouped" : target);
    rebuild();
    save("Renamed group to " + groupLabel(target));
  }

  void deleteGroup(String group) {
    int count = 0;
    for (Habit h : habits)
      if (h.group != null && h.group.equalsIgnoreCase(group)) count++;

    int ok = JOptionPane.showConfirmDialog(
      this,
      "Delete group \"" +
        group +
        "\"?\n" +
        count +
        " habit(s) will become Ungrouped.",
      "Delete Group",
      JOptionPane.OK_CANCEL_OPTION
    );
    if (ok != JOptionPane.OK_OPTION) return;

    for (Habit h : habits)
      if (h.group != null && h.group.equalsIgnoreCase(group)) h.group = "";
    collapsedGroups.remove(group);

    refreshGroupBox();
    groupBox.setSelectedItem("Ungrouped");
    rebuild();
    save("Deleted group " + group);
  }

  JPanel habitRow(Habit h, LocalDate today) {
    JLabel nameLabel = new JLabel(h.name);
    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 15f));
    if (h.hidden) nameLabel.setForeground(Color.GRAY);

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
        if (h.pinned && h.isDone(today)) h.pinned = false;
        save("Updated " + h.name);
        rebuild();
      });
      week.add(b);
    }

    JPanel left = new JPanel(new BorderLayout(0, 4));
    left.add(nameLabel, BorderLayout.NORTH);
    left.add(week, BorderLayout.CENTER);

    int now = h.streak(today);
    JLabel streak = new JLabel("now " + now + "\u00b7 best " + h.bestStreak());
    streak.setForeground(now > 0 ? new Color(0xF5A623) : new Color(0x7A7D85));

    JButton edit = new JButton("Edit");
    edit.addActionListener(e -> editHabit(h));

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
        refreshGroupBox();
        rebuild();
        save("Deleted " + h.name);
      }
    });

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    right.add(streak);
    if (h.pinned) {
      JButton unpin = new JButton("Unpin");
      unpin.addActionListener(e -> unpinHabit());
      right.add(unpin);
    } else if (h.hidden) {
      JButton show = new JButton("Show");
      show.addActionListener(e -> setHidden(h.name, false));
      right.add(show);
    } else {
      JButton pin = new JButton("Pin");
      pin.addActionListener(e -> pinHabit(h));
      right.add(pin);
    }
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
    return row;
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

  void submitInput() {
    String raw = input.getText().trim();
    if (raw.isEmpty()) return;
    input.setText("");
    if (raw.startsWith("/")) handleCommand(raw);
    else addHabit(raw);
  }

  void addHabit(String raw) {
    String name = raw.trim().replace(",", " ").replace(";", " ");
    if (name.isEmpty()) {
      status.setText("Enter a habit name");
      return;
    }
    for (Habit h : habits)
      if (h.name.equalsIgnoreCase(name)) {
        status.setText("Already exists");
        return;
      }
    Habit h = new Habit(name);
    h.group = selectedGroup();
    habits.add(h);
    input.setText("");
    refreshGroupBox();
    rebuild();
    save("Added " + name);
  }

  void refreshGroupBox() {
    String keep = selectedGroup();
    List<String> groups = groupNames();

    groupBox.removeAllItems();
    groupBox.addItem("Ungrouped");
    for (String group : groups) groupBox.addItem(group);

    boolean exists = false;
    for (String group : groups)
      if (group.equalsIgnoreCase(keep)) {
        exists = true;
        break;
      }
    groupBox.setSelectedItem(keep.isBlank() || !exists ? "Ungrouped" : keep);
  }

  void handleCommand(String raw) {
    String[] parts = raw.split("\\s+", 2);
    String cmd = parts[0].toLowerCase();
    String arg = parts.length > 1 ? parts[1].trim() : "";

    switch (cmd) {
      case "/help" -> JOptionPane.showMessageDialog(
        this,
        "/hidden — show hidden habits\n" +
          "/hide <name>\n/show <name>\n" +
          "/pin <name>\n/unpin"
      );
      case "/hidden" -> {
        showHidden = !showHidden;
        rebuild();
        status.setText(
          showHidden ? "Showing hidden habits" : "Hidden habits hidden"
        );
      }
      case "/hide" -> setHidden(arg, true);
      case "/show" -> setHidden(arg, false);
      case "/pin" -> pinHabit(findHabit(arg));
      case "/unpin" -> unpinHabit();
      default -> status.setText("Unknown command — try /help");
    }
  }

  Habit findHabit(String name) {
    if (name.isBlank()) return null;
    for (Habit h : habits) if (h.name.equalsIgnoreCase(name)) return h;
    Habit match = null;
    for (Habit h : habits)
      if (h.name.toLowerCase().startsWith(name.toLowerCase())) {
        if (match != null) return null; // ambiguous
        match = h;
      }
    return match;
  }

  void setHidden(String name, boolean hidden) {
    Habit h = findHabit(name);
    if (h == null) {
      status.setText("Habit not found or ambiguous");
      return;
    }
    h.hidden = hidden;
    save((hidden ? "Hidden " : "Showing ") + h.name);
    rebuild();
  }

  void pinHabit(Habit h) {
    if (h == null) {
      status.setText("Habit not found");
      return;
    }
    LocalDate today = LocalDate.now();
    if (h.isDone(today)) {
      status.setText("Already done today");
      return;
    }
    for (Habit other : habits) other.pinned = false;
    h.pinned = true;
    save("Pinned " + h.name);
    rebuild();
  }

  void unpinHabit() {
    for (Habit h : habits) h.pinned = false;
    save("Unpinned");
    rebuild();
  }
}
