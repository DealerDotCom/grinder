// Copyright (C) 2004 Philip Aston
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

import junit.framework.TestCase;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.grinder.console.common.Resources;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;

import net.grinder.console.editor.StringTextSource;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link CompositeTreeModel}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestCompositeTreeModel extends TestCase {

  public void testConstruction() throws Exception {
    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();
    assertTrue(compositeTreeModel instanceof TreeModel);
    assertNotNull(compositeTreeModel.getRoot());
    assertEquals(compositeTreeModel.getRoot(), compositeTreeModel.getRoot());

    final CompositeTreeModel compositeTreeModel2 = new CompositeTreeModel();
    AssertUtilities.assertNotEquals(compositeTreeModel.getRoot(),
                                    compositeTreeModel2.getRoot());
  }

  public void testGetChildMethods() throws Exception {
    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();

    final Object root = compositeTreeModel.getRoot();

    assertNull(compositeTreeModel.getChild(root, -10));
    assertNull(compositeTreeModel.getChild(root, 0));
    assertNull(compositeTreeModel.getChild(new Object(), 0));
    assertEquals(0, compositeTreeModel.getChildCount(root));
    assertEquals(0, compositeTreeModel.getChildCount(new Object()));

    final TreeModel delegateModel1 = createTreeModel();
    final DelegatingStubFactory delegateModelStubFactory1 =
      new DelegatingStubFactory(createTreeModel());
    final TreeModel instrumentedDelegateModel1 =
      (TreeModel)delegateModelStubFactory1.getStub();

    compositeTreeModel.addTreeModel(instrumentedDelegateModel1, true);

    assertSame(root, compositeTreeModel.getRoot());
    assertNull(compositeTreeModel.getChild(root, -10));

    // Identity not equal because of wrapping: compare node text
    // instead.
    assertEquals("Root", compositeTreeModel.getChild(root, 0).toString());
    assertNull(compositeTreeModel.getChild(root, 1));
    delegateModelStubFactory1.assertSuccess("getRoot");

    final Object delegateRoot1 = delegateModel1.getRoot();

    assertNull(compositeTreeModel.getChild(root, 1));
    assertEquals("Child2",
                 compositeTreeModel.getChild(delegateRoot1, 1).toString());
    delegateModelStubFactory1.assertSuccess("getChild", delegateRoot1,
                                            new Integer(1));

    assertEquals(1, compositeTreeModel.getChildCount(root));
    assertEquals(2, compositeTreeModel.getChildCount(delegateRoot1));
    delegateModelStubFactory1.assertSuccess("getChildCount", delegateRoot1);
    delegateModelStubFactory1.assertNoMoreCalls();

    final TreeModel delegateModel2 = createTreeModel();
    final DelegatingStubFactory delegateModelStubFactory2 =
      new DelegatingStubFactory(createTreeModel());
    final TreeModel instrumentedDelegateModel2 =
      (TreeModel)delegateModelStubFactory2.getStub();

    final Object delegateRoot2 = delegateModel1.getRoot();

    compositeTreeModel.addTreeModel(instrumentedDelegateModel2, false);
    assertEquals(3, compositeTreeModel.getChildCount(root));

    assertEquals("Child1",
                 compositeTreeModel.getChild(delegateRoot1, 0).toString());
    delegateModelStubFactory1.assertSuccess("getChild", delegateRoot1,
                                            new Integer(0));
    assertEquals("Child1", compositeTreeModel.getChild(root, 1).toString());
    assertEquals("Child2", compositeTreeModel.getChild(root, 2).toString());
  }

  private TreeModel createTreeModel() {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
    final DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child1");
    final DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child2");
    final DefaultMutableTreeNode grandChild =
      new DefaultMutableTreeNode("Grandchild");
    child1.add(grandChild);
    root.add(child1);
    root.add(child2);
    return new DefaultTreeModel(root);
  }
}
