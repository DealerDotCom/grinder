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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.grinder.console.model.editor.Buffer;
import net.grinder.console.model.editor.EditorModel;


/**
 * TreeModel that walks file system.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class FileTreeModel implements TreeModel {

  private final EditorModel m_editorModel;
  private final EventListenerList m_listeners = new EventListenerList();

  /**
   * Map from a File value to the latest Node to be created for the File.
   */
  private final WeakValueHashMap m_filesToNodes = new WeakValueHashMap();

  /**
   * Map from a Buffer to the FileNode that is associated with the
   * buffer.
   */
  private final WeakValueHashMap m_buffersToFileNodes = new WeakValueHashMap();

  private RootNode m_rootNode;

  FileTreeModel(EditorModel editorModel) {
    m_editorModel = editorModel;
  }

  public void setRootDirectory(File rootDirectory) {
    m_rootNode = new RootNode(rootDirectory);
    fireTreeStructureChanged();
  }

  public void refresh() {
    m_rootNode.refresh();
    fireTreeStructureChanged();
  }

  public Object getRoot() {
    return m_rootNode;
  }

  public Object getChild(Object parent, int index) {

    if (parent instanceof DirectoryNode) {
      final DirectoryNode directoryNode = (DirectoryNode)parent;

      if (directoryNode.belongsToModel(this)) {
        return directoryNode.getChild(index);
      }
    }

    return null;
  }

  public int getChildCount(Object parent) {

    if (parent instanceof DirectoryNode) {
      final DirectoryNode directoryNode = (DirectoryNode)parent;

      if (directoryNode.belongsToModel(this)) {
        return directoryNode.getChildCount();
      }
    }

    return 0;
  }

  public int getIndexOfChild(Object parent, Object child) {

    if (parent == null || child == null) {
      // The TreeModel Javadoc says we should do this.
      return -1;
    }

    if (parent instanceof DirectoryNode) {
      final DirectoryNode directoryNode = (DirectoryNode)parent;

      if (directoryNode.belongsToModel(this)) {
        return directoryNode.getIndexOfChild((Node)child);
      }
    }

    return -1;
  }

  public Node findNode(File file) {
    final Node existingNode = (Node)m_filesToNodes.get(file);

    if (existingNode != null) {
      return existingNode;
    }

    // Maybe its not been expanded. Lets try harder.
    final File[] paths = fileToArrayOfParentPaths(file);

    boolean treeStructureChanged = false;

    for (int i = 0; i < paths.length - 1; ++i) {
      final Node node = (Node)m_filesToNodes.get(paths[i]);

      if (node instanceof DirectoryNode) {
        final DirectoryNode directoryNode = (DirectoryNode)node;

        if (directoryNode.getChildForFile(paths[i + 1]) == null) {
          directoryNode.refresh();
          treeStructureChanged = true;

          if (directoryNode.getChildForFile(paths[i + 1]) == null) {
            return null;
          }
        }
      }
    }

    if (treeStructureChanged) {
      fireTreeStructureChanged();
    }

    return (Node)m_filesToNodes.get(file);
  }

  public File[] fileToArrayOfParentPaths(File file) {
    final List list = new ArrayList();

    File f = file;

    while (f != null) {
      list.add(f);
      f = f.getParentFile();
    }

    Collections.reverse(list);

    return (File[])list.toArray(new File[list.size()]);
  }

  public FileNode findFileNode(Buffer buffer) {
    return (FileNode)m_buffersToFileNodes.get(buffer);
  }

  public boolean isLeaf(Object node) {
    if (node instanceof FileNode) {
      final FileNode fileNode = (FileNode)node;

      if (fileNode.belongsToModel(this)) {
        return true;
      }
    }

    return false;
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
    System.err.println("valueForPathChanged(" + path + ", " + newValue + ")");
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
  public abstract class Node {

    private final File m_file;
    private final TreePath m_path;

    protected Node(File file) {
      this(null, file);
    }

    protected Node(Node parentNode, File file) {
      m_file = file;

      if (parentNode != null) {
        m_path = parentNode.getPath().pathByAddingChild(this);
      }
      else {
        m_path = new TreePath(this);
      }

      m_filesToNodes.put(file, this);
    }

    public String toString() {
      return m_file.getName();
    }

    public final File getFile() {
      return m_file;
    }

    public final TreePath getPath() {
      return m_path;
    }

    boolean belongsToModel(FileTreeModel model) {
      return FileTreeModel.this == model;
    }
  }

  /**
   * Node that represents a file. Used for the leaves of the tree.
   */
  public final class FileNode extends Node {

    private Buffer m_buffer;

    private FileNode(DirectoryNode parentNode, File file) {
      super(parentNode, file);

      setBuffer(m_editorModel.getBufferForFile(file));
    }

    public void setBuffer(Buffer buffer) {
      m_buffer = buffer;

      if (buffer != null) {
        m_buffersToFileNodes.put(buffer, this);
      }
    }

    public Buffer getBuffer() {
      return m_buffer;
    }

    public boolean isPythonFile() {
      return getFile().getName().endsWith(".py");
    }

    public boolean isBoringFile() {
      final String name = getFile().getName();

      return
        getFile().isHidden() ||
        name.endsWith(".class") ||
        name.endsWith("~") ||
        name.startsWith(".");
    }
  }

  /**
   * Node that represents a directory.
   */
  private class DirectoryNode extends Node {

    private File[] m_childDirectories = new File[0];
    private DirectoryNode[] m_childDirectoryNodes;
    private File[] m_childFiles = new File[0];
    private FileNode[] m_childFileNodes;

    DirectoryNode(DirectoryNode parentNode, File file) {
      super(parentNode, file);

      refresh();
    }

    public void refresh() {
      for (int i = 0; i < m_childDirectories.length; ++i) {
        final DirectoryNode oldDirectoryNode =
          (DirectoryNode)m_filesToNodes.remove(m_childDirectories[i]);

        if (oldDirectoryNode != null) {
          oldDirectoryNode.refresh();
        }
      }

      for (int i = 0; i < m_childFiles.length; ++i) {
        final FileNode oldFileNode =
          (FileNode)m_filesToNodes.remove(m_childFiles[i]);

        if (oldFileNode != null) {
          final Buffer buffer = (Buffer)oldFileNode.getBuffer();

          if (buffer != null) {
            m_buffersToFileNodes.remove(buffer);
          }
        }
      }

      m_childDirectories = getFile().listFiles(s_directoryFilter);
      m_childDirectoryNodes = new DirectoryNode[m_childDirectories.length];
      m_childFiles = getFile().listFiles(s_fileFilter);
      m_childFileNodes = new FileNode[m_childFiles.length];
    }

    final Node getChildForFile(File file) {
      if (file.isDirectory()) {
        for (int i = 0; i < m_childDirectories.length; ++i) {
          if (m_childDirectories[i].equals(file)) {
            return getChild(i);
          }
        }
      }
      else {
        for (int i = 0; i < m_childFiles.length; ++i) {
          if (m_childFiles[i].equals(file)) {
            return getChild(i + m_childDirectories.length);
          }
        }
      }

      // Not known here.
      return null;
    }

    public final Node getChild(int index) {
      if (index < m_childDirectories.length) {
        if (m_childDirectoryNodes[index] == null) {
          m_childDirectoryNodes[index] =
            new DirectoryNode(this, m_childDirectories[index]);
        }

        return m_childDirectoryNodes[index];
      }
      else {
        final int fileIndex = index - m_childDirectories.length;

        if (m_childFileNodes[fileIndex] == null) {
          m_childFileNodes[fileIndex] =
            new FileNode(this, m_childFiles[fileIndex]);
        }

        return m_childFileNodes[fileIndex];
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
  }

  /**
   * Root node of the tree.
   */
  private final class RootNode extends DirectoryNode {

    private RootNode(File file) {
      super(null, file);
    }

    public String toString() {
      return getFile().getPath();
    }
  }

  private static final class WeakValueHashMap  {
    private Map m_map = new HashMap();

    public void put(Object key, Object value) {
      m_map.put(key, new WeakReference(value));
    }

    public Object get(Object key) {
      final WeakReference reference = (WeakReference)m_map.get(key);
      return reference != null ? reference.get() : null;
    }

    public Object remove(Object key) {
      final WeakReference reference = (WeakReference)m_map.remove(key);
      return reference != null ? reference.get() : null;
    }
  }
}

