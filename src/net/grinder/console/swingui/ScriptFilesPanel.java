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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import net.grinder.console.model.ScriptDistributionFiles;


/**
 * Panel containing controls for choosing script file set.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ScriptFilesPanel extends JPanel {

  private final Resources m_resources;
  private final JFileChooser m_fileChooser = new JFileChooser();
  private final FileTreeModel m_fileTreeModel = new FileTreeModel();

  private ScriptDistributionFiles m_scriptDistributionFiles =
    new ScriptDistributionFiles();

  public ScriptFilesPanel(final JFrame frame, final Resources resources) {
    m_resources = resources;

    final JButton chooseDirectoryButton = new CustomJButton();

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

    final JPanel rootDirectoryPanel = new JPanel();
    rootDirectoryPanel.setLayout(
      new BoxLayout(rootDirectoryPanel, BoxLayout.X_AXIS));
    rootDirectoryPanel.add(chooseDirectoryButton);
    rootDirectoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    refresh();

    final JTree tree = new JTree(m_fileTreeModel);
    tree.setCellRenderer(new CustomTreeCellRenderer());

    final JScrollPane fileTreePane = new JScrollPane(tree);
    fileTreePane.setAlignmentX(Component.LEFT_ALIGNMENT);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(rootDirectoryPanel);
    add(fileTreePane);
  }

  public final void refresh() {
    final File rootDirectory = m_scriptDistributionFiles.getRootDirectory();
    m_fileChooser.setCurrentDirectory(rootDirectory);
    m_fileTreeModel.setRootDirectory(rootDirectory);
  }

  private final String limitLength(String s) {

    final String ellipses = "...";
    final int maximumLength = 25 - ellipses.length();

    if (s.length() > maximumLength) {
      return ellipses + s.substring(s.length() - maximumLength);
    }

    return s;
  }

  final class CustomTreeCellRenderer implements TreeCellRenderer {
    private final DefaultTreeCellRenderer m_standardRenderer =
      new DefaultTreeCellRenderer();
    private final DefaultTreeCellRenderer m_pythonFileRenderer =
      new DefaultTreeCellRenderer();

    CustomTreeCellRenderer() {
      m_pythonFileRenderer.setLeafIcon(
	m_resources.getImageIcon("script.pythonfile.image"));
    }

    public final Component getTreeCellRendererComponent(
      JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus) {

      final FileTreeModel.Node node = (FileTreeModel.Node)value;

      if (node.isPythonFile()) {
	return m_pythonFileRenderer.getTreeCellRendererComponent(
	  tree, value, selected, expanded, leaf, row, hasFocus);
      }

      return m_standardRenderer.getTreeCellRendererComponent(
	tree, value, selected, expanded, leaf, row, hasFocus);
    }
  }
}


