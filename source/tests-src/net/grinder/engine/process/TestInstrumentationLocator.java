// Copyright (C) 2009 Philip Aston
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.Instrumentation;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link InstrumentationLocator}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestInstrumentationLocator extends TestCase {

  private final RandomStubFactory<Instrumentation>
    m_instrumentationStubFactory =
      RandomStubFactory.create(Instrumentation.class);
  private final Instrumentation m_instrumentation =
    m_instrumentationStubFactory.getStub();

  private final RandomStubFactory<Instrumentation>
    m_instrumentationStubFactory2 =
      RandomStubFactory.create(Instrumentation.class);
  private final Instrumentation m_instrumentation2 =
    m_instrumentationStubFactory2.getStub();

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    InstrumentationLocator.clearInstrumentation();
  }

  public void testNullBehaviour() throws Exception {
    InstrumentationLocator.enter(this, "foo");
    InstrumentationLocator.exit(this, "foo", false);
  }

  public void testSingleRegistration() throws Exception {
    final Object target = new Object();

    InstrumentationLocator.register(target, "location", m_instrumentation);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(target, "location");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.exit(target, "location", true);
    m_instrumentationStubFactory.assertSuccess("endTest", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(this, "location");
    InstrumentationLocator.exit(this, "location", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(target, "location2");
    InstrumentationLocator.exit(target, "location2", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(target, "location");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.exit(target, "location", false);
    m_instrumentationStubFactory.assertSuccess("endTest", false);
    m_instrumentationStubFactory.assertNoMoreCalls();

    // Interned strings shouldn't match.
    InstrumentationLocator.enter(target, new String("location"));
    InstrumentationLocator.exit(target, new String("location"), true);
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testBadRegistration() throws Exception {
    final Object target = new Object();

    final EngineException exception = new EngineException("bork");

    m_instrumentationStubFactory.setThrows("startTest", exception);
    m_instrumentationStubFactory.setThrows("endTest", exception);

    InstrumentationLocator.register(target, "location", m_instrumentation);
    m_instrumentationStubFactory.assertNoMoreCalls();

    try {
      InstrumentationLocator.enter(target, "location");
      fail("Expected UncheckedGrinderException");
    }
    catch (UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }
    m_instrumentationStubFactory.assertException("startTest", exception);
    m_instrumentationStubFactory.assertNoMoreCalls();

    try {
      InstrumentationLocator.exit(target, "location", false);
      fail("Expected UncheckedGrinderException");
    }
    catch (UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }
    m_instrumentationStubFactory.assertException("endTest", exception, false);
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testStaticRegistration() throws Exception {
    InstrumentationLocator.register(null, "location", m_instrumentation);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(null, "location");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.exit(null, "location", true);
    m_instrumentationStubFactory.assertSuccess("endTest", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(this, "location");
    InstrumentationLocator.exit(this, "location", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(null, "location2");
    InstrumentationLocator.exit(null, "location2", true);
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testMultipleRegistrations() throws Exception {
    final Object target = new Object();
    final Object target2 = new Object();

    InstrumentationLocator.register(target, "location", m_instrumentation);

    InstrumentationLocator.enter(target2, "location");
    InstrumentationLocator.exit(target2, "location", false);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(target, "location");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.exit(target, "location", true);
    m_instrumentationStubFactory.assertSuccess("endTest", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.register(target2, "location", m_instrumentation2);
    InstrumentationLocator.register(target2, "location2", m_instrumentation);

    InstrumentationLocator.enter(target, "location");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(target2, "location");
    m_instrumentationStubFactory2.assertSuccess("startTest");
    m_instrumentationStubFactory2.assertNoMoreCalls();

    InstrumentationLocator.exit(target, "location", true);
    m_instrumentationStubFactory.assertSuccess("endTest", true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.exit(target2, "location", false);
    m_instrumentationStubFactory2.assertSuccess("endTest", false);
    m_instrumentationStubFactory2.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.enter(target2, "location2");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();

    InstrumentationLocator.exit(target2, "location2", true);
    m_instrumentationStubFactory.assertSuccess("endTest", true);
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testNestedRegistrations() throws Exception {
    final Object target = new Object();

    InstrumentationLocator.register(target, "location", m_instrumentation);
    InstrumentationLocator.register(target, "location", m_instrumentation);
    InstrumentationLocator.register(target, "location", m_instrumentation2);

    InstrumentationLocator.enter(target, "location");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory.assertSuccess("startTest");
    m_instrumentationStubFactory2.assertSuccess("startTest");
    m_instrumentationStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory2.assertNoMoreCalls();

    InstrumentationLocator.exit(target, "location", false);
    m_instrumentationStubFactory2.assertSuccess("endTest", false);
    m_instrumentationStubFactory.assertSuccess("endTest", false);
    m_instrumentationStubFactory.assertSuccess("endTest", false);
    m_instrumentationStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory2.assertNoMoreCalls();
  }

  public void testConcurrency() throws Exception {
    final ExecutorService executor = Executors.newCachedThreadPool();

    final AtomicInteger runs = new AtomicInteger(0);
    final AtomicInteger n = new AtomicInteger(0);

    final Instrumentation instrumentation = new Instrumentation() {
      public void start() throws EngineException {
        n.incrementAndGet();
      }

      public void end(boolean success) throws EngineException {
        n.decrementAndGet();
      }
    };

    final Random random = new Random();

    final String[] locations = { "L1", "L2", "L3" };

    class RegisterInstrumentation implements Runnable {
      public void run() {
        runs.incrementAndGet();

        final String location = locations[random.nextInt(locations.length)];

        if (random.nextInt(10) == 0) {
          InstrumentationLocator.register(this, location, instrumentation);
        }

        InstrumentationLocator.enter(this, location);
        InstrumentationLocator.exit(this, location, true);

        try {
          executor.execute(this);
        }
        catch (RejectedExecutionException e) {
        }
      }}

    final List<Runnable> runnables = new ArrayList<Runnable>();

    for (int i = 0; i < 10; ++i) {
      runnables.add(new RegisterInstrumentation());
    }

    for (Runnable r : runnables) {
      executor.execute(r);
    }

    while (runs.get() < 10000) {
      Thread.sleep(10);
    }
    executor.shutdown();
    assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

    assertEquals(0, n.get());
  }
}
