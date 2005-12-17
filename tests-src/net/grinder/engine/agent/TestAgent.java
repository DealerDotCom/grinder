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

package net.grinder.engine.agent;

import java.io.File;
import java.net.ServerSocket;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.Acceptor;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionIdentity;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Sender;
import net.grinder.communication.ServerReceiver;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for <code>Agent</code>
 * TestAgent.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestAgent extends AbstractFileTestCase {

  private final RandomStubFactory m_loggerStubFactory =
    new RandomStubFactory(Logger.class);
  private final Logger m_logger = (Logger)m_loggerStubFactory.getStub();

  public void testConstruction() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final Agent agent = new Agent(m_logger, propertyFile);
    agent.shutdown();

    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRun() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new Agent(m_logger, propertyFile);

    m_loggerStubFactory.assertNoMoreCalls();

    agent.run();

    m_loggerStubFactory.assertSuccess("output", String.class);
    // Cannot contact console.
    m_loggerStubFactory.assertSuccess("error", String.class);
    // grinder.py not readable.
    m_loggerStubFactory.assertSuccess("error", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setBoolean("grinder.useConsole", false);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertSuccess("output", String.class);
    // grinder.py not readable.
    m_loggerStubFactory.assertSuccess("error", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    final File scriptFile = new File(getDirectory(), "script");
    scriptFile.createNewFile();

    properties.setFile("grinder.script", scriptFile);
    properties.setInt("grinder.processes", 0);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertSuccess("output", String.class);
    // Command line.
    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setBoolean("grinder.debug.singleprocess", true);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertSuccess("output", String.class);
    // Spawning threads message.
    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setProperty("grinder.jvm.arguments", "-Dsome_stuff=blah");
    properties.save();

    agent.run();

    m_loggerStubFactory.assertSuccess("output", String.class);
    // Spawning threads message.
    m_loggerStubFactory.assertSuccess("output", String.class);
    // Warning about JVM arguments.
    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    agent.shutdown();

    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testWithConsole() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new Agent(m_logger, propertyFile);

    // Figure out a free local port.
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    final Acceptor acceptor = new Acceptor("", port, 1);
    final ServerReceiver receiver = new ServerReceiver();
    receiver.receiveFrom(acceptor, ConnectionType.AGENT, 1, 10);
    final Sender sender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    acceptor.addListener(ConnectionType.AGENT, new Acceptor.Listener() {

      public void connectionAccepted(ConnectionType connectionType,
                                     ConnectionIdentity connection) {
        try {
          // After we accept an agent connection, send a start message...
          sender.send(new StartGrinderMessage(null));

          // ..then shut communication down.
          acceptor.shutdown();
        }
        catch (CommunicationException e) {
          e.printStackTrace();
        }
      }

      public void connectionClosed(ConnectionType connectionType,
                                   ConnectionIdentity connection) {
      }});

    properties.setInt("grinder.consolePort", acceptor.getPort());
    properties.save();

    agent.run();

    acceptor.shutdown();

    m_loggerStubFactory.assertSuccess("output", String.class);

    final CallData firstCall = m_loggerStubFactory.getCallData();

    if (firstCall.getParameters().length == 1) {
      // waiting for console signal.
      firstCall.assertSuccess("output", String.class);
      // communication shutdown.
      m_loggerStubFactory.assertSuccess("output", String.class, Integer.class);
    }
    else {
      // Called in reverse order.
      firstCall.assertSuccess("output", String.class, Integer.class);
      m_loggerStubFactory.assertSuccess("output", String.class);
    }

    m_loggerStubFactory.assertNoMoreCalls();

    agent.shutdown();
    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    sender.shutdown();
    receiver.shutdown();
  }
}
