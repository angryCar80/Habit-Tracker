import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class TimerDialog extends JDialog {

  private final FocusTimer timer;
  private final JLabel timeLabel = new JLabel("25:00", SwingConstants.CENTER);
  private final JLabel status = new JLabel("Ready", SwingConstants.CENTER);
  private final JProgressBar progress = new JProgressBar(0, 100);
  private final JButton startPause = new JButton("Start");
  private final JSpinner custom = new JSpinner(
    new SpinnerNumberModel(TimerSettings.loadMinutes(), 1, 180, 1)
  );
  private final FocusTimer.Listener listener = this::refresh;

  TimerDialog(Main owner, FocusTimer timer) {
    super(owner, "Focus Timer", false);
    this.timer = timer;

    JPanel root = new JPanel(new BorderLayout(12, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    setContentPane(root);

    JLabel heading = new JLabel("Focus Timer");
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
    root.add(heading, BorderLayout.NORTH);

    JPanel center = new JPanel();
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

    timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD, 48f));
    timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    center.add(timeLabel);
    center.add(Box.createVerticalStrut(8));

    Color accent = UIManager.getColor("Component.accentColor");
    if (accent != null) progress.setForeground(accent);
    progress.setStringPainted(false);
    progress.setAlignmentX(Component.CENTER_ALIGNMENT);
    progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
    center.add(progress);
    center.add(Box.createVerticalStrut(12));

    JPanel presets = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    int[] minutes = { 5, 15, 25, 45, 60 };
    for (int m : minutes) {
      JButton preset = new JButton(m + "m");
      preset.addActionListener(e -> choose(m));
      presets.add(preset);
    }
    center.add(presets);
    center.add(Box.createVerticalStrut(8));

    JPanel customRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    customRow.add(new JLabel("Minutes"));
    customRow.add(custom);
    JButton set = new JButton("Set");
    set.addActionListener(e -> choose((Integer) custom.getValue()));
    customRow.add(set);
    center.add(customRow);
    center.add(Box.createVerticalStrut(12));

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    JButton reset = new JButton("Reset");
    controls.add(startPause);
    controls.add(reset);
    center.add(controls);
    center.add(Box.createVerticalStrut(8));

    status.setForeground(Color.GRAY);
    status.setAlignmentX(Component.CENTER_ALIGNMENT);
    center.add(status);

    root.add(center, BorderLayout.CENTER);

    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    JButton close = new JButton("Close");
    close.addActionListener(e -> setVisible(false));
    footer.add(close);
    root.add(footer, BorderLayout.SOUTH);

    startPause.addActionListener(e -> {
      if (timer.state() == FocusTimer.State.RUNNING) timer.pause();
      else if (timer.state() == FocusTimer.State.PAUSED) timer.resume();
      else timer.start((Integer) custom.getValue());
    });
    reset.addActionListener(e -> timer.reset());

    timer.addListener(listener);
    addWindowListener(
      new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          timer.removeListener(listener);
        }
      }
    );

    setDefaultCloseOperation(HIDE_ON_CLOSE);
    setMinimumSize(new Dimension(440, 360));
    setSize(480, 400);
    setLocationRelativeTo(owner);
    refresh();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) refresh();
  }

  private void choose(int minutes) {
    custom.setValue(Math.max(1, Math.min(180, minutes)));
    timer.setDuration((Integer) custom.getValue());
  }

  private void refresh() {
    FocusTimer.State state = timer.state();
    int total = timer.durationMinutes() * 60;
    int remaining = timer.remainingSeconds();

    timeLabel.setText(
      FocusTimer.formatTime(state == FocusTimer.State.IDLE ? total : remaining)
    );

    int pct =
      total <= 0 ? 0 : (int) Math.round(((total - remaining) * 100.0) / total);
    progress.setValue(Math.max(0, Math.min(100, pct)));

    switch (state) {
      case RUNNING -> {
        startPause.setText("Pause");
        status.setText("Running");
      }
      case PAUSED -> {
        startPause.setText("Resume");
        status.setText("Paused");
      }
      case FINISHED -> {
        startPause.setText("Start again");
        status.setText("Done \u2014 session logged");
      }
      case IDLE -> {
        startPause.setText("Start");
        status.setText("Ready");
      }
    }

    setTitle(
      state == FocusTimer.State.RUNNING || state == FocusTimer.State.PAUSED
        ? "Focus Timer \u2014 " + timeLabel.getText()
        : "Focus Timer"
    );
  }
}
