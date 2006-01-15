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

package net.grinder.engine.process;

import junit.framework.TestCase;

import net.grinder.common.Test;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>TestRegistry</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestRegistry extends TestCase {
  private final RandomStubFactory m_testStatisticsHelperStubFactory =
    new RandomStubFactory(TestStatisticsHelper.class);
  private final TestStatisticsHelper m_testStatisticsHelper =
    (TestStatisticsHelper)m_testStatisticsHelperStubFactory.getStub();

  public TestTestRegistry(String name) {
    super(name);
  }

  public void testConstructorAndSingleton() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestRegistry testRegistry =
      new TestRegistry(
        threadContextLocator, statisticsSetFactory, m_testStatisticsHelper);

    assertNotNull(testRegistry.getTestStatisticsMap());

    TestRegistry.setInstance(testRegistry);
    assertEquals(testRegistry, TestRegistry.getInstance());

    TestRegistry.setInstance(null);
    assertNull(TestRegistry.getInstance());

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
  }

  public void testRegister() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestRegistry testRegistry =
      new TestRegistry(
        threadContextLocator, statisticsSetFactory, m_testStatisticsHelper);

    assertNull(testRegistry.getNewTests());

    final RandomStubFactory testStubFactory =
      new RandomStubFactory(Test.class);
    final Test test = (Test)testStubFactory.getStub();

    final RandomStubFactory scriptEngineStubFactory =
      new RandomStubFactory(ScriptEngine.class);
    final ScriptEngine scriptEngine =
      (ScriptEngine)scriptEngineStubFactory.getStub();

    testRegistry.setScriptEngine(scriptEngine);

    final TestRegistry.RegisteredTest registeredTest1 =
      testRegistry.register(test);

    final TestRegistry.RegisteredTest registeredTest2 =
      testRegistry.register(test);

    assertSame(registeredTest1, registeredTest2);

    assertTrue(testRegistry.getNewTests().contains(test));
    assertNull(testRegistry.getNewTests());

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
  }
}
