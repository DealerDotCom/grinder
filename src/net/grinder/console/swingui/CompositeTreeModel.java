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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/**
 * TreeModel that combines other tree models.
 *
 * <p>The composed <code>TreeModel</code> implementations must be
 * aware of which nodes belong to them, and return null answers for
 * nodes that don't belong to them.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class CompositeTreeModel implements TreeModel {

  private final List m_wrappers = new ArrayList();
  private final EventListenerList m_listeners = new EventListenerList();

  private Object m_rootNode = new Object();

  CompositeTreeModel() {
  }

  void addTreeModel(TreeModel treeModel, boolean includeRoot) {
    if (includeRoot) {
      m_wrappers.add(new RootWrapper(treeModel));
    }
    else {
      m_wrappers.add(new FirstLevelWrapper(treeModel));
    }
  }

  public Object getRoot() {
    return m_rootNode;
  }

  public Object getChild(Object parent, int index) {
    if (index < 0) {
      return null;
    }
    else if (parent == m_rootNode) {
      int base = 0;

      final Iterator iterator = m_wrappers.iterator();

      while (iterator.hasNext()) {
        final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

        final int numberOfTopLevelNodes = wrapper.getNumberOfTopLevelNodes();

        if (index - base < numberOfTopLevelNodes) {
          return wrapper.getTopLevelNode(index - base);
        }

        base += numberOfTopLevelNodes;
      }
    }
    else {
      final Iterator iterator = m_wrappers.iterator();

      while (iterator.hasNext()) {
        final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

        final Object delegateAnswer =
          wrapper.getChild(parent, index);

        if (delegateAnswer != null) {
          return delegateAnswer;
        }
      }
    }

    return null;
  }

  public int getChildCount(Object parent) {
    if (parent == m_rootNode) {
      int answer = 0;

      final Iterator iterator = m_wrappers.iterator();

      while (iterator.hasNext()) {
        final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();
        answer += wrapper.getNumberOfTopLevelNodes();
      }

      return answer;
    }
    else {
      final Iterator iterator = m_wrappers.iterator();

      while (iterator.hasNext()) {
        final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

        final int delegateAnswer = wrapper.getChildCount(parent);

        if (delegateAnswer != 0) {
          return delegateAnswer;
        }
      }
    }

    return 0;
  }

  public int getIndexOfChild(Object parent, Object child) {

    if (parent == null || child == null) {
      // The TreeModel Javadoc says we should do this.
      return -1;
    }

    if (parent == m_rootNode) {
      int base = 0;

      final Iterator iterator = m_wrappers.iterator();

      while (iterator.hasNext()) {
        final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

        final int delegateAnswer = wrapper.getIndexOfTopLevelNode(child);

        if (delegateAnswer != -1) {
          return base + delegateAnswer;
        }

        base += wrapper.getNumberOfTopLevelNodes();
      }
    }
    else {
      final Iterator iterator = m_wrappers.iterator();

      while (iterator.hasNext()) {
        final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

        final int delegateAnswer =
          wrapper.getIndexOfChild(parent, child);

        if (delegateAnswer != -1) {
          return delegateAnswer;
        }
      }
    }

    return -1;
  }

  public boolean isLeaf(Object node) {
    final Iterator iterator = m_wrappers.iterator();

    while (iterator.hasNext()) {
      final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

      if (wrapper.isLeaf(node)) {
        return true;
      }
    }

    return false;
  }

  public void addTreeModelListener(TreeModelListener listener) {
    final Iterator iterator = m_wrappers.iterator();

    while (iterator.hasNext()) {
      final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

      wrapper.getModel().addTreeModelListener(listener);
    }

    m_listeners.add(TreeModelListener.class, listener);
  }

  public void removeTreeModelListener(TreeModelListener listener) {
    final Iterator iterator = m_wrappers.iterator();

    while (iterator.hasNext()) {
      final DelegateWrapper wrapper = (DelegateWrapper)iterator.next();

      wrapper.getModel().removeTreeModelListener(listener);
    }

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

  private abstract static class DelegateWrapper {
    private final TreeModel m_model;

    protected DelegateWrapper(TreeModel model) {
      m_model = model;
    }

    public abstract Object getTopLevelNode(int i);

    public abstract int getNumberOfTopLevelNodes();

    public abstract int getIndexOfTopLevelNode(Object node);

    public final TreeModel getModel() {
      return m_model;
    }

    public final Object getChild(Object parent, int index) {
      try {
        return getModel().getChild(parent, index);
      }
      catch (ClassCastException e) {
        return null;
      }
    }

    public int getChildCount(Object parent) {
      try {
        return getModel().getChildCount(parent);
      }
      catch (ClassCastException e) {
        return 0;
      }
    }

    public int getIndexOfChild(Object parent, Object child) {
      try {
        return getModel().getIndexOfChild(parent, child);
      }
      catch (ClassCastException e) {
        return -1;
      }
    }

    public final boolean isLeaf(Object node) {
      try {
        return m_model.isLeaf(node);
      }
      catch (ClassCastException e) {
        return false;
      }
    }
  }

  private static final class RootWrapper extends DelegateWrapper {
    public RootWrapper(TreeModel model) {
      super(model);
    }

    public Object getTopLevelNode(int i) {
      return i == 0 ? getModel().getRoot() : null;
    }

    public int getNumberOfTopLevelNodes() {
      return 1;
    }

    public int getIndexOfTopLevelNode(Object node) {
      return node.equals(getModel().getRoot()) ? 0 : -1;
    }
  }

  private static final class FirstLevelWrapper extends DelegateWrapper {
    public FirstLevelWrapper(TreeModel model) {
      super(model);
    }

    public Object getTopLevelNode(int i) {
      return this.getChild(getModel().getRoot(), i);
    }

    public int getNumberOfTopLevelNodes() {
      return this.getChildCount(getModel().getRoot());
    }

    public int getIndexOfTopLevelNode(Object node) {
      return this.getIndexOfChild(getModel().getRoot(), node);
    }
  }
}
