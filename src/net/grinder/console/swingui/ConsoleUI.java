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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ConsoleExceptionHandler;
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
public class ConsoleUI implements ModelListener, ConsoleExceptionHandler {

  private static final Font s_tpsFont =
    new Font("helvetica", Font.ITALIC | Font.BOLD, 40);
  private static final Color s_defaultLabelForeground =
    new JLabel().getForeground();
  private static final Color s_darkGreen = new Color(0, 0x80, 00);

  private final Map m_actionTable = new HashMap();
  private final StartAction m_startAction;
  private final StopAction m_stopAction;
  private final Model m_model;
  private final JFrame m_frame;
  private final JLabel m_stateLabel = new JLabel();
  private final SamplingControlPanel m_samplingControlPanel;

  private final Resources m_resources;

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
    m_resources = new Resources();

    // Create the frame to contain the a menu and the top level
    // pane. Need to do this before our actions are constructed as
    // the use the frame to create dialogs.
    m_frame = new JFrame(m_resources.getString("title"));

    m_stateIgnoringString = 
      m_resources.getString("state.ignoring.label") + " ";
    m_stateWaitingString = m_resources.getString("state.waiting.label");
    m_stateStoppedString = m_resources.getString("state.stopped.label");
    m_stateStoppedAndIgnoringString =
      m_resources.getString("state.stoppedAndIgnoring.label") + " ";
    m_stateCapturingString =
      m_resources.getString("state.capturing.label") + " ";
    m_stateUnknownString = m_resources.getString("state.unknown.label");

    m_startAction = new StartAction();
    m_stopAction = new StopAction();

    final LabelledGraph totalGraph =
      new LabelledGraph(m_resources.getString("totalGraph.title"),
			m_resources, Color.darkGray,
			m_model.getTPSExpression(),
			m_model.getPeakTPSExpression());

    final JLabel tpsLabel = new JLabel();
    tpsLabel.setForeground(Color.black);
    tpsLabel.setFont(s_tpsFont);

    m_model.addTotalSampleListener(
      new SampleListener() {
	private final String suffix = " " + m_resources.getString("tps.units");

	public void update(TestStatistics intervalStatistics,
			   TestStatistics cumulativeStatistics) {
	  final NumberFormat format = m_model.getNumberFormat();
		    
	  tpsLabel.setText(
	    format.format(
	      m_model.getTPSExpression().getDoubleValue(intervalStatistics)) +
	    suffix);

	  totalGraph.add(intervalStatistics, cumulativeStatistics, format);
	}
      });

    final JButton stateButton = new MyJButton();
    stateButton.putClientProperty("hideActionText", Boolean.TRUE);
    stateButton.setBorderPainted(true);
    stateButton.setContentAreaFilled(false);
    stateButton.setAction(m_stopAction);
    stateButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    m_stopAction.registerButton(stateButton);
    m_stateLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
    final Box statePanel = Box.createHorizontalBox();
    statePanel.add(stateButton);
    statePanel.add(m_stateLabel);

    m_samplingControlPanel = new SamplingControlPanel(m_resources);

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

    final JScrollPane graphTabPane =
      new JScrollPane(new TestGraphPanel(model, m_resources));

    graphTabPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("graphTab.title"),
		      m_resources.getImageIcon("graphTab.image"),
		      graphTabPane,
		      m_resources.getString("graphTab.tip"));

    final CumulativeStatisticsTableModel cumulativeModel =
      new CumulativeStatisticsTableModel(model, m_resources, true);

    final JScrollPane cumulativeTablePane =
      new JScrollPane(new Table(cumulativeModel));

    cumulativeTablePane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("cumulativeTableTab.title"),
		      m_resources.getImageIcon("cumulativeTableTab.image"),
		      cumulativeTablePane,
		      m_resources.getString("cumulativeTableTab.tip"));

    final SampleStatisticsTableModel sampleModel =
      new SampleStatisticsTableModel(model, m_resources);

    final JScrollPane sampleTablePane =
      new JScrollPane(new Table(sampleModel));

    sampleTablePane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("sampleTableTab.title"),
		      m_resources.getImageIcon("sampleTableTab.image"),
		      sampleTablePane,
		      m_resources.getString("sampleTableTab.tip"));

    final ProcessStatusTableModel processStatusModel =
      new ProcessStatusTableModel(model, m_resources);

    final JScrollPane processStatusPane =
      new JScrollPane(new Table(processStatusModel));

    processStatusPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("processStatusTableTab.title"),
		      m_resources.getImageIcon(
			"processStatusTableTab.image"),
		      processStatusPane,
		      m_resources.getString("processStatusTableTab.tip"));

    final JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(hackToFixLayout, BorderLayout.WEST);
    contentPanel.add(tabbedPane, BorderLayout.CENTER);

    final ImageIcon logoIcon = m_resources.getImageIcon("logo.image");

    final MyAction[] actions = {
      new StartProcessesGrinderAction(startProcessesHandler),
      new ResetProcessesGrinderAction(resetProcessesHandler),
      new StopProcessesGrinderAction(stopProcessesHandler),
      new ResetAction(),
      m_startAction,
      m_stopAction,
      new SaveAction(),
      new OptionsAction(),
      new ExitAction(),
      new AboutAction(logoIcon),
    };

    for (int i=0; i<actions.length; i++) {
      m_actionTable.put(actions[i].getKey(), actions[i]);
    }

    // Create a panel to hold the tool bar and the test pane.
    final JPanel toolBarPanel = new JPanel(new BorderLayout());
    toolBarPanel.add(createToolBar(), BorderLayout.NORTH);
    toolBarPanel.add(contentPanel, BorderLayout.CENTER);

    m_frame.addWindowListener(new WindowCloseAdapter());
    final Container topLevelPane= m_frame.getContentPane();
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

    final Dimension screenSize =
      Toolkit.getDefaultToolkit().getScreenSize();

    m_frame.setLocation(screenSize.width/2 - m_frame.getSize().width/2,
			screenSize.height/2 - m_frame.getSize().height/2);
    m_frame.show();
  }

  private final JMenuBar createMenuBar() {

    final JMenuBar menuBar = new JMenuBar();

    final Iterator menuBarIterator =
      tokenise(m_resources.getString("menubar"));
	
    while (menuBarIterator.hasNext()) {
      final String menuKey = (String)menuBarIterator.next();

      if (">".equals(menuKey)) {
	menuBar.add(Box.createHorizontalGlue());
      }
      else {
	final JMenu menu =
	  new JMenu(m_resources.getString(menuKey + ".menu.label"));

	final Iterator menuIterator =
	  tokenise(m_resources.getString(menuKey + ".menu"));

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

  private final JToolBar createToolBar() {

    final JToolBar toolBar = new JToolBar();
	
    final Iterator toolBarIterator =
      tokenise(m_resources.getString("toolbar"));

    while (toolBarIterator.hasNext()) {
      final String toolKey = (String)toolBarIterator.next();

      if ("-".equals(toolKey)) {
	toolBar.addSeparator();
      }
      else {
	final JButton button = new MyJButton();
	button.putClientProperty("hideActionText", Boolean.TRUE);
	button.setContentAreaFilled(false);
	toolBar.add(button);

	// Must set the action _after_ adding to the toolbar or the
	// rollover image isn't set correctly.
	setAction(button, toolKey);
      }
    }

    return toolBar;
  }

  private final void setAction(AbstractButton button, String actionKey) {

    final MyAction action = (MyAction)m_actionTable.get(actionKey);

    if (action != null) {
      button.setAction(action);
      action.registerButton(button);
    }
    else {
      System.err.println("Action '" + actionKey + "' not found");
      button.setEnabled(false);
    }
  }

  private final int updateStateLabel() {

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

      m_stateLabel.setForeground(Color.red);
    }
    else if (state == Model.STATE_CAPTURING) {
      m_stateLabel.setText(m_stateCapturingString + sampleCount);
      m_stateLabel.setForeground(s_darkGreen);
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
  public final void newTests(Set newTests, ModelTestIndex modelTestIndex) {
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface.
   **/
  public final void update() {

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
  public final void newStatisticsViews(
    StatisticsView intervalStatisticsView,
    StatisticsView cumulativeStatisticsView) {
  }

    /**
   * {@link net.grinder.console.model.ModelListener} interface.
   * Existing <code>Test</code<s and <code>StatisticsView</code>s have
   * been discarded. We need do nothing.
   */
  public final void resetTestsAndStatisticsViews() {
  }

  private static final class WindowCloseAdapter extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }

  private abstract class MyAction extends AbstractAction {

    public static final String ROLLOVER_ICON = "RolloverIcon";

    protected static final String SET_ACTION_PROPERTY = "setAction";

    private final String m_key;
    private final Set m_propertyChangeListenersByButton = new HashSet();

    public MyAction(String key) {
      this(key, false);
    }
	
    public MyAction(String key, boolean isDialogAction) {
      super();
      
      m_key = key;
      
      final String label = m_resources.getString(m_key + ".label", false);
      
      if (label != null) {
	if (isDialogAction) {
	  putValue(Action.NAME, label + "...");
	}
	else {
	  putValue(Action.NAME, label);
	}
      }

      final String tip = m_resources.getString(m_key + ".tip", false);
      
      if (tip != null) {
	putValue(Action.SHORT_DESCRIPTION, tip);
      }

      final ImageIcon imageIcon = m_resources.getImageIcon(m_key + ".image");
	    
      if (imageIcon != null) {
	putValue(Action.SMALL_ICON, imageIcon);
      }

      final ImageIcon rolloverImageIcon =
	m_resources.getImageIcon(m_key + ".rollover-image");

      if (rolloverImageIcon != null) {
	putValue(ROLLOVER_ICON, rolloverImageIcon);
      }
    }

    public final String getKey() {
      return m_key;
    }

    public final void registerButton(final AbstractButton button) {
      if (!m_propertyChangeListenersByButton.contains(button)) {
	addPropertyChangeListener(
	  new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent e) {
	      if (e.getPropertyName().equals(SET_ACTION_PROPERTY)) {

		final MyAction newAction = (MyAction)e.getNewValue();

		button.setAction(newAction);
		newAction.registerButton(button);
	      }
	    }
	  }
	  );

	m_propertyChangeListenersByButton.add(button);
      }
    }
  }
    
  private final class MyJButton extends JButton {
    
    public void setAction(Action a) {
      super.setAction(a);

      final ImageIcon rolloverImageIcon = 
	(ImageIcon)a.getValue(MyAction.ROLLOVER_ICON);

      if (rolloverImageIcon != null) {
	setRolloverIcon(rolloverImageIcon);
      }
    }
  }

  private final class SaveAction extends MyAction {

    private final JFileChooser m_fileChooser = new JFileChooser(new File("."));

    SaveAction() {
      super("save", true);

      m_fileChooser.setDialogTitle(m_resources.getString("save.label"));
      m_fileChooser.setSelectedFile(
	new File(m_resources.getString("default.filename")));
    }

    public final void actionPerformed(ActionEvent event) {

      try {
	if (m_fileChooser.showSaveDialog(m_frame) ==
	    JFileChooser.APPROVE_OPTION) {

	  final File file = m_fileChooser.getSelectedFile();

	  if (file.exists() &&
	      JOptionPane.showConfirmDialog(
		m_frame,
		m_resources.getString("overwriteConfirmation.text"),
		file.toString(), JOptionPane.YES_NO_OPTION) ==
	      JOptionPane.NO_OPTION) {
	    return;
	  }

	  final CumulativeStatisticsTableModel model =
	    new CumulativeStatisticsTableModel(m_model,
					       m_resources, false);
	  model.update();

	  try {
	    final FileWriter writer = new FileWriter(file);
	    model.write(writer, "\t", System.getProperty("line.separator"));
	    writer.close();
	  }
	  catch (IOException e) {
	    JOptionPane.showMessageDialog(
	      m_frame, e.getMessage(),
	      m_resources.getString("fileError.title"),
	      JOptionPane.ERROR_MESSAGE);
	  }
	}
      }
      catch (Exception e) {
	JOptionPane.showMessageDialog(
	  m_frame, e.getMessage(),
	  m_resources.getString("unexpectedError.title"),
	  JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private final class OptionsAction extends MyAction {

    private final OptionsDialogHandler m_optionsDialogHandler;

    OptionsAction() {
      super("options", true);

      m_optionsDialogHandler =
	new OptionsDialogHandler(m_frame, m_model.getProperties(),
				 m_resources) {
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

  private final class AboutAction extends MyAction {

    private final ImageIcon m_logoIcon;
    private final String m_title;
    private final Component m_contents;

    AboutAction(ImageIcon logoIcon) {
      super("about", true);

      m_logoIcon = logoIcon;
      m_title = m_resources.getString("about.label");

      JLabel text =
	new JLabel() {
	  public Dimension getPreferredSize() {
	    Dimension d = super.getPreferredSize();
	    d.width = 450;
	    return d;
	  }
	};

      text.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      text.setForeground(Color.black);
      text.setText(m_resources.getStringFromFile("about.text", true));

      m_contents =
	new JScrollPane(text,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
	  public Dimension getPreferredSize() {
	    Dimension d = super.getPreferredSize();
	    d.width = 500;
	    d.height = 400;
	    return d;
	  }
	};
    }

    public final void actionPerformed(ActionEvent event) {
      JOptionPane.showMessageDialog(m_frame, m_contents, m_title,
				    JOptionPane.PLAIN_MESSAGE,
				    m_logoIcon);
    }
  }
    
  private final class ExitAction extends MyAction {
    ExitAction() {
      super("exit");
    }

    public void actionPerformed(ActionEvent e) {
      System.exit(0);
    }
  }

  private final class ResetAction extends MyAction {
    ResetAction() {
      super("reset");
    }

    public void actionPerformed(ActionEvent e) {
      m_model.reset();
    }
  }

  private final class StartAction extends MyAction {
    StartAction() {
      super("start");
    }

    public void actionPerformed(ActionEvent e) {

      m_model.start();

      //  putValue() won't work here as the event won't fire if
      //  the value doesn't change.
      firePropertyChange(SET_ACTION_PROPERTY, null, m_stopAction);
      updateStateLabel();
    }
  }

  private final class StopAction extends MyAction {
    StopAction() {
      super("stop");
    }

    public final void actionPerformed(ActionEvent e) {
      m_model.stop();
      stopped();
    }

    public final void stopped() {
      //  putValue() won't work here as the event won't fire if
      //  the value doesn't change.
      firePropertyChange(SET_ACTION_PROPERTY, null, m_startAction);
      updateStateLabel();
    }
  }

  private final class StartProcessesGrinderAction extends MyAction {
    private final ActionListener m_delegateAction;

    StartProcessesGrinderAction(ActionListener delegateAction) {
      super("start-processes");
      m_delegateAction = delegateAction;
    }

    public final void actionPerformed(ActionEvent e) {
      m_delegateAction.actionPerformed(e);
    }
  }

  private  final class ResetProcessesGrinderAction extends MyAction {
    private final ActionListener m_delegateAction;

    ResetProcessesGrinderAction(ActionListener delegateAction) {
      super("reset-processes");
      m_delegateAction = delegateAction;
    }

    public final void actionPerformed(ActionEvent e) {
      m_delegateAction.actionPerformed(e);
    }
  }

  private final class StopProcessesGrinderAction extends MyAction {
    private final ActionListener m_delegateAction;

    StopProcessesGrinderAction(ActionListener delegateAction) {
      super("stop-processes");
      m_delegateAction = delegateAction;
    }

    public final void actionPerformed(ActionEvent e) {
      m_delegateAction.actionPerformed(e);
    }
  }

  private static final Iterator tokenise(String string) {
    final LinkedList list = new LinkedList();

    final StringTokenizer t = new StringTokenizer(string);

    while (t.hasMoreTokens()) {
      list.add(t.nextToken());
    }

    return list.iterator();
  }

  /**
   * Display an error message dialog.
   *
   * @param e A <code>ConsoleException</code> containing the message
   * to display.
   */
  public final void consoleExceptionOccurred(ConsoleException e) {
    JOptionPane.showMessageDialog(m_frame, e.getMessage(),
				  m_resources.getString("error.title"),
				  JOptionPane.ERROR_MESSAGE);
  }    
}
