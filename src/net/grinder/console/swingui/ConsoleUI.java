// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxStyle;
import org.syntax.jedit.TextAreaPainter;
import org.syntax.jedit.tokenmarker.PythonTokenMarker;
import org.syntax.jedit.tokenmarker.Token;

import net.grinder.common.GrinderException;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleListener;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;


/**
 * Swing UI for console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConsoleUI implements ModelListener {

  private static final Font s_tpsFont =
    new Font("helvetica", Font.ITALIC | Font.BOLD, 40);
  private static final Color s_defaultLabelForeground =
    new JLabel().getForeground();

  private final Map m_actionTable = new HashMap();
  private final StartAction m_startAction;
  private final StopAction m_stopAction;
  private final Model m_model;
  private final JFrame m_frame;
  private final JLabel m_stateLabel = new JLabel();
  private final SamplingControlPanel m_samplingControlPanel;
  private final ErrorHandler m_errorHandler;

  private final CumulativeStatisticsTableModel m_cumulativeTableModel;

  private final String m_stateIgnoringString;
  private final String m_stateWaitingString;
  private final String m_stateStoppedString;
  private final String m_stateStoppedAndIgnoringString;
  private final String m_stateCapturingString;
  private final String m_stateUnknownString;

  private int m_lastState = -1;

  /**
   * Creates a new <code>ConsoleUI</code> instance.
   *
   * @param model The console model.
   * @param startProcessesHandler Action listener to invoke when start
   * processes control is activated.
   * @param resetProcessesHandler Action listener to invoke when reset
   * processes control is activated.
   * @param stopProcessesHandler  Action listener to invoke when stop
   * processes control is activated.
   * @exception ConsoleException if an error occurs
   */
  public ConsoleUI(Model model,
                   ActionListener startProcessesHandler,
                   ActionListener resetProcessesHandler,
                   ActionListener stopProcessesHandler)
    throws ConsoleException {

    m_model = model;
    final Resources resources = m_model.getResources();

    // Create the frame to contain the a menu and the top level
    // pane. Need to do this before our actions are constructed as
    // the use the frame to create dialogs.
    m_frame = new JFrame(resources.getString("title"));

    m_errorHandler = new ErrorDialogHandler(m_frame, resources);

    m_stateIgnoringString = resources.getString("state.ignoring.label") + " ";
    m_stateWaitingString = resources.getString("state.waiting.label");
    m_stateStoppedString = resources.getString("state.stopped.label");
    m_stateStoppedAndIgnoringString =
      resources.getString("state.stoppedAndIgnoring.label") + " ";
    m_stateCapturingString =
      resources.getString("state.capturing.label") + " ";
    m_stateUnknownString = resources.getString("state.unknown.label");

    m_startAction = new StartAction();
    m_stopAction = new StopAction();

    final LabelledGraph totalGraph =
      new LabelledGraph(resources.getString("totalGraph.title"),
                        resources, Colours.DARK_GREY,
                        model.getTPSExpression(),
                        model.getPeakTPSExpression());

    final JLabel tpsLabel = new JLabel();
    tpsLabel.setForeground(Colours.BLACK);
    tpsLabel.setFont(s_tpsFont);

    m_model.addTotalSampleListener(
      new SampleListener() {
        private final String m_suffix = " " + resources.getString("tps.units");

        public void update(TestStatistics intervalStatistics,
                           TestStatistics cumulativeStatistics) {
          final NumberFormat format = m_model.getNumberFormat();

          tpsLabel.setText(
            format.format(
              m_model.getTPSExpression().getDoubleValue(intervalStatistics)) +
            m_suffix);

          totalGraph.add(intervalStatistics, cumulativeStatistics, format);
        }
      });

    final JButton stateButton = new CustomJButton();
    stateButton.setBorderPainted(true);
    stateButton.setAction(m_stopAction);
    stateButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    m_stopAction.registerButton(stateButton);
    m_stateLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
    final Box statePanel = Box.createHorizontalBox();
    statePanel.add(stateButton);
    statePanel.add(m_stateLabel);

    m_samplingControlPanel = new SamplingControlPanel(resources);

    m_samplingControlPanel.add(statePanel);

    m_samplingControlPanel.setBorder(
      BorderFactory.createEmptyBorder(0, 10, 0, 10));
    m_samplingControlPanel.setProperties(m_model.getProperties());

    final JPanel controlAndTotalPanel = new JPanel();
    controlAndTotalPanel.setLayout(
      new BoxLayout(controlAndTotalPanel, BoxLayout.Y_AXIS));

    controlAndTotalPanel.add(m_samplingControlPanel);
    controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 80)));
    controlAndTotalPanel.add(tpsLabel);
    controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 20)));
    controlAndTotalPanel.add(totalGraph);

    // Really wanted this left alligned, but doesn't really work
    // with a box layout.
    tpsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    final JPanel hackToFixLayout = new JPanel();
    hackToFixLayout.add(controlAndTotalPanel);

    // Create the tabbed test display.
    final JTabbedPane tabbedPane = new JTabbedPane();

    final Border threePixelBorder =
      BorderFactory.createEmptyBorder(3, 3, 3, 3);

    final TestGraphPanel graphPanel =
      new TestGraphPanel(tabbedPane, model, resources);
    graphPanel.resetTestsAndStatisticsViews(); // Show logo.

    final JScrollPane graphTabPane =
      new JScrollPane(graphPanel,
                      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    graphTabPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(resources.getString("graphTab.title"),
                      resources.getImageIcon("graphTab.image"),
                      graphTabPane,
                      resources.getString("graphTab.tip"));

    final Font resultsTableLabelFont =
      new JLabel().getFont().deriveFont(Font.PLAIN | Font.ITALIC);

    m_cumulativeTableModel = new CumulativeStatisticsTableModel(model);

    final JScrollPane cumulativeTablePane =
      new JScrollPane(new Table(m_cumulativeTableModel));

    final TitledBorder cumulativeTableTitledBorder =
      BorderFactory.createTitledBorder(
        threePixelBorder, resources.getString("cumulativeTable.label"));

    cumulativeTableTitledBorder.setTitleFont(resultsTableLabelFont);
    cumulativeTableTitledBorder.setTitleColor(Colours.HIGHLIGHT_BLUE);
    cumulativeTableTitledBorder.setTitleJustification(TitledBorder.RIGHT);

    cumulativeTablePane.setBorder(cumulativeTableTitledBorder);
    cumulativeTablePane.setMinimumSize(new Dimension(100, 60));

    final SampleStatisticsTableModel sampleModel =
      new SampleStatisticsTableModel(model);

    final JScrollPane sampleTablePane =
      new JScrollPane(new Table(sampleModel));

    final TitledBorder sampleTableTitledBorder =
      BorderFactory.createTitledBorder(
        threePixelBorder, resources.getString("sampleTable.label"));

    sampleTableTitledBorder.setTitleFont(resultsTableLabelFont);
    sampleTableTitledBorder.setTitleColor(Colours.HIGHLIGHT_BLUE);
    sampleTableTitledBorder.setTitleJustification(TitledBorder.RIGHT);

    sampleTablePane.setBorder(sampleTableTitledBorder);
    sampleTablePane.setMinimumSize(new Dimension(100, 60));

    final JSplitPane resultsPane =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                     cumulativeTablePane,
                     sampleTablePane);

    resultsPane.setOneTouchExpandable(true);
    resultsPane.setResizeWeight(1d);
    resultsPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(resources.getString("resultsTab.title"),
                      resources.getImageIcon("resultsTab.image"),
                      resultsPane,
                      resources.getString("resultsTab.tip"));

    final ProcessStatusTableModel processStatusModel =
      new ProcessStatusTableModel(model, resources);

    final JScrollPane processStatusPane =
      new JScrollPane(new Table(processStatusModel));

    final TitledBorder processTableTitledBorder =
      BorderFactory.createTitledBorder(
        threePixelBorder, resources.getString("processStatusTableTab.tip"));

    processTableTitledBorder.setTitleFont(resultsTableLabelFont);
    processTableTitledBorder.setTitleColor(Colours.HIGHLIGHT_BLUE);
    processTableTitledBorder.setTitleJustification(TitledBorder.RIGHT);

    processStatusPane.setBorder(processTableTitledBorder);

    tabbedPane.addTab(resources.getString("processStatusTableTab.title"),
                      resources.getImageIcon(
                        "processStatusTableTab.image"),
                      processStatusPane,
                      resources.getString("processStatusTableTab.tip"));

    final JEditTextArea scriptTextArea = new JEditTextArea();
    scriptTextArea.setTokenMarker(new PythonTokenMarker());

    // Override ugly default colours.
    final TextAreaPainter painter = scriptTextArea.getPainter();

    final SyntaxStyle[] styles = painter.getStyles();
    styles[Token.KEYWORD1] = new SyntaxStyle(Colours.RED, false, false);
    styles[Token.KEYWORD2] = styles[Token.KEYWORD1];
    styles[Token.KEYWORD3] = styles[Token.KEYWORD1];
    styles[Token.COMMENT1] = new SyntaxStyle(Colours.DARK_GREEN, true, false);
    styles[Token.LITERAL1] = new SyntaxStyle(Colours.BLUE, false, false);
    styles[Token.LITERAL2] = styles[Token.LITERAL1];

    painter.setCaretColor(Colours.DARK_RED);
    painter.setLineHighlightColor(Colours.FEINT_YELLOW);
    painter.setBracketHighlightColor(Colours.GREY);
    painter.setSelectionColor(Colours.GREY);
    // Initial focus?

    scriptTextArea.setMinimumSize(new Dimension(200, 100));
    scriptTextArea.setText(
      resources.getStringFromFile("scriptSupportUnderConstruction.text",
                                  true));
    scriptTextArea.setFirstLine(0);

    final ScriptFilesPanel scriptFilesPanel =
      new ScriptFilesPanel(m_frame, resources);
    scriptFilesPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    scriptFilesPanel.setMinimumSize(new Dimension(100, 100));

    scriptFilesPanel.refresh();

    final JSplitPane scriptPane =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                     scriptFilesPanel,
                     scriptTextArea);

    scriptPane.setOneTouchExpandable(true);
    scriptPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(resources.getString("scriptTab.title"),
                      resources.getImageIcon("scriptTab.image"),
                      scriptPane,
                      resources.getString("scriptTab.tip"));

    final JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(hackToFixLayout, BorderLayout.WEST);
    contentPanel.add(tabbedPane, BorderLayout.CENTER);

    final ImageIcon logoIcon = resources.getImageIcon("logo.image");

    final CustomAction[] actions = {
      new StartProcessesAction(startProcessesHandler),
      new ResetProcessesAction(resetProcessesHandler),
      new StopProcessesAction(stopProcessesHandler),
      m_startAction,
      m_stopAction,
      new SaveAction(),
      new OptionsAction(),
      new ExitAction(),
      new AboutAction(logoIcon),
    };

    for (int i = 0; i < actions.length; i++) {
      m_actionTable.put(actions[i].getKey(), actions[i]);
    }

    // Create a panel to hold the tool bar and the test pane.
    final JPanel toolBarPanel = new JPanel(new BorderLayout());
    toolBarPanel.add(createToolBar(), BorderLayout.NORTH);
    toolBarPanel.add(contentPanel, BorderLayout.CENTER);

    m_frame.addWindowListener(new WindowCloseAdapter());
    final Container topLevelPane = m_frame.getContentPane();
    topLevelPane.add(createMenuBar(), BorderLayout.NORTH);
    topLevelPane.add(toolBarPanel, BorderLayout.CENTER);

    if (logoIcon != null) {
      final Image logoImage = logoIcon.getImage();

      if (logoImage != null) {
        m_frame.setIconImage(logoImage);
      }
    }

    m_model.addModelListener(new SwingDispatchedModelListener(this));
    update();

    m_frame.pack();

    // Arbitary sizing that looks good for Phil.
    m_frame.setSize(new Dimension(900, 600));

    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    m_frame.setLocation(screenSize.width / 2 - m_frame.getSize().width / 2,
                        screenSize.height / 2 - m_frame.getSize().height / 2);

    resultsPane.setDividerLocation(resultsPane.getMaximumDividerLocation());

    m_frame.show();
  }

  private JMenuBar createMenuBar() {

    final JMenuBar menuBar = new JMenuBar();

    final Iterator menuBarIterator =
      tokenise(m_model.getResources().getString("menubar"));

    while (menuBarIterator.hasNext()) {
      final String menuKey = (String)menuBarIterator.next();

      if (">".equals(menuKey)) {
        menuBar.add(Box.createHorizontalGlue());
      }
      else {
        final JMenu menu =
          new JMenu(m_model.getResources().getString(menuKey + ".menu.label"));

        final Iterator menuIterator =
          tokenise(m_model.getResources().getString(menuKey + ".menu"));

        while (menuIterator.hasNext()) {
          final String menuItemKey = (String)menuIterator.next();

          if ("-".equals(menuItemKey)) {
            menu.addSeparator();
          }
          else {
            final JMenuItem menuItem = new JMenuItem();
            setAction(menuItem, menuItemKey);
            menu.add(menuItem);
          }
        }

        menuBar.add(menu);
      }
    }

    return menuBar;
  }

  private JToolBar createToolBar() {

    final JToolBar toolBar = new JToolBar();

    final Iterator toolBarIterator =
      tokenise(m_model.getResources().getString("toolbar"));

    while (toolBarIterator.hasNext()) {
      final String toolKey = (String)toolBarIterator.next();

      if ("-".equals(toolKey)) {
        toolBar.addSeparator();
      }
      else {
        final JButton button = new CustomJButton();
        toolBar.add(button);

        // Must set the action _after_ adding to the toolbar or the
        // rollover image isn't set correctly.
        setAction(button, toolKey);
      }
    }

    return toolBar;
  }

  private void setAction(AbstractButton button, String actionKey) {

    final CustomAction action = (CustomAction)m_actionTable.get(actionKey);

    if (action != null) {
      button.setAction(action);
      action.registerButton(button);
    }
    else {
      System.err.println("Action '" + actionKey + "' not found");
      button.setEnabled(false);
    }
  }

  private int updateStateLabel() {

    final int state = m_model.getState();
    final boolean receivedReport = m_model.getReceivedReport();
    final long sampleCount = m_model.getSampleCount();

    if (state == Model.STATE_WAITING_FOR_TRIGGER) {
      if (receivedReport) {
        m_stateLabel.setText(m_stateIgnoringString + sampleCount);
      }
      else {
        m_stateLabel.setText(m_stateWaitingString);
      }
      m_stateLabel.setForeground(s_defaultLabelForeground);
    }
    else if (state == Model.STATE_STOPPED) {
      if (receivedReport) {
        m_stateLabel.setText(m_stateStoppedAndIgnoringString +
                             sampleCount);
      }
      else {
        m_stateLabel.setText(m_stateStoppedString);
      }

      m_stateLabel.setForeground(Colours.DARK_RED);
    }
    else if (state == Model.STATE_CAPTURING) {
      m_stateLabel.setText(m_stateCapturingString + sampleCount);
      m_stateLabel.setForeground(Colours.DARK_GREEN);
    }
    else {
      m_stateLabel.setText(m_stateUnknownString);
    }

    return state;
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface. The
   * test set has probably changed. We need do nothing.
   *
   * @param newTests New tests.
   * @param modelTestIndex New model test index.
   */
  public void newTests(Set newTests, ModelTestIndex modelTestIndex) {
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface.
   **/
  public void update() {

    final int newState = updateStateLabel();

    if (newState != m_lastState && newState == Model.STATE_STOPPED) {
      m_stopAction.stopped();
    }

    m_lastState = newState;
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface. New
   * <code>StatisticsView</code>s have been added. We need do
   * nothing.
   *
   * @param intervalStatisticsView Interval statistics view.
   * @param cumulativeStatisticsView Cumulative statistics view.
   */
  public void newStatisticsViews(
    StatisticsView intervalStatisticsView,
    StatisticsView cumulativeStatisticsView) {
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface.
   * Existing <code>Test</code>s and <code>StatisticsView</code>s have
   * been discarded. We need do nothing.
   */
  public void resetTestsAndStatisticsViews() {
  }

  private static final class WindowCloseAdapter extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }

  private final class SaveAction extends CustomAction {

    private final JFileChooser m_fileChooser = new JFileChooser(".");

    SaveAction() {
      super(m_model.getResources(), "save", true);

      m_fileChooser.setDialogTitle(
        m_model.getResources().getString("save.label"));
      m_fileChooser.setSelectedFile(
        new File(m_model.getResources().getString("default.filename")));
    }

    public void actionPerformed(ActionEvent event) {

      try {
        if (m_fileChooser.showSaveDialog(m_frame) ==
            JFileChooser.APPROVE_OPTION) {

          final File file = m_fileChooser.getSelectedFile();

          if (file.exists() &&
              JOptionPane.showConfirmDialog(
                m_frame,
                m_model.getResources().getString("overwriteConfirmation.text"),
                file.toString(), JOptionPane.YES_NO_OPTION) ==
              JOptionPane.NO_OPTION) {
            return;
          }

          try {
            final FileWriter writer = new FileWriter(file);
            m_cumulativeTableModel.write(writer, "\t",
                                         System.getProperty("line.separator"));
            writer.close();
          }
          catch (IOException e) {
            getErrorHandler().handleErrorMessage(
              e.getMessage(),
              m_model.getResources().getString("fileError.title"));
          }
        }
      }
      catch (Exception e) {
        getErrorHandler().handleException(
          e, m_model.getResources().getString("unexpectedError.title"));
      }
    }
  }

  private final class OptionsAction extends CustomAction {

    private final OptionsDialogHandler m_optionsDialogHandler;

    OptionsAction() {
      super(m_model.getResources(), "options", true);

      m_optionsDialogHandler =
        new OptionsDialogHandler(m_frame, m_model.getProperties(),
                                 m_model.getResources()) {
          protected void setNewOptions(ConsoleProperties newOptions) {
            m_model.getProperties().set(newOptions);
            m_samplingControlPanel.refresh();
          }
        };
    }

    public void actionPerformed(ActionEvent event) {
      m_optionsDialogHandler.showDialog(m_model.getProperties());
    }
  }

  private final class AboutAction extends CustomAction {

    private final ImageIcon m_logoIcon;
    private final String m_title;
    private final Component m_contents;

    AboutAction(ImageIcon logoIcon) {
      super(m_model.getResources(), "about", true);

      m_logoIcon = logoIcon;
      m_title = m_model.getResources().getString("about.label");

      JLabel text =
        new JLabel() {
          public Dimension getPreferredSize() {
            final Dimension d = super.getPreferredSize();
            d.width = 450;
            return d;
          }
        };

      text.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      text.setForeground(Colours.BLACK);
      text.setText(
        m_model.getResources().getStringFromFile("about.text", true));

      m_contents =
        new JScrollPane(text,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
          public Dimension getPreferredSize() {
            final Dimension d = super.getPreferredSize();
            d.width = 500;
            d.height = 400;
            return d;
          }
        };
    }

    public void actionPerformed(ActionEvent event) {
      JOptionPane.showMessageDialog(m_frame, m_contents, m_title,
                                    JOptionPane.PLAIN_MESSAGE,
                                    m_logoIcon);
    }
  }

  private final class ExitAction extends CustomAction {

    ExitAction() {
      super(m_model.getResources(), "exit");
    }

    public void actionPerformed(ActionEvent e) {
      System.exit(0);
    }
  }

  private final class StartAction extends CustomAction {

    StartAction() {
      super(m_model.getResources(), "start");
    }

    public void actionPerformed(ActionEvent e) {

      m_model.start();

      //  putValue() won't work here as the event won't fire if
      //  the value doesn't change.
      firePropertyChange(SET_ACTION_PROPERTY, null, m_stopAction);
      updateStateLabel();
    }
  }

  private final class StopAction extends CustomAction {

    StopAction() {
      super(m_model.getResources(), "stop");
    }

    public void actionPerformed(ActionEvent e) {
      m_model.stop();
      stopped();
    }

    public void stopped() {
      //  putValue() won't work here as the event won't fire if
      //  the value doesn't change.
      firePropertyChange(SET_ACTION_PROPERTY, null, m_startAction);
      updateStateLabel();
    }
  }

  private final class StartProcessesAction extends CustomAction {

    private final ActionListener m_delegateAction;

    StartProcessesAction(ActionListener delegateAction) {
      super(m_model.getResources(), "start-processes");
      m_delegateAction = delegateAction;
    }

    public void actionPerformed(ActionEvent e) {
      m_delegateAction.actionPerformed(e);
    }
  }

  private  final class ResetProcessesAction extends CustomAction {

    private final ActionListener m_delegateAction;

    ResetProcessesAction(ActionListener delegateAction) {
      super(m_model.getResources(), "reset-processes");
      m_delegateAction = delegateAction;
    }

    public void actionPerformed(ActionEvent event) {

      final ConsoleProperties properties = m_model.getProperties();

      if (!properties.getResetConsoleWithProcessesDontAsk()) {

        final JCheckBox dontAskMeAgainCheckBox =
          new JCheckBox(
            m_model.getResources().getString("dontAskMeAgain.text"));
        dontAskMeAgainCheckBox.setAlignmentX(Component.RIGHT_ALIGNMENT);

        final Object[] message = {
          m_model.getResources().getString(
            "resetConsoleWithProcessesConfirmation1.text"),
          m_model.getResources().getString(
            "resetConsoleWithProcessesConfirmation2.text"),
          new JLabel(), // Pad.
          dontAskMeAgainCheckBox,
        };

        final int chosen =
          JOptionPane.showConfirmDialog(m_frame, message,
                                        (String) getValue(NAME),
                                        JOptionPane.YES_NO_CANCEL_OPTION);

        if (dontAskMeAgainCheckBox.isSelected()) {
          try {
            properties.setResetConsoleWithProcessesDontAsk();
          }
          catch (GrinderException e) {
            getErrorHandler().handleException(
              e, m_model.getResources().getString("unexpectedError.title"));
            return;
          }
        }

        switch (chosen) {
        case JOptionPane.YES_OPTION:
          properties.setResetConsoleWithProcesses(true);
          break;

        case JOptionPane.NO_OPTION:
          properties.setResetConsoleWithProcesses(false);
          break;

        default:
          return;
        }
      }

      if (properties.getResetConsoleWithProcesses()) {
        m_model.reset();
      }

      m_delegateAction.actionPerformed(event);
    }
  }

  private final class StopProcessesAction extends CustomAction {

    private final ActionListener m_delegateAction;

    StopProcessesAction(ActionListener delegateAction) {
      super(m_model.getResources(), "stop-processes");
      m_delegateAction = delegateAction;
    }

    public void actionPerformed(ActionEvent event) {

      final ConsoleProperties properties = m_model.getProperties();

      if (!properties.getStopProcessesDontAsk()) {

        final JCheckBox dontAskMeAgainCheckBox =
          new JCheckBox(
            m_model.getResources().getString("dontAskMeAgain.text"));
        dontAskMeAgainCheckBox.setAlignmentX(Component.RIGHT_ALIGNMENT);

        final Object[] message = {
          m_model.getResources().getString("stopProcessesConfirmation1.text"),
          m_model.getResources().getString("stopProcessesConfirmation2.text"),
          new JLabel(), // Pad.
          dontAskMeAgainCheckBox,
        };

        final int chosen =
          JOptionPane.showConfirmDialog(m_frame, message,
                                        (String) getValue(NAME),
                                        JOptionPane.OK_CANCEL_OPTION);

        if (dontAskMeAgainCheckBox.isSelected()) {
          try {
            properties.setStopProcessesDontAsk();
          }
          catch (GrinderException e) {
            getErrorHandler().handleException(
              e, m_model.getResources().getString("unexpectedError.title"));
            return;
          }
        }

        if (chosen != JOptionPane.OK_OPTION) {
          return;
        }
      }

      m_delegateAction.actionPerformed(event);
    }
  }

  private static Iterator tokenise(String string) {
    final LinkedList list = new LinkedList();

    final StringTokenizer t = new StringTokenizer(string);

    while (t.hasMoreTokens()) {
      list.add(t.nextToken());
    }

    return list.iterator();
  }

  /**
   * Return an error handler that other classes can use to report
   * problems through the UI. Should be called from a Swing Thread.
   *
   * @return The exception handler.
   */
  public ErrorHandler getErrorHandler() {
    return m_errorHandler;
  }
}
