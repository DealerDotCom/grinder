// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 * 
 * @author Philip Aston
 * @version $Revision$
 **/
final class TestData implements TestRegistry.RegisteredTest
{
    private final PluginRegistry.RegisteredPlugin m_registeredPlugin;
    private final Test m_test;
    private final TestStatistics m_statistics;

    TestData(PluginRegistry.RegisteredPlugin registeredPlugin,
	     Test testDefinition)
    {
	m_registeredPlugin = registeredPlugin;
	m_test = testDefinition;
	m_statistics = TestStatisticsFactory.getInstance().create();
    }

    final PluginRegistry.RegisteredPlugin getRegisteredPlugin() 
    {
	return m_registeredPlugin;
    }

    final Test getTest()
    {
	return m_test;
    }

    final TestStatistics getStatistics() 
    {
	return m_statistics;
    }

    public final GrinderPlugin getPlugin()
    {
	return m_registeredPlugin.getPlugin();
    }
}
