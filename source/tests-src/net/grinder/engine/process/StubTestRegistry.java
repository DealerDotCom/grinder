// Copyright (C) 2004 - 2008 Philip Aston
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

import net.grinder.script.Grinder;
import net.grinder.script.InternalScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.RandomStubFactory;


/**
 * Test utility that allows TestRegistryImplementation to be set from outside
 * package.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class StubTestRegistry {
  private static final RandomStubFactory s_testStatisticsHelperStubFactory =
    new RandomStubFactory(TestStatisticsHelper.class);
  private static final TestStatisticsHelper s_testStatisticsHelper =
    (TestStatisticsHelper)s_testStatisticsHelperStubFactory.getStub();


  public static void stubTestRegistry() {
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestRegistryImplementation testRegistry =
      new TestRegistryImplementation(
        null, statisticsSetFactory, s_testStatisticsHelper, null);

    final RandomStubFactory scriptEngineStubFactory =
      new RandomStubFactory(ScriptEngine.class);
    final Instrumenter scriptEngine =
      (Instrumenter)scriptEngineStubFactory.getStub();

    testRegistry.setInstrumenter(scriptEngine);

    final RandomStubFactory scriptContextStubFactory =
      new RandomStubFactory(InternalScriptContext.class);
    final InternalScriptContext scriptContext =
      (InternalScriptContext)scriptContextStubFactory.getStub();
    scriptContextStubFactory.setResult("getTestRegistry", testRegistry);

    Grinder.grinder = scriptContext;
  }
}
