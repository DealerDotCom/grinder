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

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/**
 * TreeModel that walks file system.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class FileTreeModel implements TreeModel {

  private RootNode m_rootNode;
  private final EventListenerList m_listeners = new EventListenerList();

  FileTreeModel() {
    m_rootNode = null;
  }

  public final void setRootDirectory(File rootDirectory) {
    m_rootNode = new RootNode(rootDirectory);
    fireTreeStructureChanged();
  }

  public final Object getRoot() {
    return m_rootNode;
  }

  public final Object getChild(Object parent, int index) {

    return ((Node)parent).getChild(index);
  }

  public final int getChildCount(Object parent) {

    return ((Node)parent).getChildCount();
  }

  public final int getIndexOfChild(Object parent, Object child) {

    if (parent == null || child == null) {
      return -1;        // The TreeModel Javadoc says we
                // should do this.
    }

    return ((Node)parent).getIndexOfChild((Node)child);
  }

  public final boolean isLeaf(Object node) {
    return ((Node)node).isLeaf();
  }

  public final void addTreeModelListener(TreeModelListener listener) {
    m_listeners.add(TreeModelListener.class, listener);
  }

  public final void removeTreeModelListener(TreeModelListener listener) {
    m_listeners.remove(TreeModelListener.class, listener);
  }

  private void fireTreeStructureChanged() {

    final Object[] listeners = m_listeners.getListenerList();

    final TreeModelEvent event =
      new TreeModelEvent(this, new Object[] { getRoot() }, null, null);

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
    ((TreeModelListener)listeners[i + 1]).treeStructureChanged(event);
      }
    }
  }

  public final void valueForPathChanged(TreePath path, Object newValue) {
    // Do nothing.
  }

  private static final FilenameFilter s_directoryFilter =
    new FilenameFilter() {
      public final boolean accept(File dir, String name) {
    return new File(dir, name).isDirectory();
      }
    };

  private static final FilenameFilter s_fileFilter =
    new FilenameFilter() {
      public final boolean accept(File dir, String name) {
    return new File(dir, name).isFile();
      }
    };

  /**
   * Node in the tree.
   */
  public class Node {

    private final File m_file;
    private final File[] m_children;

    protected Node(File file) {
      m_file = file;
      final File[] childDirectories = file.listFiles(s_directoryFilter);
      final File[] childFiles = file.listFiles(s_fileFilter);

      final int numberOfDirectories =
    childDirectories != null ? childDirectories.length : 0;

      final int numberOfFiles =    childFiles != null ? childFiles.length : 0;

      m_children = new File[numberOfDirectories + numberOfFiles];

      if (numberOfDirectories != 0) {
    System.arraycopy(childDirectories, 0, m_children, 0,
             numberOfDirectories);
      }

      if (numberOfFiles != 0) {
    System.arraycopy(childFiles, 0, m_children, numberOfDirectories,
             numberOfFiles);
      }
    }

    public final Node getChild(int index) {
      return new Node(m_children[index]);
    }

    public final int getChildCount() {
      return m_children.length;
    }

    public final int getIndexOfChild(Node child) {
      for (int i = 0; i < m_children.length; ++i) {
    if (m_children[i].equals(child.getFile())) {
      return i;
    }
      }

      return -1;
    }

    public final boolean isLeaf() {
      return !m_file.isDirectory();
    }

    public final boolean isPythonFile() {
      return m_file.isFile() && m_file.getName().endsWith(".py");
    }

    public String toString() {
      return m_file.getName();
    }

    public final File getFile() {
      return m_file;
    }
  }

  /**
   * Root node of the tree.
   */
  public final class RootNode extends Node {

    private RootNode(File file) {
      super(file);
    }

    public String toString() {
      return getFile().getPath();
    }
  }
}
