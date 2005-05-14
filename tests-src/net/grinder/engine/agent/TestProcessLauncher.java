// Copyright (C) 2004, 2005 Philip Aston
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

import junit.framework.TestCase;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.engine.common.EngineException;
import net.grinder.testutility.CallData;


/**
 *  Unit tests for <code>ProcessLauncher</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessLauncher extends TestCase {

  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  public void testConstructor() throws Exception {
    final ProcessLauncher processLauncher1 =
      new ProcessLauncher(0, null, null, null);

    assertTrue(processLauncher1.allFinished());

    final ProcessLauncher processLauncher2 =
      new ProcessLauncher(10, null, null, null);

    assertFalse(processLauncher2.allFinished());
  }

  public void testStartSomeProcesses() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final MyMonitor monitor = new MyMonitor();
    final MyProcessFactory myProcessFactory = new MyProcessFactory();

    final ProcessLauncher processLauncher =
      new ProcessLauncher(5, myProcessFactory, monitor, logger);

    monitor.waitFor(processLauncher);
    assertFalse(monitor.isFinished());

    assertEquals(-1, myProcessFactory.getLastProcessIndex());

    processLauncher.startSomeProcesses(1);

    assertEquals(0, myProcessFactory.getLastProcessIndex());

    assertFalse(processLauncher.allFinished());
    assertEquals(System.out, myProcessFactory.getLastOutputStream());
    assertEquals(System.err, myProcessFactory.getLastErrorStream());

    assertEquals(1, myProcessFactory.getChildProcesses().size());
    final ChildProcess childProcess =
      (ChildProcess)myProcessFactory.getChildProcesses().get(0);

    final CallData call = loggerStubFactory.getCallData();
    assertEquals("output", call.getMethodName());
    final Object[] parameters = call.getParameters();
    assertEquals(1, parameters.length);
    final String s = (String)parameters[0];
    assertTrue(s.indexOf(childProcess.getProcessName()) >= 0);
    loggerStubFactory.assertNoMoreCalls();

    processLauncher.startSomeProcesses(10);
    assertEquals(4, myProcessFactory.getLastProcessIndex());

    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertNoMoreCalls();

    assertEquals(5, myProcessFactory.getChildProcesses().size());

    assertFalse(processLauncher.allFinished());

    final ChildProcess[] processes =
      (ChildProcess[])
      myProcessFactory.getChildProcesses().toArray(new ChildProcess[0]);

    sendTerminationMessage(processes[0]);
    sendTerminationMessage(processes[2]);

    assertFalse(processLauncher.allFinished());
    assertFalse(monitor.isFinished());

    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);
    sendTerminationMessage(processes[4]);

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!monitor.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(processLauncher.allFinished());
  }

  private void sendTerminationMessage(ChildProcess process) {
    final PrintWriter processStdin =
      new PrintWriter(process.getStdinStream());

    processStdin.print("Foo\n");
    processStdin.flush();
  }

  public void testStartAllProcesses() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final MyMonitor monitor = new MyMonitor();
    final MyProcessFactory myProcessFactory = new MyProcessFactory();

    final ProcessLauncher processLauncher =
      new ProcessLauncher(9, myProcessFactory, monitor, logger);

    monitor.waitFor(processLauncher);
    assertFalse(monitor.isFinished());

    assertEquals(-1, myProcessFactory.getLastProcessIndex());

    processLauncher.startAllProcesses();

    assertEquals(8, myProcessFactory.getLastProcessIndex());

    assertFalse(processLauncher.allFinished());
    assertEquals(System.out, myProcessFactory.getLastOutputStream());
    assertEquals(System.err, myProcessFactory.getLastErrorStream());

    assertEquals(9, myProcessFactory.getChildProcesses().size());

    final ChildProcess[] processes =
      (ChildProcess[])
      myProcessFactory.getChildProcesses().toArray(new ChildProcess[0]);

    sendTerminationMessage(processes[0]);
    sendTerminationMessage(processes[6]);
    sendTerminationMessage(processes[5]);
    sendTerminationMessage(processes[2]);
    sendTerminationMessage(processes[7]);

    assertFalse(processLauncher.allFinished());
    assertFalse(monitor.isFinished());

    sendTerminationMessage(processes[8]);
    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);
    sendTerminationMessage(processes[4]);

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!monitor.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(processLauncher.allFinished());
  }

  public void testDestroyAllProcesses() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final MyMonitor monitor = new MyMonitor();
    final MyProcessFactory myProcessFactory = new MyProcessFactory();

    final ProcessLauncher processLauncher =
      new ProcessLauncher(4, myProcessFactory, monitor, logger);

    monitor.waitFor(processLauncher);
    assertFalse(monitor.isFinished());

    assertEquals(-1, myProcessFactory.getLastProcessIndex());

    processLauncher.startAllProcesses();

    assertEquals(3, myProcessFactory.getLastProcessIndex());

    assertFalse(processLauncher.allFinished());
    assertEquals(4, myProcessFactory.getChildProcesses().size());

    final ChildProcess[] processes =
      (ChildProcess[])
      myProcessFactory.getChildProcesses().toArray(new ChildProcess[0]);

    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);

    assertFalse(processLauncher.allFinished());
    assertFalse(monitor.isFinished());

    processLauncher.destroyAllProcesses();

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!monitor.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(processLauncher.allFinished());
  }

  private static class MyMonitor {
    private boolean m_finished;

    public synchronized void waitFor(final ProcessLauncher processLauncher) {

      m_finished = false;

      new Thread() {
        public void run() {
          try {
            synchronized (MyMonitor.this) {
              while (!processLauncher.allFinished()) {
                MyMonitor.this.wait();
              }
            }

            m_finished = true;
          }
          catch (InterruptedException e) {
          }
        }
      }.start();
    }

    public boolean isFinished() {
      return m_finished;
    }
  }

  private static class MyProcessFactory implements ProcessFactory {

    private int m_lastProcessIndex = -1;
    private OutputStream m_lastOutputStream;
    private OutputStream m_lastErrorStream;
    private ArrayList m_childProcesses = new ArrayList();

    public ChildProcess create(OutputStream outputStream,
                               OutputStream errorStream)
      throws EngineException {

      m_lastOutputStream = outputStream;
      m_lastErrorStream = errorStream;

      final String[] commandArray = {
        "java",
        "-classpath",
        s_testClasspath,
        EchoClass.class.getName(),
      };

      final ChildProcess childProcess =
        new ChildProcess("process " + ++m_lastProcessIndex, commandArray,
                         outputStream, errorStream);

      m_childProcesses.add(childProcess);

      return childProcess;
    }

    public String getCommandLine() {
      return "description of process";
    }

    public int getLastProcessIndex() {
      return m_lastProcessIndex;
    }

    public OutputStream getLastOutputStream() {
      return m_lastOutputStream;
    }

    public OutputStream getLastErrorStream() {
      return m_lastErrorStream;
    }

    public ArrayList getChildProcesses() {
      return m_childProcesses;
    }
  }
}
