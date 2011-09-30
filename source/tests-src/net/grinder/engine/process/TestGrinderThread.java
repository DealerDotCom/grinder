// Copyright (C) 2008 - 2011 Philip Aston
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.common.GrinderProperties;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.scriptengine.ScriptExecutionException;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.util.Sleeper;

import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link GrinderThread}.
 *
 * @author Philip Aston
 */
public class TestGrinderThread extends AbstractFileTestCase {

  @Mock private WorkerThreadSynchronisation m_workerThreadSynchronisation;
  @Mock private Sleeper m_sleeper;
  @Mock private ProcessLifeCycleListener m_processContext;
  @Mock private ScriptEngine m_scriptEngine;
  @Mock private WorkerRunnable m_workerRunnable;
  @Captor private ArgumentCaptor<ThreadContext> m_threadContextCaptor;

  private final GrinderProperties m_properties = new GrinderProperties();

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  private LoggerImplementation m_loggerImplementation;

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    m_loggerImplementation =
      new LoggerImplementation("grinderid", getDirectory().getPath(), false, 0);

    when(m_scriptEngine.createWorkerRunnable()).thenReturn(m_workerRunnable);
  }

  public void testConstruction() throws Exception {
    new GrinderThread(m_workerThreadSynchronisation,
                      m_processContext,
                      m_properties,
                      m_sleeper,
                      m_loggerImplementation,
                      m_statisticsServices,
                      m_scriptEngine,
                      3,
                      null);

    verify(m_workerThreadSynchronisation).threadCreated();

    verify(m_processContext).threadCreated(m_threadContextCaptor.capture());

    verifyNoMoreInteractions(m_processContext);

    assertEquals(3, m_threadContextCaptor.getValue().getThreadNumber());
  }

  public void testRun() throws Exception {
    final GrinderThread grinderThread =
      new GrinderThread(m_workerThreadSynchronisation,
                        m_processContext,
                        m_properties,
                        m_sleeper,
                        m_loggerImplementation,
                        m_statisticsServices,
                        m_scriptEngine,
                        3,
                        null);

    verify(m_workerThreadSynchronisation).threadCreated();

    verify(m_processContext).threadCreated(m_threadContextCaptor.capture());

    final ThreadContext threadContext = m_threadContextCaptor.getValue();

    final ThreadLifeCycleListener listener =
      mock(ThreadLifeCycleListener.class);

    threadContext.registerThreadLifeCycleListener(listener);

    grinderThread.run();

    verify(m_processContext).threadCreated(threadContext);

    verify(listener).beginThread();
    verify(listener).beginRun();
    verify(listener).endRun();
    verify(listener).beginShutdown();
    verify(listener).endThread();

    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);

    verify(m_sleeper).sleepFlat(0);
    verifyNoMoreInteractions(m_sleeper);

    m_properties.setInt("grinder.runs", 2);
    m_properties.setLong("grinder.initialSleepTime", 100);

    reset(m_workerThreadSynchronisation, m_sleeper);

    grinderThread.run();

    verify(listener, times(2)).beginThread();
    verify(listener, times(3)).beginRun();
    verify(listener, times(3)).endRun();
    verify(listener, times(2)).beginShutdown();
    verify(listener, times(2)).endThread();

    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);

    verify(m_sleeper).sleepFlat(100);
    verifyNoMoreInteractions(m_sleeper);

    m_properties.setInt("grinder.runs", 0);

    doThrow(new MyScriptEngineException(new ShutdownException("bye")))
      .when(m_workerRunnable).run();

    reset(m_workerThreadSynchronisation, m_sleeper);

    grinderThread.run();

    verify(listener, times(3)).beginThread();
    verify(listener, times(4)).beginRun();
    verify(listener, times(3)).beginShutdown();
    verify(listener, times(3)).endThread();

    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);


    m_properties.setInt("grinder.runs", 1);

    doThrow(new MyScriptEngineException("whatever"))
      .when(m_workerRunnable).run();
    doThrow(new MyScriptEngineException("whatever"))
    .when(m_workerRunnable).shutdown();

    reset(m_workerThreadSynchronisation, m_sleeper);

    grinderThread.run();

    verify(listener, times(4)).beginThread();
    verify(listener, times(5)).beginRun();
    verify(listener, times(4)).endRun();
    verify(listener, times(4)).beginShutdown();
    verify(listener, times(4)).endThread();

    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);


    doThrow(new MyScriptEngineException("blah"))
      .when(m_scriptEngine).createWorkerRunnable();

    reset(m_workerThreadSynchronisation, m_sleeper);

    grinderThread.run();

    verify(listener, times(5)).beginThread();

    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);

    doThrow(new EngineException("blah"))
      .when(m_scriptEngine).createWorkerRunnable();

    reset(m_workerThreadSynchronisation, m_sleeper);

    grinderThread.run();

    verify(listener, times(6)).beginThread();

    verify(m_workerThreadSynchronisation).threadFinished();

    verifyNoMoreInteractions(m_workerThreadSynchronisation, listener);
  }

  public void testRunWithWorkerRunnable() throws Exception {
    final GrinderThread grinderThread =
      new GrinderThread(m_workerThreadSynchronisation,
                        m_processContext,
                        m_properties,
                        m_sleeper,
                        m_loggerImplementation,
                        m_statisticsServices,
                        m_scriptEngine,
                        3,
                        m_workerRunnable);

    verify(m_workerThreadSynchronisation).threadCreated();

    verify(m_processContext).threadCreated(m_threadContextCaptor.capture());

    final ThreadContext threadContext = m_threadContextCaptor.getValue();

    final ThreadLifeCycleListener listener =
      mock(ThreadLifeCycleListener.class);

    threadContext.registerThreadLifeCycleListener(listener);

    grinderThread.run();

    verify(m_processContext).threadStarted(threadContext);

    verify(listener).beginThread();
    verify(listener).beginRun();
    verify(listener).endRun();
    verify(listener).beginShutdown();
    verify(listener).endThread();

    verify(m_workerRunnable).run();
    verify(m_workerRunnable).shutdown();

    verifyNoMoreInteractions(m_processContext, listener, m_workerRunnable);
  }

  private static final class MyScriptEngineException
    extends ScriptExecutionException {
    public MyScriptEngineException(Throwable t) {
      super("whoops", t);
    }

    public MyScriptEngineException(String message) {
      super(message);
    }

    public String getShortMessage() {
      return "";
    }
  }
}
