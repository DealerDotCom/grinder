// Copyright (C) 2003 Philip Aston
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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.grinder.console.model.ScriptDistributionFiles;


/**
 * Panel containing controls for choosing script file set.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ScriptFilesPanel extends JPanel {

  private final Resources m_resources;
  private final JFileChooser m_fileChooser = new JFileChooser(".");

  private final JLabel m_rootDirectoryLabel = new JLabel();

  private ScriptDistributionFiles m_scriptDistributionFiles =
    new ScriptDistributionFiles();

  public ScriptFilesPanel(final JFrame frame, final Resources resources) {
    m_resources = resources;

    final JButton chooseDirectoryButton = new CustomJButton();

    chooseDirectoryButton.setBorderPainted(true);
    chooseDirectoryButton.setBorder(
      BorderFactory.createEmptyBorder(1, 1, 1, 1));

    m_scriptDistributionFiles.setRootDirectory(
      new File("").getAbsoluteFile());

    m_fileChooser.setDialogTitle(
      resources.getString("script.chooseDirectory.tip"));
    m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    chooseDirectoryButton.setAction(
      new CustomAction(resources, "script.chooseDirectory") {
	public final void actionPerformed(ActionEvent event) {
	  try {
	    if (m_fileChooser.showOpenDialog(frame) == 
		JFileChooser.APPROVE_OPTION) {

	      final File file = m_fileChooser.getSelectedFile();

	      if (!file.exists()) {
		if (JOptionPane.showConfirmDialog(
		      frame,
		      resources.getString("createDirectory.text"),
		      file.toString(), JOptionPane.YES_NO_OPTION) ==
		    JOptionPane.NO_OPTION) {
		  return;
		}

		file.mkdir();
	      }

	      m_scriptDistributionFiles.setRootDirectory(file);

	      refresh();
	    }
	  }
	  catch (Exception e) {
	    JOptionPane.showMessageDialog(
	      frame, e.getMessage(),
	      resources.getString("unexpectedError.title"),
	      JOptionPane.ERROR_MESSAGE);
	  }
	}
      }
      );

    m_rootDirectoryLabel.setBorder(
      BorderFactory.createEmptyBorder(5, 5, 0, 0));

    final Box rootDirectoryPanel = Box.createHorizontalBox();
    rootDirectoryPanel.add(chooseDirectoryButton);
    rootDirectoryPanel.add(m_rootDirectoryLabel);
    //    rootDirectoryPanel.setBorder(
    //      BorderFactory.createTitledBorder(
    //	resources.getString("script.directory.label")));

    setLayout(new GridLayout(0, 1));

    add(rootDirectoryPanel);

    refresh();
  }

  public final void refresh() {
    final File rootDirectory = m_scriptDistributionFiles.getRootDirectory();
    
    m_rootDirectoryLabel.setText(limitLength(rootDirectory.getPath()));
    m_fileChooser.setSelectedFile(rootDirectory);
  }

  private final String limitLength(String s) {

    final String ellipses = "...";
    final int maximumLength = 25 - ellipses.length();
    
    if (s.length() > maximumLength) {
      return ellipses + s.substring(s.length() - maximumLength);
    }

    return s;
  }
}

  
