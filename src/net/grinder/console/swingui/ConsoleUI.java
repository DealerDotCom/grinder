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
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
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
import net.grinder.statistics.Statistics;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ConsoleUI implements ModelListener
{
    private static ResourceBundle s_resources = null;

    private final static Font s_tpsFont =
	new Font("helvetica", Font.ITALIC | Font.BOLD, 40);

    private final static NumberFormat s_twoDPFormat =
	new DecimalFormat("0.00");

    private final HashMap m_actionTable = new HashMap();
    private final Model m_model;
    private final JLabel m_collectSampleLabel;
    private final JLabel m_ignoreSampleLabel;
    private final JLabel m_intervalLabel;
    private final JLabel m_stateLabel;
    private final JFrame m_frame;

    public ConsoleUI(Model model, ActionListener startProcessesHandler,
		     ActionListener stopProcessesHandler)
	throws ConsoleException
    {
	getResources();

	final Action[] actions = {
	    new StartProcessesGrinderAction(startProcessesHandler),
	    new StopProcessesGrinderAction(stopProcessesHandler),
	    new StartAction(),
	    new StopAction(),
	    new SaveAction(),
	    new ExitAction(),
	};

	for (int i=0; i<actions.length; i++) {
	    m_actionTable.put(actions[i].getValue(Action.NAME), actions[i]);
	}

	m_model = model;

	final LabelledGraph totalGraph = new LabelledGraph("Total",
							   Color.darkGray);

	final JLabel tpsLabel = new JLabel();
	tpsLabel.setForeground(Color.black);
	tpsLabel.setFont(s_tpsFont);

	m_model.addTotalSampleListener(
	    new SampleListener() {
		    public void update(double tps, double average,
				       double peak, Statistics total) {
			tpsLabel.setText(s_twoDPFormat.format(tps) + " TPS");
			totalGraph.add(tps, average, peak, total);
		    }
		}
	    );

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

	m_stateLabel = new JLabel();

	final JPanel controlPanel = new JPanel();
	controlPanel.setLayout(new GridLayout(0, 1));
	controlPanel.add(m_intervalLabel);
	controlPanel.add(intervalSlider);
	controlPanel.add(textFieldPanel);
	controlPanel.add(m_stateLabel);
	controlPanel.setBorder(new EmptyBorder(0, 10, 0, 10));

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

	tabbedPane.addTab(getResourceString("graphTab.title"),
			  getImageIcon("graphTab.image"),
			  new JScrollPane(new TestGraphPanel(model)),
			  getResourceString("graphTab.tip"));

	tabbedPane.addTab(getResourceString("tableTab.title"),
			  getImageIcon("tableTab.image"),
			  new JScrollPane(new TestTable(model)),
			  getResourceString("tableTab.tip"));

	final JPanel contentPanel = new JPanel();

	contentPanel.setLayout(new BorderLayout());
	contentPanel.add(hackToFixLayout, BorderLayout.WEST);
	contentPanel.add(tabbedPane, BorderLayout.CENTER);

	final ImageIcon logoIcon = getImageIcon("logo.image");
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
	m_frame = new JFrame(getResourceString("title"));

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
	    tokenise(getResourceString("menubar"));
	
	while (menuBarIterator.hasNext()) {
	    final String menuKey = (String)menuBarIterator.next();
	    final JMenu menu =
		new JMenu(getResourceString(menuKey + ".menu.label"));

	    final Iterator menuIterator =
		tokenise(getResourceString(menuKey + ".menu"));

	    while (menuIterator.hasNext()) {
		final String menuItemKey = (String)menuIterator.next();

		if ("-".equals(menuItemKey)) {
		    menu.addSeparator();
		}
		else {
		    final JMenuItem menuItem =
			new JMenuItem(
			    getResourceString(menuItemKey + ".label"));

		    final ImageIcon imageIcon =
			getImageIcon(menuItemKey + ".image");

		    if (imageIcon != null) {
			menuItem.setIcon(imageIcon);
		    }

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
	    tokenise(getResourceString("toolbar"));
	
	while (toolBarIterator.hasNext()) {
	    final String toolKey = (String)toolBarIterator.next();

	    if ("-".equals(toolKey)) {
		toolBar.addSeparator();
	    }
	    else {
		final ImageIcon imageIcon = getImageIcon(toolKey + ".image");

		if (imageIcon != null) {
		    final JButton button = new JButton(imageIcon);
		
		    setAction(button, toolKey);

		    final String tipString =
			getResourceString(toolKey + ".tip", false);

		    if (tipString != null) {
			button.setToolTipText(tipString);
		    }

		    toolBar.add(button);
		}
	    }
	}

	return toolBar;
    }

    private void setAction(AbstractButton  button, String resourceKey)
    {
	final String actionString = getResourceString(resourceKey + ".action");
	final Action action = (Action)m_actionTable.get(actionString);

	if (action != null) {
	    action.addPropertyChangeListener(
		new ActionChangedListener(button));

	    button.addActionListener(action);
	    button.setActionCommand(actionString);
	    button.setEnabled(action.isEnabled());
	}
	else {
	    System.err.println("Action '" + actionString + "' not found");
	    button.setEnabled(false);
	}
    }

    public void update()
    {
	// Ignoring synchronisation issues for now.
	final int state = m_model.getState();
	final long sampleInterval = m_model.getSampleInterval();
	final long sampleCount = m_model.getSampleCount();
	final int ignoreCount = m_model.getIgnoreSampleCount();
	final int collectCount = m_model.getCollectSampleCount();
	final boolean receivedSample = m_model.getRecievedSample();

	if (state == Model.STATE_WAITING_FOR_TRIGGER) {
	    if (receivedSample) {
		m_stateLabel.setText(
		    "Waiting for samples (ignoring: " + sampleCount + ")");
	    }
	    else {
		m_stateLabel.setText("Waiting for samples");
	    }
	}
	else if (state == Model.STATE_STOPPED) {
	    if (receivedSample) {
		m_stateLabel.setText(
		    "Collection stopped, ignoring samples");
	    }
	    else {
		m_stateLabel.setText("Collection stopped");
	    }
	}
	else if (state == Model.STATE_CAPTURING) {
	    m_stateLabel.setText("Collecting samples: " + sampleCount);
	}
	else {
	    m_stateLabel.setText("UNKNOWN STATE");
	}

	m_intervalLabel.setText("Sample interval: " + sampleInterval + " ms");

	if (ignoreCount == 0) {
	    m_ignoreSampleLabel.setText("Start on first sample");
	}
	else if (ignoreCount == 1) {
	    m_ignoreSampleLabel.setText("Ignore first sample");
	}
	else {
	    m_ignoreSampleLabel.setText("Ignore first " + ignoreCount +
					" samples");
	}

	if (collectCount == 0) {
	    m_collectSampleLabel.setText("Collect samples forever");
	}
	else if (collectCount == 1) {
	    m_collectSampleLabel.setText("Stop after 1 sample");
	}
	else {
	    m_collectSampleLabel.setText("Stop after " + collectCount +
					 " samples");
	}
    }

    private class ActionChangedListener implements PropertyChangeListener
    {
        final private AbstractButton m_button;
        
        ActionChangedListener(AbstractButton button)
	{
            super();
            m_button = button;
        }

        public void propertyChange(PropertyChangeEvent e) 
	{
            final String propertyName = e.getPropertyName();

            if (e.getPropertyName().equals(Action.NAME))
	    {
                final String text = (String)e.getNewValue();
                m_button.setText(text);
            }
	    else if (propertyName.equals("enabled"))
	    {
                final Boolean enabledState = (Boolean)e.getNewValue();
                m_button.setEnabled(enabledState.booleanValue());
            }
        }
    }

    private static final class WindowCloseAdapter extends WindowAdapter
    {
	public void windowClosing(WindowEvent e)
	{
	    System.exit(0);
	}
    }

    private class SaveAction extends AbstractAction
    {
	private final JFileChooser m_fileChooser = 
	    new JFileChooser(new File("."));

	SaveAction()
	{
	    super("save");

	    m_fileChooser.setDialogTitle(getResourceString("save.label"));
	    m_fileChooser.setSelectedFile(new File(getResourceString(
						       "default.filename")));
	}

        public void actionPerformed(ActionEvent event)
	{
	    if (m_fileChooser.showSaveDialog(m_frame) ==
		JFileChooser.APPROVE_OPTION) {
		final File file = m_fileChooser.getSelectedFile();

		if (file.exists() &&
		    JOptionPane.showConfirmDialog(
			m_frame,
			getResourceString("overwriteConfirmation.text"),
			file.toString(), JOptionPane.YES_NO_OPTION) ==
		    JOptionPane.NO_OPTION) {
		    return;
		}

		final StatisticsTableModel model =
		    new StatisticsTableModel(m_model, false);
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
			getResourceString("fileError.title"),
			JOptionPane.ERROR_MESSAGE);
		}
	    }
	}
    }

    private class ExitAction extends AbstractAction
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

    private class StartAction extends AbstractAction
    {
	StartAction()
	{
	    super("start");
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_model.start();
	}
    }

    private class StopAction extends AbstractAction
    {
	StopAction()
	{
	    super("stop");
	}

        public void actionPerformed(ActionEvent e)
	{
	    m_model.stop();
	}
    }

    private class StartProcessesGrinderAction extends AbstractAction
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

    private class StopProcessesGrinderAction extends AbstractAction
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

    private static void getResources()
	throws ConsoleException
    {
	try {
	    s_resources = ResourceBundle.getBundle(
		"net.grinder.console.swingui.resources.Console");
	}
	catch (MissingResourceException e) {
	    throw new ConsoleException("Resource bundle not found");
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

    private String getResourceString(String key)
    {
	return getResourceString(key, true);
    }

    private String getResourceString(String key, boolean warnIfMissing)
    {
	try {
	    return s_resources.getString(key);
	}
	catch (MissingResourceException e) {
	    if (warnIfMissing) {
		System.err.println(
		    "Warning - resource " + key + " not specified");
	    }
	    
	    return "";
	}
    }

    private URL getResource(String key)
    {
	return getResource(key, true);
    }

    private URL getResource(String key, boolean warnIfMissing)
    {
	final String name = getResourceString(key, true);

	if (name.length() == 0) {
	    return null;
	}
	
	final URL url = this.getClass().getResource("resources/" + name);

	if (warnIfMissing && url == null) {
	    System.err.println("Warning - could not load resource " + name);
	}

	return url;
    }

    private ImageIcon getImageIcon(String resourceName)
    {
	final URL resource = getResource(resourceName, false);

	return resource != null ? new ImageIcon(resource) : null;
    }
}
