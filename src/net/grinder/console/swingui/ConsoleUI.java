// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.console.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
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
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.grinder.console.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.console.model.SampleListener;
import net.grinder.statistics.CumulativeStatistics;
import net.grinder.statistics.IntervalStatistics;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ConsoleUI implements ModelListener
{
    private final static Font s_tpsFont =
	new Font("helvetica", Font.ITALIC | Font.BOLD, 40);

    private final Map m_actionTable = new HashMap();
    private final StartAction m_startAction;
    private final StopAction m_stopAction;
    private final Model m_model;
    private final JLabel m_collectSampleLabel;
    private final JLabel m_ignoreSampleLabel;
    private final JLabel m_intervalLabel;
    private final JLabel m_stateLabel;
    private final JFrame m_frame;

    private final Resources m_resources;

    private final String m_sampleIntervalString;
    private final String m_msUnit;
    private final String m_msUnits;
    private final String m_ignoreCountZeroString;
    private final String m_ignoreCountString;
    private final String m_collectCountZeroString;
    private final String m_collectCountString;
    private final String m_sampleUnit;
    private final String m_sampleUnits;
    private final String m_stateIgnoringString;
    private final String m_stateWaitingString;
    private final String m_stateStoppedString;
    private final String m_stateStoppedAndIgnoringString;
    private final String m_stateCapturingString;
    private final String m_stateUnknownString;


    public ConsoleUI(Model model, ActionListener startProcessesHandler,
		     ActionListener resetProcessesHandler,
		     ActionListener stopProcessesHandler)
	throws ConsoleException
    {
	m_resources = new Resources();

	m_sampleIntervalString =
	    m_resources.getString("sampleInterval.label") + " ";

	m_msUnit = " " + m_resources.getString("ms.unit");
	m_msUnits = " " + m_resources.getString("ms.units");

	m_ignoreCountZeroString =
	    m_resources.getString("ignoreCountZero.label", false);
	m_ignoreCountString = m_resources.getString("ignoreCount.label") + " ";

	m_collectCountZeroString =
	    m_resources.getString("collectCountZero.label", false);
	m_collectCountString =
	    m_resources.getString("collectCount.label") + " ";

	m_sampleUnit = " " + m_resources.getString("sample.unit");
	m_sampleUnits = " " + m_resources.getString("sample.units");

	m_startAction = new StartAction();
	m_stopAction = new StopAction();

	m_stateIgnoringString =
	    m_resources.getString("state.ignoring.label") + " ";
	m_stateWaitingString = m_resources.getString("state.waiting.label");
	m_stateStoppedString = m_resources.getString("state.stopped.label");
	m_stateStoppedAndIgnoringString =
	    m_resources.getString("state.stoppedAndIgnoring.label");
	m_stateCapturingString =
	    m_resources.getString("state.capturing.label") + " ";
	m_stateUnknownString = m_resources.getString("state.unknown.label");

	final MyAction[] actions = {
	    new StartProcessesGrinderAction(startProcessesHandler),
	    new ResetProcessesGrinderAction(resetProcessesHandler),
	    new StopProcessesGrinderAction(stopProcessesHandler),
	    m_startAction,
	    m_stopAction,
	    new SaveAction(),
	    new ExitAction(),
	};

	for (int i=0; i<actions.length; i++) {
	    m_actionTable.put(actions[i].getKey(), actions[i]);
	}

	m_model = model;

	final LabelledGraph totalGraph =
	    new LabelledGraph(m_resources.getString("totalGraph.title"),
			      m_resources, Color.darkGray);

	final JLabel tpsLabel = new JLabel();
	tpsLabel.setForeground(Color.black);
	tpsLabel.setFont(s_tpsFont);

	m_model.addTotalSampleListener(
	    new SampleListener() {
		private final String m_suffix =
		    " " + m_resources.getString("tps.units");

		public void update(IntervalStatistics intervalStatistics,
				   CumulativeStatistics cumulativeStatistics) {
		    final NumberFormat format = m_model.getNumberFormat();
		    
		    tpsLabel.setText(
			format.format(intervalStatistics.getTPS()) + m_suffix);

		    totalGraph.add(intervalStatistics, cumulativeStatistics,
				   format);
		}
	    });

	final JSlider intervalSlider =
	    new JSlider(100, 10000, m_model.getSampleInterval());
	intervalSlider.setMajorTickSpacing(1000);
	intervalSlider.setMinorTickSpacing(100);
	intervalSlider.setPaintTicks(true);
	intervalSlider.setSnapToTicks(true);

	intervalSlider.addChangeListener(
	    new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			m_model.setSampleInterval(intervalSlider.getValue());
		    }
		}
	    );

	m_intervalLabel = new JLabel();

	final JSlider sfSlider =
	    new JSlider(1, 6, m_model.getSignificantFigures());
	sfSlider.setMajorTickSpacing(1);
	sfSlider.setPaintLabels(true);
	sfSlider.setSnapToTicks(true);
	sfSlider.setPreferredSize(new Dimension(0, 0));

	sfSlider.addChangeListener(
	    new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			m_model.setSignificantFigures(sfSlider.getValue());
		    }
		}
	    );

	final JPanel sfPanel = new JPanel();
	sfPanel.setLayout(new GridLayout(1, 0));
	sfPanel.add(new JLabel(
			m_resources.getString("significantFigures.label")));
	sfPanel.add(sfSlider);

	final IntegerField ignoreSampleField = new IntegerField(0, 999999);
	ignoreSampleField.setValue(m_model.getIgnoreSampleCount());

	m_ignoreSampleLabel = new JLabel();

	ignoreSampleField.addChangeListener(
	    new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			m_model.setIgnoreSampleCount(
			    ignoreSampleField.getValue());
		    }
		}
	    );

	final IntegerField collectSampleField = new IntegerField(0, 999999);
	collectSampleField.setValue(m_model.getCollectSampleCount());

	m_collectSampleLabel = new JLabel();

	collectSampleField.addChangeListener(
	    new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			m_model.setCollectSampleCount(
			    collectSampleField.getValue());
		    }
		}
	    );

	final JPanel textFieldLabelPanel = new JPanel();
	textFieldLabelPanel.setLayout(new GridLayout(0, 1));
	textFieldLabelPanel.add(m_ignoreSampleLabel);
	textFieldLabelPanel.add(m_collectSampleLabel);

	final JPanel textFieldControlPanel = new JPanel();
	textFieldControlPanel.setLayout(new GridLayout(0, 1));
	textFieldControlPanel.add(ignoreSampleField);
	textFieldControlPanel.add(collectSampleField);

	final JPanel textFieldPanel = new JPanel();
	textFieldPanel.setLayout(new BorderLayout());
	textFieldPanel.add(textFieldLabelPanel, BorderLayout.CENTER);
	textFieldPanel.add(textFieldControlPanel, BorderLayout.EAST);

	final JButton stateButton = new JButton();
	stateButton.putClientProperty("hideActionText", Boolean.TRUE);
	stateButton.setAction(m_stopAction);
	stateButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	m_stopAction.registerButton(stateButton);
	m_stateLabel = new JLabel();
	m_stateLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
	final Box statePanel = Box.createHorizontalBox();
	statePanel.add(stateButton);
	statePanel.add(m_stateLabel);

	final JPanel controlPanel = new JPanel();
	controlPanel.setLayout(new GridLayout(0, 1));
	controlPanel.add(m_intervalLabel);
	controlPanel.add(intervalSlider);
	controlPanel.add(sfPanel);
	controlPanel.add(textFieldPanel);
	controlPanel.add(statePanel);
	controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

	final JPanel controlAndTotalPanel = new JPanel();
	controlAndTotalPanel.setLayout(
	    new BoxLayout(controlAndTotalPanel, BoxLayout.Y_AXIS));

	controlAndTotalPanel.add(controlPanel);
	controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 20)));
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

	tabbedPane.addTab(m_resources.getString("graphTab.title"),
			  m_resources.getImageIcon("graphTab.image"),
			  new JScrollPane(new TestGraphPanel(model,
							     m_resources)),
			  m_resources.getString("graphTab.tip"));

	final CumulativeStatisticsTableModel cumulativeModel =
	    new CumulativeStatisticsTableModel(model, true, m_resources);

	tabbedPane.addTab(m_resources.getString("cumulativeTableTab.title"),
			  m_resources.getImageIcon("cumulativeTableTab.image"),
			  new JScrollPane(new TestTable(cumulativeModel)),
			  m_resources.getString("cumulativeTableTab.tip"));

	final SampleStatisticsTableModel sampleModel =
	    new SampleStatisticsTableModel(model, m_resources);

	tabbedPane.addTab(m_resources.getString("sampleTableTab.title"),
			  m_resources.getImageIcon("sampleTableTab.image"),
			  new JScrollPane(new TestTable(sampleModel)),
			  m_resources.getString("sampleTableTab.tip"));

	final JPanel contentPanel = new JPanel();

	contentPanel.setLayout(new BorderLayout());
	contentPanel.add(hackToFixLayout, BorderLayout.WEST);
	contentPanel.add(tabbedPane, BorderLayout.CENTER);

	final ImageIcon logoIcon = m_resources.getImageIcon("logo.image");
	Image logoImage = null;

	if (logoIcon != null) {
	    final JLabel logo = new JLabel(logoIcon, SwingConstants.LEADING);
	    contentPanel.add(logo, BorderLayout.EAST);

	    logoImage = logoIcon.getImage();
	}

	// Create a panel to hold the tool bar and the test pane.
        final JPanel toolBarPanel = new JPanel();
	toolBarPanel.setLayout(new BorderLayout());
	toolBarPanel.add(createToolBar(), BorderLayout.NORTH);
	toolBarPanel.add(contentPanel, BorderLayout.CENTER);

	// Create the frame, containing the a menu and the top level pane.
	m_frame = new JFrame(m_resources.getString("title"));

        m_frame.addWindowListener(new WindowCloseAdapter());
	final Container topLevelPane= m_frame.getContentPane();
	topLevelPane.add(createMenuBar(), BorderLayout.NORTH);
        topLevelPane.add(toolBarPanel, BorderLayout.CENTER);

	if (logoImage != null) {
	    m_frame.setIconImage(logoImage);
	}

	m_model.addModelListener(this);
	update();

        m_frame.pack();

	// Arbitary sizing that looks good for Phil.
	final int minHeight = 480;
	final int maxHeight = 800;
	final Dimension d = m_frame.getSize();

	if (d.height > maxHeight) {
	    d.height = maxHeight;
	    m_frame.setSize(d);
	}
	else if (d.height < minHeight) {
	    d.height = minHeight;
	    m_frame.setSize(d);
	}

        m_frame.show();
    }

    private JMenuBar createMenuBar()
    {
	final JMenuBar menuBar = new JMenuBar();

	final Iterator menuBarIterator =
	    tokenise(m_resources.getString("menubar"));
	
	while (menuBarIterator.hasNext()) {
	    final String menuKey = (String)menuBarIterator.next();
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

	return menuBar;
    }

    private JToolBar createToolBar() 
    {
	final JToolBar toolBar = new JToolBar();
	
	final Iterator toolBarIterator =
	    tokenise(m_resources.getString("toolbar"));

	while (toolBarIterator.hasNext()) {
	    final String toolKey = (String)toolBarIterator.next();

	    if ("-".equals(toolKey)) {
		toolBar.addSeparator();
	    }
	    else {
		final JButton button = new JButton();
		button.putClientProperty("hideActionText", Boolean.TRUE);
		setAction(button, toolKey);
		toolBar.add(button);
	    }
	}

	return toolBar;
    }

    private void setAction(AbstractButton button, String actionKey)
    {
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

    private int updateStateLabel()
    {
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
	}
	else if (state == Model.STATE_STOPPED) {
	    if (receivedReport) {
		m_stateLabel.setText(m_stateStoppedAndIgnoringString);
	    }
	    else {
		m_stateLabel.setText(m_stateStoppedString);
	    }
	}
	else if (state == Model.STATE_CAPTURING) {
	    m_stateLabel.setText(m_stateCapturingString + sampleCount);
	}
	else {
	    m_stateLabel.setText(m_stateUnknownString);
	}

	return state;
    }

    /**
     * {@link ModelListener} interface. The test set has probably
     * changed. We need do nothing
     **/
    public void reset(Set newTests)
    {
    }

    /**
     * {@link ModelListener} interface.
     **/
    public void update()
    {
	final int state = updateStateLabel();

	if (state == Model.STATE_STOPPED) {
	    m_stopAction.stopped();
	}

	// Ignoring synchronisation issues for now.
	final int sampleInterval = m_model.getSampleInterval();
	final int ignoreCount = m_model.getIgnoreSampleCount();
	final int collectCount = m_model.getCollectSampleCount();

	m_intervalLabel.setText(m_sampleIntervalString + sampleInterval +
				(sampleInterval == 1 ? m_msUnit : m_msUnits));

	if (ignoreCount == 0 && m_ignoreCountZeroString != null) {
	    m_ignoreSampleLabel.setText(m_ignoreCountZeroString);
	}
	else {
	    m_ignoreSampleLabel.setText(m_ignoreCountString + ignoreCount +
					(ignoreCount == 1 ?
					 m_sampleUnit : m_sampleUnits));
	}

	if (collectCount == 0 && m_collectCountZeroString != null) {
	    m_collectSampleLabel.setText(m_collectCountZeroString);
	}
	else {
	    m_collectSampleLabel.setText(m_collectCountString + collectCount +
					 (collectCount == 1 ?
					  m_sampleUnit : m_sampleUnits));
	}
    }

    private static final class WindowCloseAdapter extends WindowAdapter
    {
	public void windowClosing(WindowEvent e)
	{
	    System.exit(0);
	}
    }

    private abstract class MyAction extends AbstractAction
    {
	protected final static String SET_ACTION_PROPERTY = "setAction";

	private final String m_key;
	private final Set m_propertyChangeListenersByButton = new HashSet();

	public MyAction(String key) 
	{
	    super();

	    m_key = key;

	    final String label =
		m_resources.getString(m_key + ".label", false);

	    if (label != null) {
		putValue(Action.NAME, label);
	    }

	    final String tip = m_resources.getString(m_key + ".tip", false);

	    if (tip != null) {
		putValue(Action.SHORT_DESCRIPTION, tip);
	    }

	    final ImageIcon imageIcon =
		m_resources.getImageIcon(m_key + ".image");
	    
	    if (imageIcon != null) {
		putValue(Action.SMALL_ICON, imageIcon);
	    }
	}

	public String getKey()
	{
	    return m_key;
	}

	public void registerButton(final AbstractButton button) 
	{
	    if (!m_propertyChangeListenersByButton.contains(button)) {
		addPropertyChangeListener(
		    new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
			    if (e.getPropertyName().equals(
				    SET_ACTION_PROPERTY)) {

				final MyAction newAction =
				    (MyAction)e.getNewValue();

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

    private class SaveAction extends MyAction
    {
	private final JFileChooser m_fileChooser = 
	    new JFileChooser(new File("."));

	SaveAction()
	{
	    super("save");

	    m_fileChooser.setDialogTitle(m_resources.getString("save.label"));
	    m_fileChooser.setSelectedFile(new File(m_resources.getString(
						       "default.filename")));
	}

        public void actionPerformed(ActionEvent event)
	{
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
							   false,
							   m_resources);
		    model.update();

		    try {
			final FileWriter writer = new FileWriter(file);
			model.write(writer, ",",
				    System.getProperty("line.separator"));
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
    
    private class ExitAction extends MyAction
    {
	ExitAction()
	{
	    super("exit");
	}

        public void actionPerformed(ActionEvent e)
	{
	    System.exit(0);
	}
    }

    private class StartAction extends MyAction
    {
	StartAction()
	{
	    super("start");
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_model.start();

	    //  putValue() won't work here as the event won't // fire
	    //  if the value doesn't change.
	    firePropertyChange(SET_ACTION_PROPERTY, null, m_stopAction);
	    updateStateLabel();
	}
    }

    private class StopAction extends MyAction
    {
	StopAction()
	{
	    super("stop");
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_model.stop();
	    stopped();
	}

	public void stopped()
	{
	    //  putValue() won't work here as the event won't // fire
	    //  if the value doesn't change.
	    firePropertyChange(SET_ACTION_PROPERTY, null, m_startAction);
	    updateStateLabel();
	}
    }

    private class StartProcessesGrinderAction extends MyAction
    {
	private final ActionListener m_delegateAction;

	StartProcessesGrinderAction(ActionListener delegateAction)
	{
	    super("start-processes");
	    m_delegateAction = delegateAction;
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_delegateAction.actionPerformed(e);
	}
    }

    private class ResetProcessesGrinderAction extends MyAction
    {
	private final ActionListener m_delegateAction;

	ResetProcessesGrinderAction(ActionListener delegateAction)
	{
	    super("reset-processes");
	    m_delegateAction = delegateAction;
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_delegateAction.actionPerformed(e);
	}
    }

    private class StopProcessesGrinderAction extends MyAction
    {
	private final ActionListener m_delegateAction;

	StopProcessesGrinderAction(ActionListener delegateAction)
	{
	    super("stop-processes");
	    m_delegateAction = delegateAction;
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_delegateAction.actionPerformed(e);
	}
    }

    private static Iterator tokenise(String string)
    {
	final LinkedList list = new LinkedList();

	final StringTokenizer t = new StringTokenizer(string);

	while (t.hasMoreTokens()) {
	    list.add(t.nextToken());
	}

	return list.iterator();
    }
}
