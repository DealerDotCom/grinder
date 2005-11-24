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

import net.grinder.common.AgentIdentity;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.common.WorkerIdentity;
import net.grinder.engine.common.EngineException;
import net.grinder.testutility.CallData;


/**
 *  Unit tests for <code>WorkerLauncher</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestWorkerLauncher extends TestCase {

  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  public void testConstructor() throws Exception {
    final WorkerLauncher workerLauncher1 =
      new WorkerLauncher(0, null, null, null);

    assertTrue(workerLauncher1.allFinished());

    final WorkerLauncher workerLauncher2 =
      new WorkerLauncher(10, null, null, null);

    assertFalse(workerLauncher2.allFinished());
  }

  public void testStartSomeProcesses() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final MyMonitor monitor = new MyMonitor();
    final MyWorkerFactory myProcessFactory = new MyWorkerFactory();

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(5, myProcessFactory, monitor, logger);

    monitor.waitFor(workerLauncher);
    assertFalse(monitor.isFinished());

    assertEquals(-1, myProcessFactory.getNumberOfProcesses());

    workerLauncher.startSomeWorkers(1);

    assertEquals(0, myProcessFactory.getNumberOfProcesses());

    assertFalse(workerLauncher.allFinished());
    assertEquals(System.out, myProcessFactory.getLastOutputStream());
    assertEquals(System.err, myProcessFactory.getLastErrorStream());

    assertEquals(1, myProcessFactory.getChildProcesses().size());
    final Worker childProcess =
      (Worker)myProcessFactory.getChildProcesses().get(0);

    final CallData call = loggerStubFactory.getCallData();
    assertEquals("output", call.getMethodName());
    final Object[] parameters = call.getParameters();
    assertEquals(1, parameters.length);
    final String s = (String)parameters[0];
    assertTrue(s.indexOf(childProcess.getIdentity().getName()) >= 0);
    loggerStubFactory.assertNoMoreCalls();

    workerLauncher.startSomeWorkers(10);
    assertEquals(4, myProcessFactory.getNumberOfProcesses());

    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertNoMoreCalls();

    assertEquals(5, myProcessFactory.getChildProcesses().size());

    assertFalse(workerLauncher.allFinished());

    final Worker[] processes =
      (Worker[])
      myProcessFactory.getChildProcesses().toArray(new Worker[0]);

    sendTerminationMessage(processes[0]);
    sendTerminationMessage(processes[2]);

    assertFalse(workerLauncher.allFinished());
    assertFalse(monitor.isFinished());

    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);
    sendTerminationMessage(processes[4]);

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!monitor.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(workerLauncher.allFinished());
  }

  private void sendTerminationMessage(Worker process) {
    final PrintWriter processStdin =
      new PrintWriter(process.getCommunicationStream());

    processStdin.print("Foo\n");
    processStdin.flush();
  }

  public void testStartAllProcesses() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final MyMonitor monitor = new MyMonitor();
    final MyWorkerFactory myProcessFactory = new MyWorkerFactory();

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(9, myProcessFactory, monitor, logger);

    monitor.waitFor(workerLauncher);
    assertFalse(monitor.isFinished());

    assertEquals(-1, myProcessFactory.getNumberOfProcesses());

    workerLauncher.startAllWorkers();

    assertEquals(8, myProcessFactory.getNumberOfProcesses());

    assertFalse(workerLauncher.allFinished());
    assertEquals(System.out, myProcessFactory.getLastOutputStream());
    assertEquals(System.err, myProcessFactory.getLastErrorStream());

    assertEquals(9, myProcessFactory.getChildProcesses().size());

    final Worker[] processes =
      (Worker[])
      myProcessFactory.getChildProcesses().toArray(new Worker[0]);

    sendTerminationMessage(processes[0]);
    sendTerminationMessage(processes[6]);
    sendTerminationMessage(processes[5]);
    sendTerminationMessage(processes[2]);
    sendTerminationMessage(processes[7]);

    assertFalse(workerLauncher.allFinished());
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

    assertTrue(workerLauncher.allFinished());
  }

  public void testDestroyAllProcesses() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final MyMonitor monitor = new MyMonitor();
    final MyWorkerFactory myProcessFactory = new MyWorkerFactory();

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(4, myProcessFactory, monitor, logger);

    monitor.waitFor(workerLauncher);
    assertFalse(monitor.isFinished());

    assertEquals(-1, myProcessFactory.getNumberOfProcesses());

    workerLauncher.startAllWorkers();

    assertEquals(3, myProcessFactory.getNumberOfProcesses());

    assertFalse(workerLauncher.allFinished());
    assertEquals(4, myProcessFactory.getChildProcesses().size());

    final Worker[] processes =
      (Worker[])
      myProcessFactory.getChildProcesses().toArray(new Worker[0]);

    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);

    assertFalse(workerLauncher.allFinished());
    assertFalse(monitor.isFinished());

    workerLauncher.destroyAllWorkers();

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!monitor.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(workerLauncher.allFinished());
  }

  private static class MyMonitor {
    private boolean m_finished;

    public synchronized void waitFor(final WorkerLauncher workerLauncher) {

      m_finished = false;

      new Thread() {
        public void run() {
          try {
            synchronized (MyMonitor.this) {
              while (!workerLauncher.allFinished()) {
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

  private static class MyWorkerFactory implements WorkerFactory {

    private int numberOfProcesses = -1;
    private OutputStream m_lastOutputStream;
    private OutputStream m_lastErrorStream;
    private ArrayList m_childProcesses = new ArrayList();
    private PublicAgentIdentityImplementation m_agentIdentity =
      new PublicAgentIdentityImplementation("process");

    public Worker create(OutputStream outputStream,
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

      final Worker childProcess =
        new ProcessWorker(m_agentIdentity.createWorkerIdentity(),
                          commandArray,
                          outputStream,
                          errorStream);
      ++numberOfProcesses;
      m_childProcesses.add(childProcess);

      return childProcess;
    }

    public String getCommandLine() {
      return "description of process";
    }

    public int getNumberOfProcesses() {
      return numberOfProcesses;
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
