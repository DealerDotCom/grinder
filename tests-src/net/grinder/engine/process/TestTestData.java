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

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.common.StubTest;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.StubGrinderPlugin;


/**
 * Unit test case for <code>TestTestData</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestTestData extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestData.class);
    }

    public TestTestData(String name)
    {
	super(name);
    }

    public void testTestData() throws Exception
    {
	final GrinderPlugin plugin = new StubGrinderPlugin();

	final PluginRegistry.RegisteredPlugin registeredPlugin =
	    new PluginRegistry.RegisteredPlugin(plugin);
	
	final Test test1 = new StubTest(99, "Some stuff");
	
	final TestData testData1 =
	    new TestData(registeredPlugin, test1);
	assertEquals(registeredPlugin, testData1.getRegisteredPlugin());
	assertEquals(test1, testData1.getTest());
	assertNotNull(testData1.getStatistics());

	final Test test2 = new StubTest(-33, "");

	final TestData testData2 =
	    new TestData(registeredPlugin, test2);
	assertEquals(registeredPlugin, testData2.getRegisteredPlugin());
	assertEquals(test2, testData2.getTest());
	assertNotNull(testData2.getStatistics());
    }
}
