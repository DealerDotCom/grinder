// Copyright (C) 2000 - 2008 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessStatus;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionHandler;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.ModelImplementation;
import net.grinder.console.model.ModelListener;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleListener;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsSet;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;


/**
 * Swing UI for console.
 *
 * Thinking about hacking this? See
 * http://madbean.com/blog/2004/17/totallygridbag.html first.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConsoleUI implements ModelListener {

  // Do not initialise any Swing components before the constructor has
  // set the Look and Feel to avoid loading the default Look and Feel
  // unnecessarily.
  private final LookAndFeel m_lookAndFeel;

  private final ActionTable m_actionTable = new ActionTable();
  private final CloseFileAction m_closeFileAction;
  private final StartAction m_startAction;
  private final ExitAction m_exitAction;
  private final StopAction m_stopAction;
  private final SaveFileAction m_saveFileAction;
  private final SaveFileAsAction m_saveFileAsAction;
  private final DistributeFilesAction m_distributeFilesAction;

  private final ModelImplementation m_model;
  private final ProcessControl m_processControl;
  private final FileDistribution m_fileDistribution;
  private final EditorModel m_editorModel;
  private final Resources m_resources;

  private final JFrame m_frame;
  private final FrameBounds m_frameBounds;
  private final JLabel m_stateLabel;
  private final SamplingControlPanel m_samplingControlPanel;
  private final FileTree m_fileTree;
  private final ErrorDialogHandler m_errorHandler;
  private final OptionalConfirmDialog m_optionalConfirmDialog;
  private final Font m_titleLabelFont;

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
   * @param processControl ProcessReport control.
   * @param fileDistribution File distribution.
   * @param resources Resources.
   * @exception ConsoleException if an error occurs
   */
  public ConsoleUI(ModelImplementation model,
                   ProcessControl processControl,
                   FileDistribution fileDistribution,
                   Resources resources)
    throws ConsoleException {

    m_model = model;
    m_processControl = processControl;
    m_fileDistribution = fileDistribution;
    m_resources = resources;

    final ConsoleProperties properties = m_model.getProperties();

    // Create the frame to contain the a menu and the top level pane.
    // Do before actions are constructed as we use the frame to create dialogs.
    m_frame = new JFrame(m_resources.getString("title"));

    m_errorHandler = new ErrorDialogHandler(m_frame, m_resources);

    final SwingDispatcherFactory swingDispatcherFactory =
      new SwingDispatcherFactory(m_errorHandler);

    // LookAndFeel constructor will set initial Look and Feel from properties.
    m_lookAndFeel = new LookAndFeel(properties, swingDispatcherFactory);

    m_errorHandler.registerWithLookAndFeel(m_lookAndFeel);

    m_editorModel = new EditorModel(m_resources,
                                    new Editor.TextSourceFactory(),
                                    m_fileDistribution.getAgentCacheState(),
                                    m_fileDistribution);

    m_editorModel.setExternalEditor(properties.getExternalEditorCommand(),
                                    properties.getExternalEditorArguments());
    m_editorModel.setSelectedPropertiesFile(properties.getPropertiesFile());

    properties.addPropertyChangeListener(
      new PropertyChangeListener()  {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals(
                ConsoleProperties.EXTERNAL_EDITOR_COMMAND_PROPERTY) ||
              e.getPropertyName().equals(
                ConsoleProperties.EXTERNAL_EDITOR_ARGUMENTS_PROPERTY)) {
            m_editorModel.setExternalEditor(
              properties.getExternalEditorCommand(),
              properties.getExternalEditorArguments());
          }
          else if (e.getPropertyName().equals(
                ConsoleProperties.PROPERTIES_FILE_PROPERTY)) {
            m_editorModel.setSelectedPropertiesFile(
              properties.getPropertiesFile());
          }
        }
      });

    m_optionalConfirmDialog =
      new OptionalConfirmDialog(m_frame, m_resources, properties);

    m_stateIgnoringString = m_resources.getString("state.ignoring.label") + ' ';
    m_stateWaitingString = m_resources.getString("state.waiting.label");
    m_stateStoppedString = m_resources.getString("state.stopped.label");
    m_stateStoppedAndIgnoringString =
      m_resources.getString("state.stoppedAndIgnoring.label") + ' ';
    m_stateCapturingString =
      m_resources.getString("state.capturing.label") + ' ';
    m_stateUnknownString = m_resources.getString("state.unknown.label");

    m_closeFileAction = new CloseFileAction();
    m_exitAction = new ExitAction();
    m_startAction = new StartAction();
    m_stopAction = new StopAction();
    m_saveFileAction = new SaveFileAction();
    m_saveFileAsAction = new SaveFileAsAction();
    m_distributeFilesAction = new DistributeFilesAction();

    m_actionTable.add(m_closeFileAction);
    m_actionTable.add(m_exitAction);
    m_actionTable.add(m_startAction);
    m_actionTable.add(m_stopAction);
    m_actionTable.add(m_saveFileAsAction);
    m_actionTable.add(m_distributeFilesAction);
    m_actionTable.add(new AboutAction(m_resources.getImageIcon("logo.image")));
    m_actionTable.add(new ChooseDirectoryAction());
    m_actionTable.add(new StartProcessesAction());
    m_actionTable.add(new NewFileAction());
    m_actionTable.add(new OptionsAction());
    m_actionTable.add(new ResetProcessesAction());
    m_actionTable.add(new SaveFileAction());
    m_actionTable.add(new SaveResultsAction());
    m_actionTable.add(new StopProcessesAction());

    m_stateLabel = new JLabel();
    m_samplingControlPanel = new SamplingControlPanel(m_resources);

    final JPanel controlAndTotalPanel = createControlAndTotalPanel();

    // Create the tabbed test display.
    final JTabbedPane tabbedPane = new JTabbedPane();

    final TestGraphPanel graphPanel =
      new TestGraphPanel(tabbedPane,
                         m_model,
                         m_resources,
                         swingDispatcherFactory);
    graphPanel.resetTestsAndStatisticsViews(); // Show logo.

    final JScrollPane graphTabPane =
      new JScrollPane(graphPanel,
                      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    graphTabPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("graphTab.title"),
                      m_resources.getImageIcon("graphTab.image"),
                      graphTabPane,
                      m_resources.getString("graphTab.tip"));

    m_titleLabelFont =
      new JLabel().getFont().deriveFont(Font.PLAIN | Font.ITALIC);

    m_cumulativeTableModel =
      new CumulativeStatisticsTableModel(m_model, m_resources);

    final JScrollPane cumulativeTablePane =
      new JScrollPane(new Table(m_cumulativeTableModel));

    cumulativeTablePane.setBorder(createTitledBorder("cumulativeTable.label"));
    cumulativeTablePane.setMinimumSize(new Dimension(100, 60));

    final SampleStatisticsTableModel sampleModel =
      new SampleStatisticsTableModel(m_model, m_resources);

    final JScrollPane sampleTablePane = new JScrollPane(new Table(sampleModel));
    sampleTablePane.setBorder(createTitledBorder("sampleTable.label"));
    sampleTablePane.setMinimumSize(new Dimension(100, 60));

    final JSplitPane resultsPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                  cumulativeTablePane,
                                                  sampleTablePane);

    resultsPane.setOneTouchExpandable(true);
    resultsPane.setResizeWeight(1.0d);
    resultsPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("resultsTab.title"),
                      m_resources.getImageIcon("resultsTab.image"),
                      resultsPane,
                      m_resources.getString("resultsTab.tip"));

    final ProcessStatusTableModel processStatusModel =
      new ProcessStatusTableModel(m_resources,
                                  m_processControl,
                                  swingDispatcherFactory);

    final JScrollPane processStatusPane =
      new JScrollPane(new Table(processStatusModel));

    processStatusPane.setBorder(
      createTitledBorder("processStatusTableTab.tip"));

    tabbedPane.addTab(m_resources.getString("processStatusTableTab.title"),
                      m_resources.getImageIcon(
                        "processStatusTableTab.image"),
                      processStatusPane,
                      m_resources.getString("processStatusTableTab.tip"));

    final JToolBar editorToolBar = new JToolBar();
    new ToolBarAssembler(editorToolBar, true).populate("editor.toolbar");

    final Font editorSmallFont =
      m_titleLabelFont.deriveFont(Font.PLAIN)
        .deriveFont(m_titleLabelFont.getSize2D() * 0.8f);

    final EditorControls editorControls =
      new EditorControls(
        m_resources, m_editorModel, editorSmallFont, editorToolBar);

    final Editor editor = new Editor(m_editorModel, m_saveFileAction);

    final FileTreeModel fileTreeModel =
      new FileTreeModel(m_editorModel,
                        m_fileDistribution.getDistributionFileFilter());
    fileTreeModel.setRootDirectory(
      properties.getDistributionDirectory().getFile());

    properties.addPropertyChangeListener(
      new PropertyChangeListener()  {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals(
                ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY)) {
            fileTreeModel.setRootDirectory(
              properties.getDistributionDirectory().getFile());
          }
        }
      });

    m_fileDistribution.addFileChangedListener(
      fileTreeModel.new RefreshChangedDirectoriesListener());

    final JPopupMenu fileTreePopupMenu = new JPopupMenu();

    m_fileTree = new FileTree(m_resources,
                              getErrorHandler(),
                              m_editorModel,
                              new BufferTreeModel(m_editorModel),
                              fileTreeModel,
                              editorSmallFont,
                              fileTreePopupMenu,
                              properties);

    final CustomAction[] fileTreeActions = m_fileTree.getActions();

    for (int i = 0; i < fileTreeActions.length; ++i) {
      m_actionTable.add(fileTreeActions[i]);
    }

    new PopupMenuAssembler(fileTreePopupMenu).populate(
      "editor.filetree.popupmenu");

    final JPanel editorPanel = new JPanel();
    editorPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    editorPanel.add(editorControls.getComponent());
    editorPanel.add(editor.getComponent());

    final JToolBar fileTreeToolBar = new JToolBar();
    new ToolBarAssembler(fileTreeToolBar, true).populate("filetree.toolbar");
    fileTreeToolBar.setFloatable(false);
    fileTreeToolBar.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JComponent fileTreeComponent = m_fileTree.getComponent();
    fileTreeComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
    fileTreeComponent.setPreferredSize(new Dimension(200, 100));

    final JPanel fileTreeControlPanel = new JPanel();
    fileTreeControlPanel.setLayout(
      new BoxLayout(fileTreeControlPanel, BoxLayout.Y_AXIS));
    fileTreeControlPanel.setBorder(BorderFactory.createEmptyBorder());
    fileTreeControlPanel.add(fileTreeToolBar);
    fileTreeControlPanel.add(fileTreeComponent);

    final JSplitPane scriptPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                 fileTreeControlPanel,
                                                 editorPanel);

    scriptPane.setOneTouchExpandable(true);
    scriptPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_resources.getString("scriptTab.title"),
                      m_resources.getImageIcon("scriptTab.image"),
                      scriptPane,
                      m_resources.getString("scriptTab.tip"));

    final JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(controlAndTotalPanel, BorderLayout.WEST);
    contentPanel.add(tabbedPane, BorderLayout.CENTER);

    // Create a panel to hold the tool bar and the test pane.
    final JPanel toolBarPanel = new JPanel(new BorderLayout());
    final JToolBar mainToolBar = new JToolBar();
    new ToolBarAssembler(mainToolBar, false).populate("main.toolbar");
    toolBarPanel.add(mainToolBar, BorderLayout.NORTH);
    toolBarPanel.add(contentPanel, BorderLayout.CENTER);

    m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    m_frame.addWindowListener(new WindowCloseAdapter());

    final Container topLevelPane = m_frame.getContentPane();
    final JMenuBar menuBar = new JMenuBar();
    new MenuBarAssembler(menuBar).populate("menubar");
    topLevelPane.add(menuBar, BorderLayout.NORTH);
    topLevelPane.add(toolBarPanel, BorderLayout.CENTER);

    final ImageIcon logoIcon = m_resources.getImageIcon("logo.image");

    if (logoIcon != null) {
      final Image logoImage = logoIcon.getImage();

      if (logoImage != null) {
        m_frame.setIconImage(logoImage);
      }
    }

    m_model.addModelListener(new SwingDispatchedModelListener(this));
    update();

    m_lookAndFeel.addListener(new LookAndFeelListener());

    m_frameBounds = new FrameBounds(properties, m_frame);
    m_frameBounds.restore();

    resultsPane.setDividerLocation(resultsPane.getMaximumDividerLocation());

    m_frame.setVisible(true);
  }

  private TitledBorder createTitledBorder(String titleResource) {
    final TitledBorder border =
      BorderFactory.createTitledBorder(
        BorderFactory.createEmptyBorder(3, 3, 3, 3),
        m_resources.getString(titleResource));

    border.setTitleFont(m_titleLabelFont);
    border.setTitleColor(Colours.HIGHLIGHT_TEXT);
    border.setTitleJustification(TitledBorder.RIGHT);

    return border;
  }

  private JPanel createControlAndTotalPanel() {
    final LabelledGraph totalGraph =
      new LabelledGraph(m_resources.getString("totalGraph.title"),
                        m_resources, Colours.DARK_GREY,
                        m_model.getTPSExpression(),
                        m_model.getPeakTPSExpression(),
                        m_model.getTestStatisticsQueries());

    final JLabel tpsLabel = new JLabel();
    tpsLabel.setForeground(Colours.BLACK);
    tpsLabel.setFont(new Font("helvetica", Font.ITALIC | Font.BOLD, 40));
    tpsLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

    m_model.addTotalSampleListener(
      new SampleListener() {
        private final String m_suffix =
          ' ' + m_resources.getString("tps.units");

        public void update(StatisticsSet intervalStatistics,
                           StatisticsSet cumulativeStatistics) {
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

    final JPanel statePanel = new JPanel();
    statePanel.setLayout(new BoxLayout(statePanel, BoxLayout.X_AXIS));
    statePanel.add(stateButton);
    statePanel.add(m_stateLabel);

    statePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    m_samplingControlPanel.add(Box.createRigidArea(new Dimension(0, 40)));
    m_samplingControlPanel.add(statePanel);

    m_samplingControlPanel.setBorder(
      BorderFactory.createEmptyBorder(10, 10, 0, 10));
    m_samplingControlPanel.setProperties(m_model.getProperties());

    final JPanel controlAndTotalPanel = new JPanel();
    controlAndTotalPanel.setLayout(
      new BoxLayout(controlAndTotalPanel, BoxLayout.Y_AXIS));

    m_samplingControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    tpsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    totalGraph.setAlignmentX(Component.LEFT_ALIGNMENT);

    controlAndTotalPanel.add(m_samplingControlPanel);
    controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 100)));
    controlAndTotalPanel.add(tpsLabel);
    controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 20)));
    controlAndTotalPanel.add(totalGraph);

    final JPanel hackToFixLayout = new JPanel();
    hackToFixLayout.add(controlAndTotalPanel);

    return hackToFixLayout;
  }

  private final class LookAndFeelListener
    extends LookAndFeel.ComponentListener {

    private LookAndFeelListener() {
      super(m_frame);
    }

    public void lookAndFeelChanged() {
      m_frame.setVisible(false);

      try {
        m_frameBounds.store();
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }

      super.lookAndFeelChanged();

      m_frameBounds.restore();
      m_frame.setVisible(true);
    }
  }

  private abstract class ListTokeniserTemplate {
    private final JComponent m_component;

    protected ListTokeniserTemplate(JComponent component) {
      m_component = component;
    }

    public void populate(String key) {
      final String tokens = m_resources.getString(key);
      final Iterator iterator =
        Collections.list(new StringTokenizer(tokens)).iterator();

      while (iterator.hasNext()) {
        final String itemKey = (String)iterator.next();

        if ("-".equals(itemKey)) {
          dash();
        }
        else if (">".equals(itemKey)) {
          greaterThan();
        }
        else {
          token(itemKey);
        }
      }
    }

    protected final JComponent getComponent() {
      return m_component;
    }

    protected void dash() { }
    protected void greaterThan() { }
    protected abstract void token(String key);
  }

  /** Work around polymorphic interface that's missing from Swing. */
  private abstract class AbstractMenuAssembler extends ListTokeniserTemplate {

    protected AbstractMenuAssembler(JComponent component) {
      super(component);
      new MnemonicHeuristics(component);
    }

    protected void token(String menuItemKey) {
      final JMenuItem menuItem = new JMenuItem() {

        public Dimension getPreferredSize() {
          final Dimension d = super.getPreferredSize();
          d.height = (int) (d.height * 0.9);
          return d;
        }
      };

      m_actionTable.setAction(menuItem, menuItemKey);

      final Icon icon = menuItem.getIcon();

      final Icon rolloverIcon =
        (Icon) menuItem.getAction().getValue(CustomAction.ROLLOVER_ICON);

      menuItem.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          menuItem.setIcon(menuItem.isArmed() ? rolloverIcon : icon);
        }
      });

      getComponent().add(menuItem);
    }
  }

  private final class MenuAssembler extends AbstractMenuAssembler {

    protected MenuAssembler(JMenu component) {
      super(component);
    }

    protected void dash() {
      ((JMenu)getComponent()).addSeparator();
    }
  }

  private final class PopupMenuAssembler extends AbstractMenuAssembler {

    protected PopupMenuAssembler(JPopupMenu component) {
      super(component);

      component.addContainerListener(new ContainerAdapter() {
        public void componentAdded(ContainerEvent e) {
          if (e.getChild() instanceof JMenuItem) {
            final JMenuItem menuItem = (JMenuItem)e.getChild();

            menuItem.setVisible(
              ((CustomAction)menuItem.getAction()).isRelevantToSelection());

            menuItem.getAction().addPropertyChangeListener(
              new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                  if (evt.getPropertyName().equals(
                    CustomAction.RELEVANT_TO_SELECTION)) {
                    menuItem.setVisible(
                      ((CustomAction)menuItem.getAction())
                      .isRelevantToSelection());
                  }
                }
              }
            );
          }
        }
      });
    }

    protected void dash() {
      ((JPopupMenu)getComponent()).addSeparator();
    }
  }

  private final class MenuBarAssembler extends ListTokeniserTemplate {

    protected MenuBarAssembler(JComponent component) {
      super(component);
      new MnemonicHeuristics(component);
    }

    protected void greaterThan() {
      getComponent().add(Box.createHorizontalGlue());
    }

    protected void token(String key) {
      final JMenu menu =
        new JMenu(m_resources.getString(key + ".menu.label"));

      new MenuAssembler(menu).populate(key + ".menu");

      getComponent().add(menu);
    }
  }

  private final class ToolBarAssembler extends ListTokeniserTemplate {

    private final boolean m_small;

    protected ToolBarAssembler(JComponent component, boolean small) {
      super(component);
      m_small = small;
    }

    protected void dash() {
      ((JToolBar)getComponent()).addSeparator();
    }

    protected void token(String key) {
      final JButton button = new CustomJButton();

      if (m_small) {
        button.setBorder(BorderFactory.createEmptyBorder());
      }

      getComponent().add(button);

      // Must set the action _after_ adding to the tool bar or the
      // rollover image isn't set correctly.
      m_actionTable.setAction(button, key);
    }
  }

  private static class ActionTable {
    private final Map m_map = new HashMap();

    public void add(CustomAction action) {
      m_map.put(action.getKey(), action);
    }

    public void setAction(AbstractButton button, String actionKey) {
      final CustomAction action = (CustomAction)m_map.get(actionKey);

      if (action != null) {
        button.setAction(action);
        action.registerButton(button);
      }
      else {
        System.err.println("Action '" + actionKey + "' not found");
        button.setEnabled(false);
      }
    }
  }

  private int updateStateLabel() {

    final int state = m_model.getState();
    final boolean receivedReport = m_model.getReceivedReport();
    final long sampleCount = m_model.getSampleCount();

    if (state == ModelImplementation.STATE_WAITING_FOR_TRIGGER) {
      if (receivedReport) {
        m_stateLabel.setText(m_stateIgnoringString + sampleCount);
      }
      else {
        m_stateLabel.setText(m_stateWaitingString);
      }
      m_stateLabel.setForeground(UIManager.getColor("Label.foreground"));
    }
    else if (state == ModelImplementation.STATE_STOPPED) {
      if (receivedReport) {
        m_stateLabel.setText(m_stateStoppedAndIgnoringString +
                             sampleCount);
      }
      else {
        m_stateLabel.setText(m_stateStoppedString);
      }

      m_stateLabel.setForeground(Colours.DARK_RED);
    }
    else if (state == ModelImplementation.STATE_CAPTURING) {
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

    if (newState != m_lastState &&
        newState == ModelImplementation.STATE_STOPPED) {
      m_stopAction.stopped();
    }

    m_lastState = newState;
  }

  /**
   * Called when a new statistic expression has been added to the model. We need
   * do nothing. {@link net.grinder.console.model.ModelListener} interface.
   *
   * @param statisticExpression
   *          The new statistic expression.
   */
  public void newStatisticExpression(ExpressionView statisticExpression) {
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface.
   * Existing <code>Test</code>s and <code>StatisticsView</code>s have
   * been discarded. We need do nothing.
   */
  public void resetTestsAndStatisticsViews() {
  }

  private final class WindowCloseAdapter extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      m_exitAction.exit();
    }
  }

  private final class SaveResultsAction extends CustomAction {
    private final JFileChooser m_fileChooser = new JFileChooser(".");

    SaveResultsAction() {
      super(m_resources, "save-results", true);

      m_fileChooser.setDialogTitle(
        MnemonicHeuristics.removeMnemonicMarkers(
          m_resources.getString("save-results.label")));

      m_fileChooser.setSelectedFile(
        new File(m_resources.getString("default.filename")));

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    public void actionPerformed(ActionEvent event) {
      if (m_fileChooser.showSaveDialog(m_frame) ==
        JFileChooser.APPROVE_OPTION) {

        final File file = m_fileChooser.getSelectedFile();

        if (file.exists() &&
            JOptionPane.showConfirmDialog(
              m_frame,
              m_resources.getString("overwriteConfirmation.text"),
              file.toString(),
              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
          return;
        }

        try {
          final FileWriter writer = new FileWriter(file);
          m_cumulativeTableModel.write(writer, "\t",
                                       System.getProperty("line.separator"));
          writer.close();
        }
        catch (IOException e) {
          UncheckedInterruptedException.ioException(e);
          getErrorHandler().handleErrorMessage(
            e.getMessage(),
            m_resources.getString("fileError.title"));
        }
      }
    }
  }

  private final class OptionsAction extends CustomAction {
    private final OptionsDialogHandler m_optionsDialogHandler;

    OptionsAction() {
      super(m_resources, "options", true);

      m_optionsDialogHandler =
        new OptionsDialogHandler(m_frame, m_lookAndFeel,
                                 m_model.getProperties(),
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

  private final class AboutAction extends CustomAction {

    private final ImageIcon m_logoIcon;

    AboutAction(ImageIcon logoIcon) {
      super(m_resources, "about", true);
      m_logoIcon = logoIcon;
    }

    public void actionPerformed(ActionEvent event) {

      final Resources resources = m_resources;

      final String title =
        MnemonicHeuristics.removeMnemonicMarkers(
          resources.getString("about.label"));
      final String aboutText = resources.getStringFromFile("about.text", true);

      final JEditorPane htmlPane = new JEditorPane("text/html", aboutText);
      htmlPane.setEditable(false);
      htmlPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      htmlPane.setBackground(new JLabel().getBackground());

      final JScrollPane contents =
        new JScrollPane(htmlPane,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
          public Dimension getPreferredSize() {
            final Dimension d = super.getPreferredSize();
            d.width = 500;
            d.height = 400;
            return d;
          }
        };

      htmlPane.setCaretPosition(0);

      JOptionPane.showMessageDialog(m_frame, contents, title,
                                    JOptionPane.PLAIN_MESSAGE,
                                    m_logoIcon);
    }
  }

  private final class ExitAction extends CustomAction {

    ExitAction() {
      super(m_resources, "exit");
    }

    public void actionPerformed(ActionEvent e) {
      exit();
    }

    void exit() {
      try {
        m_frameBounds.store();
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }

      final Buffer[] buffers = m_editorModel.getBuffers();

      for (int i = 0; i < buffers.length; ++i) {
        if (!m_closeFileAction.closeBuffer(buffers[i])) {
          return;
        }
      }

      System.exit(0);
    }
  }

  private final class StartAction extends CustomAction {
    StartAction() {
      super(m_resources, "start");
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
      super(m_resources, "stop");
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

  private final class NewFileAction extends CustomAction {
    public NewFileAction() {
      super(m_resources, "new-file");
    }

    public void actionPerformed(ActionEvent event) {
      m_editorModel.selectNewBuffer();
    }
  }

  private final class SaveFileAction extends CustomAction {
    public SaveFileAction() {
      super(m_resources, "save-file");

      m_editorModel.addListener(new EditorModel.AbstractListener() {
          public void bufferStateChanged(Buffer ignored) {
            final Buffer buffer = m_editorModel.getSelectedBuffer();

            setEnabled(buffer != null && buffer.isDirty());
          }
        });
    }

    public void actionPerformed(ActionEvent event) {
      try {
        final Buffer buffer = m_editorModel.getSelectedBuffer();

        if (buffer.getFile() != null) {
          if (!buffer.isUpToDate() &&
              JOptionPane.showConfirmDialog(
                m_frame,
                m_resources.getString(
                  "outOfDateOverwriteConfirmation.text"),
                buffer.getFile().toString(),
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
          }

          buffer.save();
        }
        else {
          m_saveFileAsAction.saveBufferAs(buffer);
        }
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }
    }
  }

  private final class SaveFileAsAction extends CustomAction {

    private final JFileChooser m_fileChooser = new JFileChooser(".");

    public SaveFileAsAction() {
      super(m_resources, "save-file-as", true);

      m_editorModel.addListener(new EditorModel.AbstractListener() {
          public void bufferStateChanged(Buffer ignored) {
            setEnabled(m_editorModel.getSelectedBuffer() != null);
          }
        });

      m_fileChooser.setDialogTitle(
        MnemonicHeuristics.removeMnemonicMarkers(
          m_resources.getString("save-file-as.label")));

      final String pythonFilesText =
        m_resources.getString("pythonScripts.label");

      m_fileChooser.addChoosableFileFilter(
        new FileFilter() {
          public boolean accept(File file) {
            return m_editorModel.isPythonFile(file) || file.isDirectory();
          }

          public String getDescription() {
            return pythonFilesText;
          }
        });

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    public void actionPerformed(ActionEvent event) {
      try {
        saveBufferAs(m_editorModel.getSelectedBuffer());
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }
    }

    void saveBufferAs(Buffer buffer) throws ConsoleException {
      final File currentFile = buffer.getFile();
      final Directory distributionDirectory =
        m_model.getProperties().getDistributionDirectory();

      if (currentFile != null) {
        m_fileChooser.setSelectedFile(currentFile);
      }
      else {
        m_fileChooser.setCurrentDirectory(distributionDirectory.getFile());
      }

      if (m_fileChooser.showSaveDialog(m_frame) !=
          JFileChooser.APPROVE_OPTION) {
        return;
      }

      final File file = m_fileChooser.getSelectedFile();

      if (!distributionDirectory.isParentOf(file) &&
        JOptionPane.showConfirmDialog(
          m_frame,
          m_resources.getString(
            "saveOutsideOfDistributionConfirmation.text"),
          (String) getValue(NAME),
          JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
        return;
      }

      if (!file.equals(currentFile)) {
        // Save as.
        final Buffer oldBuffer = m_editorModel.getBufferForFile(file);

        if (oldBuffer != null) {
          final ArrayList messages = new ArrayList();
          messages.add(
            m_resources.getString("ignoreExistingBufferConfirmation.text"));

          if (oldBuffer.isDirty()) {
            messages.add(
              m_resources.getString("existingBufferHasUnsavedChanges.text"));
          }

          if (!oldBuffer.isUpToDate()) {
            messages.add(
              m_resources.getString("existingBufferOutOfDate.text"));
          }

          messages.add(
            m_resources.getString("ignoreExistingBufferConfirmation2.text"));

          if (JOptionPane.showConfirmDialog(
                m_frame, messages.toArray(), file.toString(),
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
          }

          m_editorModel.closeBuffer(oldBuffer);
        }
        else {
          if (file.exists() &&
              JOptionPane.showConfirmDialog(
                m_frame,
                m_resources.getString("overwriteConfirmation.text"),
                file.toString(),
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
          }
        }
      }
      else {
        // We only need to check whether the current buffer is up to date
        // for Save, not Save As.
        if (!buffer.isUpToDate() &&
            JOptionPane.showConfirmDialog(
              m_frame,
              m_resources.getString("outOfDateOverwriteConfirmation.text"),
              buffer.getFile().toString(),
              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
          return;
        }
      }

      buffer.save(file);
    }
  }

  private final class CloseFileAction extends CustomAction {
    public CloseFileAction() {
      super(m_resources, "close-file");

      m_editorModel.addListener(new EditorModel.AbstractListener() {
          public void bufferStateChanged(Buffer ignored) {
            setEnabled(m_editorModel.getSelectedBuffer() != null);
          }
        });
    }

    public void actionPerformed(ActionEvent event) {
      closeBuffer(m_editorModel.getSelectedBuffer());
    }

    boolean closeBuffer(Buffer buffer) {
      if (buffer != null) {
        while (buffer.isDirty()) {
          // Loop until we've saved the buffer successfully or
          // canceled.

          final String confirmationMessage =
            MessageFormat.format(
              m_resources.getString(
                "saveModifiedBufferConfirmation.text"),
              new Object[] { buffer.getDisplayName() });

          final int chosen =
            JOptionPane.showConfirmDialog(m_frame,
                                          confirmationMessage,
                                          (String) getValue(NAME),
                                          JOptionPane.YES_NO_CANCEL_OPTION);

          if (chosen == JOptionPane.YES_OPTION) {
            try {
              if (buffer.getFile() != null) {
                buffer.save();
              }
              else {
                m_saveFileAsAction.saveBufferAs(buffer);
              }
            }
            catch (GrinderException e) {
              getErrorHandler().handleException(e);
              return false;
            }
          }
          else if (chosen == JOptionPane.NO_OPTION) {
            break;
          }
          else {
            return false;
          }
        }

        m_editorModel.closeBuffer(buffer);
      }

      return true;
    }
  }

  private abstract class AbstractEnableIfAgentsConnected
    implements ProcessStatus.Listener {

    private final Action m_action;

    AbstractEnableIfAgentsConnected(Action action) {
      m_action = action;
    }

    public final void update(ProcessStatus.ProcessReports[] processStatuses,
                             boolean newAgent) {
      enableOrDisable();
    }

    protected final void enableOrDisable() {
      m_action.setEnabled(shouldEnable());
    }

    protected abstract boolean shouldEnable();
  }

  private final class EnableIfAgentsConnected
    extends AbstractEnableIfAgentsConnected {

    EnableIfAgentsConnected(Action action) {
      super(action);
      enableOrDisable();
    }

    protected boolean shouldEnable() {
      return m_processControl.getNumberOfLiveAgents() > 0;
    }
  }

  private class StartProcessesAction extends CustomAction {

    StartProcessesAction() {
      super(m_resources, "start-processes");
      m_processControl.addProcessStatusListener(
        new EnableIfAgentsConnected(this));
    }

    public void actionPerformed(ActionEvent event) {
      try {
        final File propertiesFile = m_editorModel.getSelectedPropertiesFile();

        if (propertiesFile == null) {
          final int chosen =
            m_optionalConfirmDialog.show(
              m_resources.getString("propertiesNotSetConfirmation.text"),
              (String) getValue(NAME),
              JOptionPane.OK_CANCEL_OPTION,
              "propertiesNotSetAsk");

          if (chosen != JOptionPane.OK_OPTION &&
              chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
            return;
          }

          m_processControl.startWorkerProcesses(null);
        }
        else {
          if (m_editorModel.isABufferDirty()) {
            final int chosen =
              m_optionalConfirmDialog.show(
                m_resources.getString(
                  "startWithUnsavedBuffersConfirmation.text"),
                (String) getValue(NAME),
                JOptionPane.OK_CANCEL_OPTION,
                "startWithUnsavedBuffersAsk");

            if (chosen != JOptionPane.OK_OPTION &&
                chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
              return;
            }
          }

          if (m_fileDistribution.getAgentCacheState().getOutOfDate()) {
            final int chosen =
              m_optionalConfirmDialog.show(
                m_resources.getString("cachesOutOfDateConfirmation.text"),
                (String) getValue(NAME),
                JOptionPane.OK_CANCEL_OPTION,
                "distributeOnStartAsk");

            if (chosen != JOptionPane.OK_OPTION &&
                chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
              return;
            }

            m_distributeFilesAction.distribute();
          }

          final GrinderProperties properties =
            new GrinderProperties(propertiesFile);

          final File scriptFile =
            properties.getFile("grinder.script",
                               GrinderProperties.DEFAULT_SCRIPT);

          if (scriptFile == null) {
            getErrorHandler().handleErrorMessage(
              m_resources.getString("propertiesDoNotSpecifyScriptError.text"),
              (String) getValue(NAME));

            return;
          }
          else {
            final Directory directory =
              m_model.getProperties().getDistributionDirectory();

            if (directory.getRelativePath(scriptFile) == null) {
              getErrorHandler().handleErrorMessage(
                m_resources.getString("scriptNotInDirectoryError.text"),
                (String) getValue(NAME));

              return;
            }
          }

          m_processControl.startWorkerProcesses(properties);
        }
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
        return;
      }
    }
  }

  private final class ResetProcessesAction extends CustomAction {
    ResetProcessesAction() {
      super(m_resources, "reset-processes");
      m_processControl.addProcessStatusListener(
        new EnableIfAgentsConnected(this));
    }

    public void actionPerformed(ActionEvent event) {

      final ConsoleProperties properties = m_model.getProperties();

      try {
        final int chosen =
          m_optionalConfirmDialog.show(
            m_resources.getString(
              "resetConsoleWithProcessesConfirmation.text"),
            (String) getValue(NAME),
            JOptionPane.YES_NO_CANCEL_OPTION,
            "resetConsoleWithProcessesAsk");

        switch (chosen) {
        case JOptionPane.YES_OPTION:
          properties.setResetConsoleWithProcesses(true);
          break;

        case JOptionPane.NO_OPTION:
          properties.setResetConsoleWithProcesses(false);
          break;

        case OptionalConfirmDialog.DONT_ASK_OPTION:
          break;

        default:
          return;
        }
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
        return;
      }

      if (properties.getResetConsoleWithProcesses()) {
        m_model.reset();
      }

      m_processControl.resetWorkerProcesses();
    }
  }

  private final class StopProcessesAction extends CustomAction {
    StopProcessesAction() {
      super(m_resources, "stop-processes");
      m_processControl.addProcessStatusListener(
        new EnableIfAgentsConnected(this));
    }

    public void actionPerformed(ActionEvent event) {

      try {
        final int chosen =
          m_optionalConfirmDialog.show(
            m_resources.getString("stopProcessesConfirmation.text"),
            (String) getValue(NAME),
            JOptionPane.OK_CANCEL_OPTION,
            "stopProcessesAsk");

        if (chosen != JOptionPane.OK_OPTION &&
            chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
          return;
        }
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
        return;
      }

      m_processControl.stopAgentAndWorkerProcesses();
    }
  }

  private final class ChooseDirectoryAction extends CustomAction {
    private final JFileChooser m_fileChooser = new JFileChooser(".");

    ChooseDirectoryAction() {
      super(m_resources, "choose-directory", true);

      m_fileChooser.setDialogTitle(
        m_resources.getString("choose-directory.tip"));

      m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      m_fileChooser.setSelectedFile(
        m_model.getProperties().getDistributionDirectory().getFile());

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    public void actionPerformed(ActionEvent event) {
      try {
        final String title =
          MnemonicHeuristics.removeMnemonicMarkers(
            m_resources.getString("choose-directory.label"));

        if (m_fileChooser.showDialog(m_frame, title) ==
            JFileChooser.APPROVE_OPTION) {

          final Directory directory =
            new Directory(m_fileChooser.getSelectedFile());
          final File file = directory.getFile();

          if (!file.exists()) {
            if (JOptionPane.showConfirmDialog(
                  m_frame,
                  m_resources.getString("createDirectory.text"),
                  file.toString(),
                  JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
              return;
            }

            directory.create();
          }

          final ConsoleProperties properties = m_model.getProperties();
          properties.setAndSaveDistributionDirectory(directory);
        }
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        getErrorHandler().handleException(e);
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
      }
    }
  }

  private final class DistributeFilesAction extends CustomAction {

    DistributeFilesAction() {
      super(m_resources, "distribute-files");

      final AgentCacheState agentCacheState =
        m_fileDistribution.getAgentCacheState();

      agentCacheState.addListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ignored) {
            setEnabled(shouldEnable());
          }
        });

      m_processControl.addProcessStatusListener(
        new AbstractEnableIfAgentsConnected(this) {
          protected boolean shouldEnable() {
            return DistributeFilesAction.this.shouldEnable();
          }
        });

      setEnabled(shouldEnable());
    }

    private boolean shouldEnable() {
      return
        m_fileDistribution.getAgentCacheState().getOutOfDate() &&
        m_processControl.getNumberOfLiveAgents() > 0;
    }

    public void actionPerformed(ActionEvent event) {
      distribute();
    }

    public void distribute() {
      final FileDistributionHandler distributionHandler =
        m_fileDistribution.getHandler();

      final ProgressMonitor progressMonitor =
        new ProgressMonitor(m_frame, getValue(NAME), "", 0, 100);
      progressMonitor.setMillisToDecideToPopup(0);
      progressMonitor.setMillisToPopup(0);

      final Runnable distributionRunnable = new Runnable() {
          public void run() {
            while (!progressMonitor.isCanceled()) {
              try {
                final FileDistributionHandler.Result result =
                  distributionHandler.sendNextFile();

                if (result != null) {
                  progressMonitor.setProgress(result.getProgressInCents());
                  progressMonitor.setNote(result.getFileName());
                }
                else {
                  progressMonitor.close();
                  break;
                }
              }
              catch (FileContents.FileContentsException e) {
                // We don't want to put a dialog in the user's face
                // for every problem. Lets just log the the terminal
                // until we have a proper console log.
                e.printStackTrace();
              }
            }
          }
        };

      new Thread(distributionRunnable).start();
    }
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
