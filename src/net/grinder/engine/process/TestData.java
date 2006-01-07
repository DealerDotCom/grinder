// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.common.Test;
import net.grinder.engine.common.EngineException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.statistics.StatisticsSet;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class TestData
  implements TestRegistry.RegisteredTest, ScriptEngine.Dispatcher {

  private final ScriptEngine m_scriptEngine;
  private final ThreadContextLocator m_threadContextLocator;
  private final Test m_test;

  /**
   * Cumulative statistics for our test that haven't yet been set to
   * the console.
   */
  private final StatisticsSet m_statisticsSet;

  private final ThreadLocal m_dispatchProtection = new ThreadLocal() {
    public Object initialValue() { return new DispatchProtection(); }
  };

  TestData(ScriptEngine scriptEngine,
           ThreadContextLocator threadContextLocator,
           StatisticsSet statisticsSet,
           Test testDefinition) {
    m_scriptEngine = scriptEngine;
    m_threadContextLocator = threadContextLocator;
    m_test = testDefinition;
    m_statisticsSet = statisticsSet;
  }

  Test getTest() {
    return m_test;
  }

  StatisticsSet getStatisticsSet() {
    return m_statisticsSet;
  }

  public Object dispatch(Callable callable) throws EngineException {
    final DispatchProtection dispatchProtection =
      (DispatchProtection)m_dispatchProtection.get();

    try {
      if (!dispatchProtection.checkAndSet()) {
        final ThreadContext threadContext = m_threadContextLocator.get();

        if (threadContext == null) {
          throw new EngineException("Only Worker Threads can invoke tests");
        }

        return threadContext.invokeTest(this, callable);

        //return new Dispatcher().dispatch(invokeable);
      }
      else {
        // Already in a dispatch.
        return callable.call();
      }
    }
    finally {
      dispatchProtection.reset();
    }
  }

  /**
   * Create a proxy object that wraps an target object for this test.
   *
   * @param o Object to wrap.
   * @return The proxy.
   * @throws NotWrappableTypeException If the target could not be wrapped.
   */
  public Object createProxy(Object o) throws NotWrappableTypeException {
    return m_scriptEngine.createInstrumentedProxy(getTest(), this, o);
  }

    // TODO, rename, comment
  private class DispatchProtection {
    private boolean m_inDispatch;

    public boolean checkAndSet() {
      final boolean result = m_inDispatch;
      m_inDispatch = true;
      return result;
    }

    public void reset() {
      m_inDispatch = false;
    }
  }

  /* TODO
  private class Dispatcher {

    public Object dispatch(Callable invokeable) throws ShutdownException {
      final ThreadContext threadContext = m_threadContextLocator.get();

      if (threadContext == null) {
        throw new EngineException("Only Worker Threads can invoke tests");
      }

      final ThreadLogger threadLogger = threadContext.getThreadLogger();

      if (m_processContext.getShutdown()) {
        throw new ShutdownException("Process has been shutdown");
      }

      if (threadLogger.getCurrentTestNumber() != -1) {
        // Originally we threw a ReentrantInvocationException here.
        // However, this caused problems when wrapping Jython objects
        // that call themselves; in our scheme the wrapper shares a
        // dictionary so self = self and we recurse up our own.
        return invokeable.call();
      }

      threadLogger.setCurrentTestNumber(testData.getTest().getNumber());

      m_scriptStatistics.beginTest(testData, getRunNumber());

      try {
        startTimer();

        final Object testResult;

        try {
          testResult = invokeable.call();
        }
        finally {
          stopTimer();
        }

        return testResult;
      }
      catch (org.python.core.PyException e) {
        // Always mark as an error if the test threw an exception.
        m_scriptStatistics.setSuccessNoChecks(false);

        // We don't log the exception. If the script doesn't handle the
        // exception it will be logged when the run is aborted,
        // otherwise we assume the script writer knows what they're
        // doing.
        throw e;
      }
      finally {
        m_scriptStatistics.endTest(
          m_startTime - m_processContext.getExecutionStartTime(),
          m_elapsedTime);

        threadLogger.setCurrentTestNumber(-1);
      }
    }
  }
      */
}
