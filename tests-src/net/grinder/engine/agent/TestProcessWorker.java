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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.grinder.common.GrinderException;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>ChildProcess</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessWorker extends TestCase {

  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  private ByteArrayOutputStream m_outputStream = new ByteArrayOutputStream();
  private ByteArrayOutputStream m_errorStream = new ByteArrayOutputStream();

  public void testWithInvalidProcess() throws Exception {
    final String[] commandArray = {
      "No such process blah blah blah",
      "some argument",
    };

    try {
      new ProcessWorker("my process", commandArray, m_outputStream,
                       m_errorStream);
      fail("Expected GrinderException");
    }
    catch (GrinderException e) {
    }

    assertEquals(0, m_outputStream.toByteArray().length);
    assertEquals(0, m_errorStream.toByteArray().length);
  }

  public void testWithInvalidJavaClass() throws Exception {

    final String[] commandArray = {
      "java",
      "some nonsense class",
    };

    final ProcessWorker childProcess =
      new ProcessWorker("my process", commandArray, m_outputStream,
                       m_errorStream);

    childProcess.waitFor();

    assertEquals("my process", childProcess.getName());
    assertEquals(0, m_outputStream.toByteArray().length);
    assertTrue(m_errorStream.toByteArray().length > 0);
  }

  public void testArguments() throws Exception {
    final String[] commandArray = {
      "java",
      "-classpath",
      s_testClasspath,
      EchoClass.class.getName(),
      "some stuff",
      "blah",
      "3810 32190 130100''''",
    };

    final ProcessWorker childProcess =
      new ProcessWorker("echo", commandArray, m_outputStream, m_errorStream);

    final PrintWriter out =
      new PrintWriter(childProcess.getCommunicationStream());
    out.print(EchoClass.ECHO_ARGUMENTS);
    out.print('\n');
    out.flush();

    childProcess.waitFor();

    final StringBuffer expected = new StringBuffer();

    for (int i=4; i<commandArray.length; ++i) {
      expected.append(commandArray[i]);
    }

    assertEquals("", new String(m_errorStream.toByteArray()));

    assertEquals(expected.toString(),
                 new String(m_outputStream.toByteArray()));

    assertEquals("echo", childProcess.getName());
  }

  public void testConcurrentProcessing() throws Exception {
    final String[] commandArray = {
      "java",
      "-classpath",
      s_testClasspath,
      EchoClass.class.getName(),
    };

    final ProcessWorker childProcess =
      new ProcessWorker("echo", commandArray, m_outputStream, m_errorStream);

    final PrintWriter out =
      new PrintWriter(childProcess.getCommunicationStream());
    out.print(EchoClass.ECHO_STREAMS);
    out.print('\n');
    out.flush();

    final Thread t =
      new Thread(new WriteData(childProcess.getCommunicationStream()));
    t.start();

    childProcess.waitFor();

    assertTrue(!t.isAlive());

    final byte[] outputBytes = m_outputStream.toByteArray();
    final byte[] errorBytes = m_errorStream.toByteArray();

    assertEquals(256, outputBytes.length);
    assertEquals(256, errorBytes.length);

    for (int i=0; i<256; ++i) {
      assertEquals(i, outputBytes[i] & 0xFF);
      assertEquals(i, errorBytes[i] & 0xFF);
    }
  }

  public void testDestroy() throws Exception {
    final String[] commandArray = {
      "java",
      "-classpath",
      s_testClasspath,
      EchoClass.class.getName(),
    };

    final ProcessWorker childProcess =
      new ProcessWorker("echo", commandArray, m_outputStream, m_errorStream);

    childProcess.destroy();

    // Won't return if process is running. Actual exit value is
    // platform specific, and sometimes 0 on win32!
    final int exitValue = childProcess.waitFor();
  }

  private static final class WriteData implements Runnable {
    private final OutputStream m_outputStream;

    public WriteData(OutputStream outputStream) {
      m_outputStream = outputStream;
    }

    public void run() {
      try {
        for (int i=0; i<256; ++i) {
          m_outputStream.write(i);
          Thread.sleep(1);
        }
        m_outputStream.close();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
