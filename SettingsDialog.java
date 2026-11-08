import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class SettingsDialog extends JDialog {

  private final Main owner;
  private final ThemeManager.ThemeOption original;
  private ThemeManager.ThemeOption selected;
  private final JComboBox<ThemeManager.ThemeOption> themeBox =
    new JComboBox<>();
  private final JCheckBox darkOnly = new JCheckBox("Dark Themes Only");
  private boolean adjusting;

  public SettingsDialog(Main owner) {
    super(owner, "Settings", true);
    this.owner = owner;
    this.original = ThemeManager.current();
    this.selected = original;

    JPanel root = new JPanel(new BorderLayout(12, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    setContentPane(root);

    JLabel heading = new JLabel("Settings");
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
    root.add(heading, BorderLayout.NORTH);

    themeBox.setRenderer(
      new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(
          JList<?> list,
          Object value,
          int index,
          boolean isSelected,
          boolean cellHasFocus
        ) {
          super.getListCellRendererComponent(
            list,
            value,
            index,
            isSelected,
            cellHasFocus
          );
          if (value instanceof ThemeManager.ThemeOption t) setText(
            t.name() + "   \u00b7   " + (t.dark() ? "Dark" : "Light")
          );
          return this;
        }
      }
    );
    JPanel themeRow = new JPanel(new BorderLayout(8, 0));
    themeRow.add(new JLabel("Theme"), BorderLayout.WEST);
    themeRow.add(themeBox, BorderLayout.CENTER);

    JPanel appearance = new JPanel(new BorderLayout(8, 8));
    appearance.setBorder(BorderFactory.createTitledBorder("Appearance"));
    appearance.add(themeRow, BorderLayout.NORTH);
    appearance.add(darkOnly, BorderLayout.SOUTH);
    root.add(appearance, BorderLayout.CENTER);

    JButton restore = new JButton("Restore default");
    JButton cancel = new JButton("Cancel");
    JButton save = new JButton("Save");

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    right.add(cancel);
    right.add(save);
    JPanel footer = new JPanel(new BorderLayout());
    footer.add(restore, BorderLayout.WEST);
    footer.add(right, BorderLayout.EAST);
    root.add(footer, BorderLayout.SOUTH);

    darkOnly.addActionListener(e -> {
      rebuildModel();
      preview((ThemeManager.ThemeOption) themeBox.getSelectedItem());
    });
    themeBox.addActionListener(e -> {
      if (!adjusting) preview(
        (ThemeManager.ThemeOption) themeBox.getSelectedItem()
      );
    });

    restore.addActionListener(e -> {
      ThemeManager.ThemeOption def = ThemeManager.defaultTheme();
      if (!contains(def)) {
        darkOnly.setSelected(false);
        rebuildModel();
      }
      preview(def);
    });
    cancel.addActionListener(e -> {
      ThemeManager.apply(original);
      owner.rebuild();
      dispose();
    });
    save.addActionListener(e -> {
      ThemeManager.save(selected);
      dispose();
    });

    rebuildModel();
    preview(original);

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(false);
    pack();
    setMinimumSize(new Dimension(460, getHeight()));
    setLocationRelativeTo(owner);
  }

  private boolean contains(ThemeManager.ThemeOption t) {
    for (int i = 0; i < themeBox.getItemCount(); i++) if (
      themeBox.getItemAt(i).className().equals(t.className())
    ) return true;
    return false;
  }

  private void rebuildModel() {
    ThemeManager.ThemeOption keep = selected;
    DefaultComboBoxModel<ThemeManager.ThemeOption> model =
      new DefaultComboBoxModel<>();
    for (ThemeManager.ThemeOption t : ThemeManager.themes())
      if (!darkOnly.isSelected() || t.dark()) model.addElement(t);

    adjusting = true;
    themeBox.setModel(model);
    if (!contains(keep) && model.getSize() > 0) keep = model.getElementAt(0);
    themeBox.setSelectedItem(keep);
    adjusting = false;
  }

  private void preview(ThemeManager.ThemeOption t) {
    if (t == null) return;
    adjusting = true;
    themeBox.setSelectedItem(t);
    adjusting = false;
    selected = t;
    if (t.className().equals(ThemeManager.current().className())) return;
    if (ThemeManager.apply(t)) owner.rebuild();
  }
}
