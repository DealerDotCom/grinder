// Copyright (C) 2005 Philip Aston
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

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.editor.StringTextSource;
import net.grinder.console.editor.TextSource;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;


public class TestFileTree extends AbstractFileTestCase {
  private static final Resources s_resources =
    new ResourcesImplementation(
      "net.grinder.console.swingui.resources.Console");
  private final RandomStubFactory m_errorHandlerStubFactory =
    new RandomStubFactory(ErrorHandler.class);
  private final ErrorHandler m_errorHandler =
    (ErrorHandler)m_errorHandlerStubFactory.getStub();

  private final StringTextSource.Factory m_stringTextSourceFactory =
    new StringTextSource.Factory();
  private final DelegatingStubFactory m_textSourceFactoryStubFactory =
    new DelegatingStubFactory(m_stringTextSourceFactory);
  private final TextSource.Factory m_textSourceFactory =
    (TextSource.Factory)m_textSourceFactoryStubFactory.getStub();
  private final RandomStubFactory m_agentCacheStateStubFactory =
    new RandomStubFactory(AgentCacheState.class);
  private final AgentCacheState m_agentCacheState =
    (AgentCacheState)m_agentCacheStateStubFactory.getStub();
  private final RandomStubFactory m_fileChangeWatcherStubFactory =
    new RandomStubFactory(FileChangeWatcher.class);
  private final FileChangeWatcher m_fileChangeWatcher =
    (FileChangeWatcher)m_fileChangeWatcherStubFactory.getStub();

  public void testConstruction() throws Exception {
    final EditorModel editorModel = new EditorModel(s_resources,
                                                    m_textSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);
    final FileTreeModel fileTreeModel = new FileTreeModel(editorModel);
    fileTreeModel.setRootDirectory(new File("c:"));

    final FileTree fileTree = new FileTree(s_resources,
      m_errorHandler, editorModel, bufferTreeModel, fileTreeModel);

    // Simulate L&F change.
    SwingUtilities.updateComponentTreeUI(fileTree.getComponent());

    assertNotNull(fileTree.getOpenFileAction());
    assertNotNull(fileTree.getSetScriptAction());
  }

  public void testEditorModelListener() throws Exception {
    final EditorModel editorModel = new EditorModel(s_resources,
                                                    m_textSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);
    final FileTreeModel fileTreeModel = new FileTreeModel(editorModel);

    fileTreeModel.setRootDirectory(new File("c:"));

    new FileTree(s_resources, m_errorHandler, editorModel, bufferTreeModel,
                 fileTreeModel);

    // Exercise the EditorModel listeners.
    editorModel.selectNewBuffer();
    editorModel.getSelectedBuffer().getTextSource().setText("Foo");
    editorModel.closeBuffer(editorModel.getSelectedBuffer());

    // Tests with files outside of the root directory.
    final File f1 = new File(getDirectory(), "file1");
    f1.createNewFile();
    final File f2 = new File(getDirectory(), "file2");
    f1.createNewFile();
    final Buffer buffer = editorModel.selectBufferForFile(f1);
    buffer.save(f2);
    editorModel.closeBuffer(buffer);

    final Buffer buffer2 = editorModel.selectBufferForFile(f1);
    fileTreeModel.setRootDirectory(getDirectory());
    fileTreeModel.refresh();

    editorModel.selectBufferForFile(f1);
    buffer2.save(f2);
    editorModel.selectBuffer(buffer2);

    buffer2.save(f1);
    fileTreeModel.refresh();     // Create new FileNodes.
    editorModel.selectBuffer(buffer2);

    editorModel.selectBufferForFile(f2);
    editorModel.closeBuffer(buffer2);
  }

  public void testDisplay() throws Exception {
    final EditorModel editorModel = new EditorModel(s_resources,
                                                    m_textSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);
    final FileTreeModel fileTreeModel = new FileTreeModel(editorModel);

    fileTreeModel.setRootDirectory(getDirectory());

    final FileTree fileTree =
      new FileTree(s_resources, m_errorHandler, editorModel,
                   bufferTreeModel, fileTreeModel);

    final JFrame frame = new JFrame();

    frame.getContentPane().add(fileTree.getComponent(), BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
    frame.dispose();
  }
}