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

  public void setRootDirectory(File rootDirectory) {
    m_rootNode = new RootNode(rootDirectory);
    fireTreeStructureChanged();
  }

  public Object getRoot() {
    return m_rootNode;
  }

  public Object getChild(Object parent, int index) {

    return ((Node)parent).getChild(index);
  }

  public int getChildCount(Object parent) {

    return ((Node)parent).getChildCount();
  }

  public int getIndexOfChild(Object parent, Object child) {

    if (parent == null || child == null) {
      return -1;        // The TreeModel Javadoc says we should do
                        // this.
    }

    return ((Node)parent).getIndexOfChild((Node)child);
  }

  public boolean isLeaf(Object node) {
    return ((Node)node).isLeaf();
  }

  public void addTreeModelListener(TreeModelListener listener) {
    m_listeners.add(TreeModelListener.class, listener);
  }

  public void removeTreeModelListener(TreeModelListener listener) {
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

  public void valueForPathChanged(TreePath path, Object newValue) {
    // Do nothing.
  }

  private static final FilenameFilter s_directoryFilter =
    new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return new File(dir, name).isDirectory();
      }
    };

  private static final FilenameFilter s_fileFilter =
    new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return new File(dir, name).isFile();
      }
    };

  /**
   * Node in the tree.
   */
  public class Node {

    private final File m_file;
    private final boolean m_isDirectory;
    private final File[] m_childDirectories;
    private final File[] m_childFiles;

    protected Node(File file, boolean isDirectory) {
      m_file = file;
      m_isDirectory = isDirectory;
      final File[] childDirectories = file.listFiles(s_directoryFilter);

      m_childDirectories =
        childDirectories != null ? childDirectories : new File[0];

      final File[] childFiles = file.listFiles(s_fileFilter);

      m_childFiles = childFiles != null ? childFiles : new File[0];
    }

    public final Node getChild(int index) {
      if (index < m_childDirectories.length) {
        return new Node(m_childDirectories[index], true);
      }
      else {
        return new Node(m_childFiles[index - m_childDirectories.length],
                        false);
      }
    }

    public final int getChildCount() {
      return m_childDirectories.length + m_childFiles.length;
    }

    public final int getIndexOfChild(Node child) {
      for (int i = 0; i < m_childDirectories.length; ++i) {
        if (m_childDirectories[i].equals(child.getFile())) {
          return i;
        }
      }

      for (int i = 0; i < m_childFiles.length; ++i) {
        if (m_childFiles[i].equals(child.getFile())) {
          return m_childDirectories.length + i;
        }
      }

      return -1;
    }

    public final boolean isLeaf() {
      return !m_isDirectory;
    }

    public final boolean isPythonFile() {
      return !m_isDirectory && m_file.getName().endsWith(".py");
    }

    public final boolean isBoringFile() {
      final String name = m_file.getName();

      return !m_isDirectory &&
        (m_file.isHidden() ||
         name.endsWith(".class") ||
         name.endsWith("~") ||
         name.startsWith("."));
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
      super(file, true);
    }

    public String toString() {
      return getFile().getPath();
    }
  }
}
