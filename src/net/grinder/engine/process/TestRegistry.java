// Copyright (C) 2001, 2002, 2003 Philip Aston
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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.communication.RegisterTestsMessage;
import net.grinder.communication.Sender;
import net.grinder.engine.EngineException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.statistics.TestStatisticsMap;


/**
 * Registry of Tests.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class TestRegistry {

  private static TestRegistry s_instance;
  private final Sender m_consoleSender;

  /**
   * A map of Test to TestData's. (TestData is the class this
   * package uses to store information about Tests). Synchronize on
   * instance when accessesing.
   **/
  private final Map m_testMap = new TreeMap();

  /**
   * A map of Tests to Statistics for passing elsewhere.
   **/
  private final TestStatisticsMap m_testStatisticsMap =
    new TestStatisticsMap();

  /**
   * Singleton accessor.
   * @return The singleton.
   */
  public static final TestRegistry getInstance() {
    return s_instance;
  }

  /**
   * Constructor.
   */
  TestRegistry(Sender consoleSender) throws EngineException {
    if (s_instance != null) {
      throw new EngineException("Already initialised");
    }

    s_instance = this;

    m_consoleSender = consoleSender;
  }

  /**
   * Register a new test.
   *
   * @param test The test.
   * @return A handle to the test.
   * @exception GrinderException if an error occurs
   */
  public RegisteredTest register(Test test) throws GrinderException {

    final TestData newTestData;

    synchronized (this) {
      final TestData existing = (TestData)m_testMap.get(test);

      if (existing != null) {
	return existing;
      }
	    
      newTestData = new TestData(test);
      m_testMap.put(test, newTestData);
      m_testStatisticsMap.put(test, newTestData.getStatistics());
    }
	
    m_consoleSender.queue(
      new RegisterTestsMessage(Collections.singleton(test)));

    return newTestData;
  }

  final TestStatisticsMap getTestStatisticsMap() {
    return m_testStatisticsMap;
  }

  /**
   * Interface for test handles.
   */
  public interface RegisteredTest {
    /**
     * Create a proxy object that wraps an target object for this test.
     *
     * @param o Object to wrap.
     * @return The proxy.
     * @exception NotWrappableTypeException If the target is not wrappable.
     */
    Object createProxy(Object o) throws NotWrappableTypeException;
  }
}
