// Copyright (C) 2003, 2004 Philip Aston
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.grinder.console.common.Resources;


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

  private File m_distributionDirectory = new File(".").getAbsoluteFile();

  /** Synchronise on m_listeners before accessing. */
  private final List m_listeners = new LinkedList();

  public ScriptFilesPanel(final JFrame frame, LookAndFeel lookAndFeel,
                          Resources resources,
                          final ActionListener distributeFilesHandler) {

    m_resources = resources;

    m_fileChooser.setDialogTitle(
      m_resources.getString("choose-directory.tip"));
    m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    lookAndFeel.addListener(new LookAndFeel.ComponentListener(m_fileChooser));

    final JButton chooseDirectoryButton = new CustomJButton();

    chooseDirectoryButton.setAction(
      new CustomAction(m_resources, "choose-directory") {
        public void actionPerformed(ActionEvent event) {
          try {
            if (m_fileChooser.showOpenDialog(frame) ==
                JFileChooser.APPROVE_OPTION) {

              final File file = m_fileChooser.getSelectedFile();

              if (!file.exists()) {
                if (JOptionPane.showConfirmDialog(
                      frame,
                      m_resources.getString("createDirectory.text"),
                      file.toString(), JOptionPane.YES_NO_OPTION) ==
                    JOptionPane.NO_OPTION) {
                  return;
                }

                file.mkdir();
              }

              m_distributionDirectory = file;
              refresh();
            }
          }
          catch (Exception e) {
            new ErrorDialogHandler(frame, m_resources).handleException(e);
          }
        }
      });

    final JButton distributeFilesButton = new CustomJButton();

    distributeFilesButton.setAction(
      new CustomAction(m_resources, "distribute-files") {
        public void actionPerformed(ActionEvent event) {
          distributeFilesHandler.actionPerformed(event);
        }
      });

    final JPanel distributionDirectoryPanel = new JPanel();
    distributionDirectoryPanel.setLayout(
      new BoxLayout(distributionDirectoryPanel, BoxLayout.X_AXIS));
    distributionDirectoryPanel.add(chooseDirectoryButton);
    distributionDirectoryPanel.add(distributeFilesButton);
    distributionDirectoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    refresh();

    final JTree tree = new JTree(m_fileTreeModel);
    tree.setCellRenderer(new CustomTreeCellRenderer());
    tree.getSelectionModel().setSelectionMode(
      TreeSelectionModel.SINGLE_TREE_SELECTION);

    tree.addTreeSelectionListener(
      new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
          final FileTreeModel.Node node =
            (FileTreeModel.Node)tree.getLastSelectedPathComponent();

          if (node != null && node instanceof FileTreeModel.FileNode) {
            fireFileSelected((FileTreeModel.FileNode) node);
          }
        }
      }
      );

    final JScrollPane fileTreePane = new JScrollPane(tree);
    fileTreePane.setAlignmentX(Component.LEFT_ALIGNMENT);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(distributionDirectoryPanel);
    add(fileTreePane);
  }

  public void refresh() {
    m_fileChooser.setCurrentDirectory(m_distributionDirectory);
    m_fileTreeModel.setRootDirectory(m_distributionDirectory);
  }

  private String limitLength(String s) {

    final String ellipses = "...";
    final int maximumLength = 25 - ellipses.length();

    if (s.length() > maximumLength) {
      return ellipses + s.substring(s.length() - maximumLength);
    }

    return s;
  }

  /**
   * Custom cell renderer.
   */
  final class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
    private final DefaultTreeCellRenderer m_defaultRenderer =
      new DefaultTreeCellRenderer();

    private final Font m_boldFont;
    private final Font m_boldItalicFont;

    CustomTreeCellRenderer() {
      m_boldFont = new JLabel().getFont().deriveFont(Font.BOLD);
      m_boldItalicFont = m_boldFont.deriveFont(Font.BOLD | Font.ITALIC);
    }

    public Component getTreeCellRendererComponent(
      JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus) {

      final FileTreeModel.Node node = (FileTreeModel.Node)value;

      if (node instanceof FileTreeModel.FileNode) {
        final FileTreeModel.FileNode fileNode = (FileTreeModel.FileNode)node;

        setLeafIcon(
          fileNode.isPythonFile() ?
          m_resources.getImageIcon("script.pythonfile.image") :
          m_defaultRenderer.getLeafIcon());

        setTextNonSelectionColor(
          fileNode.isBoringFile() && !fileNode.isOpen() ?
          Colours.INACTIVE_TEXT :
          m_defaultRenderer.getTextNonSelectionColor());

        if (fileNode.isDirty()) {
          setFont(m_boldItalicFont);
        }
        else if (fileNode.isOpen()) {
          setFont(m_boldFont);
        }
        else {
          setFont(m_defaultRenderer.getFont());
        }

        return super.getTreeCellRendererComponent(
          tree, value, selected, expanded, leaf, row, hasFocus);
      }
      else {
        return m_defaultRenderer.getTreeCellRendererComponent(
          tree, value, selected, expanded, leaf, row, hasFocus);
      }
    }

    /**
     * Our parent overrides validate() and revalidate() for speed.
     * This means it never resizes. Go with this, but be a few pixels
     * wider to allow text to be italicised.
     */
    public Dimension getPreferredSize() {
      final Dimension result = super.getPreferredSize();

      return result != null ?
        new Dimension(result.width + 3, result.height) : null;
    }
  }

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  public void addListener(Listener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  private void fireFileSelected(FileTreeModel.FileNode fileNode) {
    synchronized (m_listeners) {
      final Iterator iterator = m_listeners.iterator();

      while (iterator.hasNext()) {
        final Listener listener = (Listener)iterator.next();
        listener.newFileSelection(fileNode);
      }
    }
  }

  /**
   * Interface for listeners.
   */
  public interface Listener extends EventListener {

    /**
     * Called when a new file node has been selected.
     *
     * @param fileNode The file node.
     */
    void newFileSelection(FileTreeModel.FileNode fileNode);
  }
}


