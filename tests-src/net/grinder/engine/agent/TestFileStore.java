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

package net.grinder.engine.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.Sender;
import net.grinder.communication.SimpleMessage;
import net.grinder.engine.messages.DistributeFilesMessage;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.FileContents;


/**
 *  Unit tests for <code>FileStore</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFileStore extends AbstractFileTestCase {

  public void testConstruction() throws Exception {

    final FileStore fileStore = new FileStore(getDirectory(), null);
    assertNotNull(fileStore.getSender(null));

    final File aFile = File.createTempFile("file", "", getDirectory());

    try {
      new FileStore(aFile, null);
      fail("Expected FileStoreException");
    }
    catch (FileStore.FileStoreException e) {
    }

    final File readOnlyDirectory = new File(getDirectory(), "directory");
    readOnlyDirectory.mkdir();
    readOnlyDirectory.setReadOnly();

    try {
      new FileStore(readOnlyDirectory, null);
      fail("Expected FileStoreException");
    }
    catch (FileStore.FileStoreException e) {
    }

    try {
      new FileStore(new File(aFile, "foo"), null);
      fail("Expected FileStoreException");
    }
    catch (FileStore.FileStoreException e) {
    }
  }

  public void testSender() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();

    final FileStore fileStore = new FileStore(getDirectory(), logger);

    final RandomStubFactory delegateSenderStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender delegateSender = (Sender)delegateSenderStubFactory.getStub();

    final Sender sender = fileStore.getSender(delegateSender);

    // Messages that aren't DistributeFilesMessages get passed through
    // to delegate.
    final Message message0 = new SimpleMessage();
    sender.send(message0);
    loggerStubFactory.assertNoMoreCalls();
    delegateSenderStubFactory.assertSuccess("send", new Object[] { message0 });
    delegateSenderStubFactory.assertNoMoreCalls();

    // No action if DistributeFilesMessages is empty.
    final Message message1 = new DistributeFilesMessage(new FileContents[0]);
    sender.send(message1);
    loggerStubFactory.assertNoMoreCalls();
    delegateSenderStubFactory.assertNoMoreCalls();

    // Shutdown is delegated.
    sender.shutdown();
    loggerStubFactory.assertNoMoreCalls();
    delegateSenderStubFactory.assertSuccess("shutdown", new Object[] {});
    delegateSenderStubFactory.assertNoMoreCalls();

    // Test with a good message.
    final File sourceDirectory = new File(getDirectory(), "source");
    sourceDirectory.mkdirs();

    final File file0 = new File(sourceDirectory, "file0");
    final OutputStream outputStream = new FileOutputStream(file0);
    final byte[] bytes = new byte[500];
    new Random().nextBytes(bytes);
    outputStream.write(bytes);
    outputStream.close();

    final File file1 = new File(sourceDirectory, "dir/file1");
    file1.getParentFile().mkdirs();
    file1.createNewFile();

    final FileContents fileContents0 =
      new FileContents(sourceDirectory, new File("file0"));

    final FileContents fileContents1 =
      new FileContents(sourceDirectory, new File("dir/file1"));

    final Message message2 = new DistributeFilesMessage(
      new FileContents[] { fileContents0, fileContents1, });
    sender.send(message2);
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertNoMoreCalls();
    delegateSenderStubFactory.assertNoMoreCalls();

    // Test with a bad message.
    new File(getDirectory(), "file0").setReadOnly();

    try {
      sender.send(message2);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("error", new Class[] { String.class });
    loggerStubFactory.assertNoMoreCalls();
    delegateSenderStubFactory.assertNoMoreCalls();
  }
}
