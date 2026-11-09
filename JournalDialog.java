import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class JournalDialog extends JDialog {

  private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern(
    "EEE, MMM d yyyy"
  );
  private static final DateTimeFormatter LIST = DateTimeFormatter.ofPattern(
    "EEE, MMM d"
  );

  private final LocalDate today = LocalDate.now();
  private LocalDate current = today;
  private boolean loading;
  private boolean dirty;
  private boolean adjustingList;

  private final JLabel dateLabel = new JLabel("", SwingConstants.CENTER);
  private final JTextArea editor = new JTextArea();
  private final JLabel status = new JLabel(" ");
  private final DefaultListModel<LocalDate> entryModel =
    new DefaultListModel<>();
  private final JList<LocalDate> entries = new JList<>(entryModel);

  JournalDialog(Main owner) {
    super(owner, "Journal", true);

    JPanel root = new JPanel(new BorderLayout(12, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    setContentPane(root);

    JLabel heading = new JLabel("Journal");
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
    root.add(heading, BorderLayout.NORTH);

    entries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    entries.setCellRenderer(
      new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(
          JList<?> list,
          Object value,
          int index,
          boolean selected,
          boolean focus
        ) {
          super.getListCellRendererComponent(
            list,
            value,
            index,
            selected,
            focus
          );
          if (value instanceof LocalDate d) setText(d.format(LIST));
          return this;
        }
      }
    );
    entries.setPreferredSize(new Dimension(150, 0));
    JScrollPane listScroll = new JScrollPane(entries);
    listScroll.setBorder(BorderFactory.createTitledBorder("Entries"));
    root.add(listScroll, BorderLayout.WEST);

    JPanel editorPanel = new JPanel(new BorderLayout(0, 8));

    JButton prev = new JButton("<");
    prev.addActionListener(e -> goTo(current.minusDays(1)));
    JButton next = new JButton(">");
    next.addActionListener(e -> goTo(current.plusDays(1)));
    JButton todayButton = new JButton("Today");
    todayButton.addActionListener(e -> goTo(today));

    dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD, 15f));

    JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    navLeft.add(prev);
    navLeft.add(next);
    JPanel nav = new JPanel(new BorderLayout(8, 0));
    nav.add(navLeft, BorderLayout.WEST);
    nav.add(dateLabel, BorderLayout.CENTER);
    nav.add(todayButton, BorderLayout.EAST);
    editorPanel.add(nav, BorderLayout.NORTH);

    editor.setLineWrap(true);
    editor.setWrapStyleWord(true);
    editor.setMargin(new Insets(8, 8, 8, 8));
    editor.setFont(editor.getFont().deriveFont(15f));
    editorPanel.add(new JScrollPane(editor), BorderLayout.CENTER);

    status.setForeground(Color.GRAY);
    editorPanel.add(status, BorderLayout.SOUTH);

    root.add(editorPanel, BorderLayout.CENTER);

    JButton save = new JButton("Save");
    JButton close = new JButton("Close");
    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    right.add(close);
    right.add(save);
    root.add(right, BorderLayout.SOUTH);

    editor.getDocument().addDocumentListener(
      new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
          changed();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
          changed();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          changed();
        }
      }
    );

    entries.addListSelectionListener(e -> {
      if (e.getValueIsAdjusting() || adjustingList) return;
      LocalDate selected = entries.getSelectedValue();
      if (selected != null && !selected.equals(current)) goTo(selected);
    });

    save.addActionListener(e -> saveCurrent());
    close.addActionListener(e -> {
      if (saveCurrent()) dispose();
    });

    addWindowListener(
      new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          if (saveCurrent()) dispose();
        }
      }
    );

    editor.getInputMap().put(KeyStroke.getKeyStroke("control S"), "save");
    editor.getActionMap().put(
      "save",
      new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          saveCurrent();
        }
      }
    );

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    load(current);
    setMinimumSize(new Dimension(680, 480));
    setSize(780, 560);
    setLocationRelativeTo(owner);
  }

  private void changed() {
    if (loading) return;
    dirty = true;
    updateStatus();
  }

  private void goTo(LocalDate d) {
    if (d.equals(current)) return;
    if (!saveCurrent()) {
      adjustingList = true;
      entries.setSelectedValue(current, true);
      adjustingList = false;
      return;
    }
    load(d);
  }

  private void load(LocalDate d) {
    current = d;
    dateLabel.setText(d.format(LABEL));

    String text = "";
    String error = null;
    try {
      text = Journal.read(d);
    } catch (IOException ex) {
      error = ex.getMessage();
    }

    loading = true;
    editor.setText(text);
    editor.setCaretPosition(0);
    loading = false;

    dirty = false;
    if (error != null) status.setText("Could not load: " + error);
    else updateStatus();
    refreshEntries();
  }

  private boolean saveCurrent() {
    if (!dirty) return true;
    try {
      Journal.write(current, editor.getText());
      dirty = false;
      updateStatus();
      refreshEntries();
      return true;
    } catch (IOException ex) {
      status.setText("Save failed: " + ex.getMessage());
      return false;
    }
  }

  private void updateStatus() {
    String text = editor.getText();
    if (text.isBlank()) {
      status.setText("No entry");
      return;
    }
    int words = text.trim().split("\\s+").length;
    status.setText(
      (dirty ? "Unsaved" : "Saved") +
        "  \u00b7  " +
        words +
        (words == 1 ? " word" : " words")
    );
  }

  private void refreshEntries() {
    adjustingList = true;
    entryModel.clear();
    try {
      for (LocalDate d : Journal.entryDates()) entryModel.addElement(d);
    } catch (IOException ex) {
      status.setText("Could not list entries: " + ex.getMessage());
    }
    if (!entryModel.isEmpty()) entries.setSelectedValue(current, true);
    adjustingList = false;
  }
}
