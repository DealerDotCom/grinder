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

package net.grinder.console.model.editor;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;

import net.grinder.console.common.Resources;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.CallRecorder;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test for {@link EditorModel}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestEditorModel extends AbstractFileTestCase {

  private static final Resources s_resources =
      new Resources("net.grinder.console.swingui.resources.Console");

  public void testConstruction() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final DelegatingStubFactory textSourceFactoryStubFactory =
      new DelegatingStubFactory(stringTextSourceFactory);
    final TextSource.Factory textSourceFactory =
      (TextSource.Factory)textSourceFactoryStubFactory.getStub();

    final EditorModel editorModel =
      new EditorModel(s_resources, textSourceFactory);

    textSourceFactoryStubFactory.assertSuccess("create");
    textSourceFactoryStubFactory.assertNoMoreCalls();
    assertNotNull(stringTextSourceFactory.getLast().getText());
    assertNull(editorModel.getSelectedBuffer());
  }

  public void testSelectDefaultBuffer() throws Exception {

    final EditorModel editorModel =
      new EditorModel(s_resources, new StringTextSource.Factory());

    final RandomStubFactory listener1StubFactory =
      new RandomStubFactory(EditorModel.Listener.class);
    final EditorModel.Listener listener1 =
      (EditorModel.Listener)listener1StubFactory.getStub();

    final RandomStubFactory listener2StubFactory =
      new RandomStubFactory(EditorModel.Listener.class);
    final EditorModel.Listener listener2 =
      (EditorModel.Listener)listener2StubFactory.getStub();

    editorModel.addListener(listener1);
    editorModel.addListener(listener2);

    editorModel.selectDefaultBuffer();

    assertNotNull(editorModel.getSelectedBuffer());
    assertNull(editorModel.getSelectedBuffer().getFile());
    listener1StubFactory.assertSuccess("bufferChanged", Buffer.class);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferChanged", Buffer.class);
    listener2StubFactory.assertNoMoreCalls();

    // Select same buffer is a noop.
    editorModel.selectDefaultBuffer();

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();
  }

  public void testSelectBufferForFile() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final DelegatingStubFactory textSourceFactoryStubFactory =
      new DelegatingStubFactory(stringTextSourceFactory);
    final TextSource.Factory textSourceFactory =
      (TextSource.Factory)textSourceFactoryStubFactory.getStub();

    final EditorModel editorModel =
      new EditorModel(s_resources, textSourceFactory);

    textSourceFactoryStubFactory.resetCallHistory();

    final File file1 = createFile("myfile.txt", "blah");
    final File file2 = createFile("anotherFile.py", "Some stuffb");

    final RandomStubFactory listener1StubFactory =
      new RandomStubFactory(EditorModel.Listener.class);
    final EditorModel.Listener listener1 =
      (EditorModel.Listener)listener1StubFactory.getStub();

    final RandomStubFactory listener2StubFactory =
      new RandomStubFactory(EditorModel.Listener.class);
    final EditorModel.Listener listener2 =
      (EditorModel.Listener)listener2StubFactory.getStub();

    editorModel.addListener(listener1);
    editorModel.addListener(listener2);

    editorModel.selectBufferForFile(file1);

    assertNotNull(editorModel.getSelectedBuffer());
    assertEquals(file1, editorModel.getSelectedBuffer().getFile());
    textSourceFactoryStubFactory.assertSuccess("create");
    textSourceFactoryStubFactory.assertNoMoreCalls();

    final CallRecorder.CallData callData = listener1StubFactory.getCallData();
    assertEquals("bufferChanged", callData.getMethodName());
    final Object[] parameters = callData.getParameters();
    assertEquals(1, parameters.length);
    final Buffer bufferForFile1 = (Buffer)parameters[0];
    assertTrue(bufferForFile1.isActive());
    listener1StubFactory.assertNoMoreCalls();

    listener2StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener2StubFactory.assertNoMoreCalls();

    // Select same buffer is a noop.
    editorModel.selectBufferForFile(file1);

    assertTrue(bufferForFile1.isActive());
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();

    editorModel.selectBufferForFile(file2);

    assertTrue(!bufferForFile1.isActive());
    textSourceFactoryStubFactory.assertSuccess("create");
    textSourceFactoryStubFactory.assertNoMoreCalls();
    listener1StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener1StubFactory.assertSuccess("bufferChanged", Buffer.class);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener2StubFactory.assertSuccess("bufferChanged", Buffer.class);
    listener2StubFactory.assertNoMoreCalls();

    editorModel.selectBufferForFile(file1);

    textSourceFactoryStubFactory.assertNoMoreCalls();
    assertTrue(bufferForFile1.isActive());
    listener1StubFactory.assertSuccess("bufferChanged", Buffer.class);
    listener1StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferChanged", Buffer.class);
    listener2StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener2StubFactory.assertNoMoreCalls();

    final StringTextSource textSource1 =
      (StringTextSource)bufferForFile1.getTextSource();

    textSource1.markDirty();
    textSource1.markDirty();
    textSource1.markDirty();
    textSource1.markDirty();
    listener1StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferChanged", bufferForFile1);
    listener2StubFactory.assertNoMoreCalls();
  }

  private File createFile(String name, String text) throws Exception {
    final File file = new File(getDirectory(), name);
    final FileWriter out = new FileWriter(file);
    out.write(text);
    out.close();

    return file;
  }
}
