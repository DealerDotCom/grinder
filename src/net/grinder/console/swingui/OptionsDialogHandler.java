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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.grinder.common.GrinderException;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.ConsoleProperties;


/**
 * Wrap up all the machinery used to show the options dialog.
 *
 * @author Philip Aston
 * @version $Revision$
 */
abstract class OptionsDialogHandler {
  private final JFrame m_frame;

  /** A working copy of console properties. **/
  private final ConsoleProperties m_properties;

  private final JTextField m_consoleAddress = new JTextField();
  private final IntegerField m_consolePort =
    new IntegerField(0, CommunicationDefaults.MAX_PORT);
  private final JTextField m_grinderAddress = new JTextField();
  private final IntegerField m_grinderPort =
    new IntegerField(0, CommunicationDefaults.MAX_PORT);
  private final SamplingControlPanel m_samplingControlPanel;
  private final JSlider m_sfSlider = new JSlider(1, 6, 1);
  private final JCheckBox m_resetConsoleWithProcessesCheckBox;
  private final JOptionPane m_optionPane;
  private final JOptionPaneDialog m_dialog;

  /**
   * Constructor.
   *
   * @param frame Parent frame.
   * @param properties A {@link
   * net.grinder.console.model.ConsoleProperties} associated with
   * the properties file to save to.
   * @param resources Resources object to use for strings and things.
   **/
  public OptionsDialogHandler(JFrame frame, ConsoleProperties properties,
			      final Resources resources) {
    m_frame = frame;
    m_properties = new ConsoleProperties(properties);

    final GridLayout addressLayout = new GridLayout(0, 2, 0, 1);
    addressLayout.setHgap(5);
    final JPanel addressPanel = new JPanel(addressLayout);
    addressPanel.add(
      new JLabel(resources.getString("consoleAddress.label")));
    addressPanel.add(m_consoleAddress);
    addressPanel.add(
      new JLabel(resources.getString("consolePort.label")));
    addressPanel.add(m_consolePort);

    addressPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    addressPanel.add(Box.createRigidArea(new Dimension(0, 10)));

    addressPanel.add(
      new JLabel(resources.getString("grinderAddress.label")));
    addressPanel.add(m_grinderAddress);
    addressPanel.add(
      new JLabel(resources.getString("grinderPort.label")));
    addressPanel.add(m_grinderPort);

    // Use an additional flow layout so the GridLayout doesn't
    // steal all the space.
    final JPanel communicationTab =
      new JPanel(new FlowLayout(FlowLayout.LEFT));
    communicationTab.add(addressPanel);

    m_samplingControlPanel = new SamplingControlPanel(resources);

    final JPanel samplingControlTab =
      new JPanel(new FlowLayout(FlowLayout.LEFT));
    samplingControlTab.add(m_samplingControlPanel);

    m_sfSlider.setMajorTickSpacing(1);
    m_sfSlider.setPaintLabels(true);
    m_sfSlider.setSnapToTicks(true);
    final Dimension d = m_sfSlider.getPreferredSize();
    d.width = 0;
    m_sfSlider.setPreferredSize(d);

    final JPanel sfPanel = new JPanel(new GridLayout(0, 2));
    sfPanel.add(
      new JLabel(resources.getString("significantFigures.label")));
    sfPanel.add(m_sfSlider);

    m_resetConsoleWithProcessesCheckBox =
      new JCheckBox(resources.getString("resetConsoleWithProcesses.label"));
    final JPanel checkBoxPanel = new JPanel();
    checkBoxPanel.add(m_resetConsoleWithProcessesCheckBox);

    final JPanel miscellaneousPanel = new JPanel(new GridLayout(0, 1));
    miscellaneousPanel.add(sfPanel);
    miscellaneousPanel.add(checkBoxPanel);

    final JPanel miscellaneousTab =
      new JPanel(new FlowLayout(FlowLayout.LEFT));
    miscellaneousTab.add(miscellaneousPanel);

    final JTabbedPane tabbedPane = new JTabbedPane();

    tabbedPane.addTab(resources.getString(
			"options.communicationTab.title"),
		      null, communicationTab,
		      resources.getString("options.communicationTab.tip"));

    tabbedPane.addTab(resources.getString("options.samplingTab.title"),
		      null, samplingControlTab,
		      resources.getString("options.samplingTab.tip"));

    tabbedPane.addTab(resources.getString(
			"options.miscellaneousTab.title"),
		      null, miscellaneousTab,
		      resources.getString("options.miscellaneousTab.tip"));


    final Object[] options = {
      resources.getString("options.ok.label"),
      resources.getString("options.cancel.label"),
      resources.getString("options.save.label"),
    };

    m_optionPane = new JOptionPane(tabbedPane, JOptionPane.PLAIN_MESSAGE,
				   0, null, options);

    // The SamplingControlPanel will automatically update m_properties.
    m_samplingControlPanel.setProperties(m_properties);

    m_dialog = new JOptionPaneDialog(m_frame, m_optionPane,
				     resources.getString("options.label"),
				     true) {

	protected boolean shouldClose() {
	  final Object value = m_optionPane.getValue();

	  if (value == options[1]) {
	    return true;
	  }
	  else {
	    try {
	      m_properties.setConsoleAddress(
		m_consoleAddress.getText());
	      m_properties.setConsolePort(
		m_consolePort.getValue());
	      m_properties.setGrinderAddress(
		m_grinderAddress.getText());
	      m_properties.setGrinderPort(
		m_grinderPort.getValue());
	      m_properties.setSignificantFigures(
		m_sfSlider.getValue());
	      m_properties.setResetConsoleWithProcesses(
		m_resetConsoleWithProcessesCheckBox.isSelected());
	    }
	    catch (ConsoleException e) {
	      JOptionPane.showMessageDialog(
		m_dialog, e.getMessage(),
		resources.getString("error.title"),
		JOptionPane.ERROR_MESSAGE);
	      return false;
	    }

	    if (value == options[2]) {
	      try {
		m_properties.save();
	      }
	      catch (GrinderException e) {
		final Throwable nested =
		  e.getNestedThrowable();

		JOptionPane.showMessageDialog(
		  m_dialog, (nested != null ? nested : e)
		  .getMessage(),
		  resources.getString("fileError.title"),
		  JOptionPane.ERROR_MESSAGE);

		return false;
	      }
	    }

	    // Success.
	    setNewOptions(m_properties);

	    return true;
	  }
	}
      };

    // Increase the width a bit so that all the tabs fit on a single
    // line.
    m_dialog.pack();
    final Dimension dialogSize = m_dialog.getSize();
    dialogSize.width += 50;
    m_dialog.setSize(dialogSize);
  }

  /**
   * Show the dialog.
   *
   * @param initialProperties A set of properties to initialise the
   * options with.
   **/
  public void showDialog(ConsoleProperties initialProperties) {
    m_properties.set(initialProperties);

    // Initialise input values.
    m_consoleAddress.setText(m_properties.getConsoleAddress());
    m_consolePort.setValue(m_properties.getConsolePort());
    m_grinderAddress.setText(m_properties.getGrinderAddress());
    m_grinderPort.setValue(m_properties.getGrinderPort());
    m_sfSlider.setValue(m_properties.getSignificantFigures());
    m_resetConsoleWithProcessesCheckBox.setSelected(
      m_properties.getResetConsoleWithProcesses());

    m_samplingControlPanel.refresh();

    m_dialog.setLocationRelativeTo(m_frame);
    m_dialog.setVisible(true);
  }

  /**
   * User should override this to handle new options set by the dialog.
   **/
  protected abstract void setNewOptions(ConsoleProperties newOptions);
}
