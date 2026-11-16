import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Collection;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class HabitDialog extends JDialog {

  private final JTextField nameField = new JTextField();
  private final JComboBox<String> groupBox = new JComboBox<>();
  private final JCheckBox hiddenBox = new JCheckBox("Hidden from main list");
  private boolean saved;

  HabitDialog(Main owner, Habit habit, Collection<String> groups) {
    super(owner, "Edit Habit", true);

    JPanel root = new JPanel(new BorderLayout(12, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    setContentPane(root);

    JLabel heading = new JLabel("Edit Habit");
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
    root.add(heading, BorderLayout.NORTH);

    nameField.setText(habit.name);
    groupBox.setEditable(true);
    groupBox.setMaximumRowCount(12);
    groupBox.addItem("Ungrouped");

    TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (String group : groups)
      if (group != null && !group.isBlank()) sorted.add(group);
    for (String group : sorted) groupBox.addItem(group);
    groupBox.setSelectedItem(
      habit.group == null || habit.group.isBlank() ? "Ungrouped" : habit.group
    );
    hiddenBox.setSelected(habit.hidden);

    JPanel form = new JPanel();
    form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
    form.add(fieldRow("Name", nameField));
    form.add(Box.createVerticalStrut(8));
    form.add(fieldRow("Group", groupBox));
    form.add(Box.createVerticalStrut(8));
    form.add(hiddenBox);
    root.add(form, BorderLayout.CENTER);

    JButton cancel = new JButton("Cancel");
    JButton save = new JButton("Save");
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    buttons.add(cancel);
    buttons.add(save);
    root.add(buttons, BorderLayout.SOUTH);

    cancel.addActionListener(e -> dispose());
    save.addActionListener(e -> {
      if (nameField.getText().isBlank()) {
        JOptionPane.showMessageDialog(
          this,
          "Name cannot be empty",
          "Edit Habit",
          JOptionPane.WARNING_MESSAGE
        );
        return;
      }
      saved = true;
      dispose();
    });
    getRootPane().setDefaultButton(save);

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(420, 250));
    setSize(460, 280);
    setLocationRelativeTo(owner);
    SwingUtilities.invokeLater(() -> {
      nameField.requestFocusInWindow();
      nameField.selectAll();
    });
  }

  boolean showDialog() {
    setVisible(true);
    return saved;
  }

  String habitName() {
    return nameField.getText();
  }

  String groupName() {
    Object value = groupBox.isEditable()
      ? groupBox.getEditor().getItem()
      : groupBox.getSelectedItem();
    return value == null ? "" : value.toString();
  }

  boolean hiddenSelected() {
    return hiddenBox.isSelected();
  }

  private JPanel fieldRow(String label, Component field) {
    JPanel row = new JPanel(new BorderLayout(8, 0));
    row.add(new JLabel(label), BorderLayout.WEST);
    row.add(field, BorderLayout.CENTER);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    return row;
  }
}
