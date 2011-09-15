// Copyright (C) 2004 - 2011 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.*;
import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.script.Barrier;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.script.Statistics;
import net.grinder.script.TestRegistry;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.testutility.Time;
import net.grinder.util.Sleeper;
import net.grinder.util.SleeperImplementation;
import net.grinder.util.StandardTimeAuthority;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit test case for {@code ScriptContextImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestScriptContextImplementation {

  @Mock private ThreadContext m_threadContext;
  @Mock private Logger m_logger;
  @Mock private FilenameFactory m_filenameFactory;
  @Mock private ThreadStarter m_threadStarter;
  @Mock private ThreadStopper m_threadStopper;
  @Mock private Statistics m_statistics;
  @Mock private SSLControl m_sslControl;
  @Mock private TestRegistry m_testRegistry;
  @Mock private BarrierGroups m_barrierGroups;
  @Mock private BarrierGroup m_barrierGroup;
  @Mock private BarrierIdentityGenerator m_identityGenerator;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_barrierGroup.getName()).thenReturn("MyBarrierGroup");
    when(m_barrierGroups.getGroup("MyBarrierGroup"))
      .thenReturn(m_barrierGroup);
    when(m_barrierGroups.getIdentityGenerator())
      .thenReturn(m_identityGenerator);
  }

  @Test public void testConstructorAndGetters() throws Exception {

    final GrinderProperties properties = new GrinderProperties();

    final int threadNumber = 99;
    final int runNumber = 3;
    final StubThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    threadContextLocator.set(m_threadContext);

    when(m_threadContext.getThreadNumber()).thenReturn(threadNumber);
    when(m_threadContext.getRunNumber()).thenReturn(runNumber);

    final Sleeper sleeper = new SleeperImplementation(null, m_logger, 1, 0);

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");
    final WorkerIdentity workerIdentity = agentIdentity.createWorkerIdentity();
    final WorkerIdentity firstWorkerIdentity =
      agentIdentity.createWorkerIdentity();

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        workerIdentity,
        firstWorkerIdentity,
        threadContextLocator,
        properties,
        m_logger,
        m_filenameFactory,
        sleeper,
        m_sslControl,
        m_statistics,
        m_testRegistry,
        m_threadStarter,
        m_threadStopper,
        m_barrierGroups,
        m_barrierGroups);

    assertEquals(workerIdentity.getName(), scriptContext.getProcessName());
    assertEquals(workerIdentity.getNumber(),
                 scriptContext.getProcessNumber());
    assertEquals(firstWorkerIdentity.getNumber(),
                 scriptContext.getFirstProcessNumber());
    assertEquals(threadNumber, scriptContext.getThreadNumber());
    assertEquals(runNumber, scriptContext.getRunNumber());
    assertSame(m_logger, scriptContext.getLogger());
    assertSame(m_filenameFactory, scriptContext.getFilenameFactory());
    assertSame(properties, scriptContext.getProperties());
    assertSame(m_statistics, scriptContext.getStatistics());
    assertSame(m_sslControl, scriptContext.getSSLControl());
    assertSame(m_testRegistry, scriptContext.getTestRegistry());

    threadContextLocator.set(null);
    assertEquals(-1, scriptContext.getThreadNumber());
    assertEquals(-1, scriptContext.getRunNumber());
    assertEquals(m_statistics, scriptContext.getStatistics());

    assertEquals(0, scriptContext.getProcessNumber());
    assertEquals(-1, scriptContext.getAgentNumber());

    agentIdentity.setNumber(10);
    assertEquals(0, scriptContext.getProcessNumber());
    assertEquals(10, scriptContext.getAgentNumber());

    verifyNoMoreInteractions(m_threadStarter);

    scriptContext.startWorkerThread();
    verify(m_threadStarter).startThread(any());

    final Object testRunner = new Object();
    scriptContext.startWorkerThread(testRunner);
    verify(m_threadStarter).startThread(testRunner);
    verifyNoMoreInteractions(m_threadStarter);

    scriptContext.stopWorkerThread(10);
    verify(m_threadStopper).stopThread(10);
    verifyNoMoreInteractions(m_threadStopper);
  }

  @Test public void testSleep() throws Exception {

    final Sleeper sleeper =
      new SleeperImplementation(new StandardTimeAuthority(),
                                m_logger,
                                1,
                                0);

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, null, null, null, null, sleeper, null, null, null, null,
        null, null, null);

    assertTrue(
      new Time(50, 70) {
        public void doIt() throws Exception  { scriptContext.sleep(50); }
      }.run());

    assertTrue(
      new Time(40, 70) {
        public void doIt() throws Exception  { scriptContext.sleep(50, 5); }
      }.run());
  }

  @Test public void testStopThisWorkerThread() throws Exception {
    final StubThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, threadContextLocator, null, null, null, null, null, null,
        null, null, null, null, null);

    try {
      scriptContext.stopThisWorkerThread();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    threadContextLocator.set(m_threadContext);

    try {
      scriptContext.stopThisWorkerThread();
      fail("Expected ShutdownException");
    }
    catch (ShutdownException e) {
    }
  }

  @Test public void testLocalBarrier() throws Exception {
    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, null, null, null, null, null, null, null,
        null, null, null, m_barrierGroups, null);

    final Barrier globalBarrier = scriptContext.localBarrier("MyBarrierGroup");
    assertEquals("MyBarrierGroup", globalBarrier.getName());

    verify(m_barrierGroups).getIdentityGenerator();
    verify(m_identityGenerator).next();
  }

  @Test public void testGlobalBarrier() throws Exception {
    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, null, null, null, null, null, null, null,
        null, null, null, null, m_barrierGroups);

    final Barrier globalBarrier = scriptContext.globalBarrier("MyBarrierGroup");
    assertEquals("MyBarrierGroup", globalBarrier.getName());

    verify(m_barrierGroups).getIdentityGenerator();
    verify(m_identityGenerator).next();
  }
}
