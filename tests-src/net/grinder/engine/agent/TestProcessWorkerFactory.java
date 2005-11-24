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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Properties;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.engine.process.GrinderProcess;


/**
 *  Unit tests for <code>ProcessWorkerFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessWorkerFactory extends TestCase {

  private static final String s_classesDir = System.getProperty("classes.dir");
  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  public void testCreate() throws Exception {
    final GrinderProperties grinderProperties = new GrinderProperties() {{
      setProperty("grinder.jvm.classpath",
                  s_testClasspath + File.pathSeparatorChar + s_classesDir);
    }};

    final Properties overrideProperties = new Properties();

    final File alternateFile = new File("/tmp/my.properties");

    final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(1);

    final WorkerProcessCommandLine commandLine =
      new WorkerProcessCommandLine(
        grinderProperties, overrideProperties, alternateFile);

    final List commandList = commandLine.getCommandList();

    commandList.set(commandList.indexOf(GrinderProcess.class.getName()),
                    ReadMessageEchoClass.class.getName());

    final File scriptFile = new File("a");
    final File scriptDirectory = new File("b");
    final boolean reportToConsole = false;

    final AgentIdentityImplementation agentIdentityImplementation =
      new AgentIdentityImplementation("agent");

    final ProcessWorkerFactory processWorkerFactory =
      new ProcessWorkerFactory(commandLine,
                               agentIdentityImplementation,
                               fanOutStreamSender,
                               reportToConsole,
                               scriptFile,
                               scriptDirectory);

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

    final Worker worker =
      processWorkerFactory.create(outputStream, errorStream);

    assertTrue(worker.getIdentity().getName().endsWith("-0"));

    worker.waitFor();

    assertEquals("", new String(errorStream.toByteArray()));

    final InputStream output =
      new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(output);

    final InitialiseGrinderMessage echoedInitialiseGrinderMessage =
      (InitialiseGrinderMessage)objectInputStream.readObject();

    assertEquals(reportToConsole,
                 echoedInitialiseGrinderMessage.getReportToConsole());
    assertEquals(scriptDirectory,
                 echoedInitialiseGrinderMessage.getScriptDirectory());
    assertEquals(scriptFile,
                 echoedInitialiseGrinderMessage.getScriptFile());
    assertEquals(
      agentIdentityImplementation,
      echoedInitialiseGrinderMessage.getWorkerIdentity().getAgentIdentity());

    final byte[] remainingBytes = new byte[500];
    final int n = output.read(remainingBytes);
    final String echoedArguments = new String(remainingBytes, 0, n);

    assertEquals(alternateFile.getPath(), echoedArguments);
  }
}
