// Copyright (C) 2002 Philip Aston
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

package net.grinder.plugininterface;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.grinder.engine.process.StubTestRegistryInitialisation;


/**
 * Unit test case for <code>PluginTest</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestPluginTest extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestPluginTest.class);
    }

    public TestPluginTest(String name)
    {
	super(name);
    }

    GrinderPlugin m_plugin = new StubGrinderPlugin();

    protected void setUp() throws Exception 
    {
	StubTestRegistryInitialisation.initialise();
    }

    public void testGetters() throws Exception
    {
	final PluginTest pluginTest  =
	    new MyPluginTest(m_plugin, 1, "description");

	assertEquals(1, pluginTest.getNumber());
	assertEquals("description", pluginTest.getDescription());
	assertNotNull(pluginTest.getParameters());
    }

    public void testOrdering() throws Exception
    {
	final int size = 100;

	final Set sorted = new TreeSet();
	final List keys = new ArrayList(size);

	for (int i=0; i<size; i++) {
	    keys.add(new Integer(i));
	}
	
	Collections.shuffle(keys);

	final Iterator keyIterator = keys.iterator();

	while (keyIterator.hasNext()) {
	    final int i = ((Integer)keyIterator.next()).intValue();
	    sorted.add(new MyPluginTest(m_plugin, i, Integer.toString(i)));
	}
	
	final Iterator sortedIterator = sorted.iterator();
	int i = 0;

	while (keyIterator.hasNext()) {
	    final PluginTest pluginTest = (PluginTest)sortedIterator.next();
	    assertEquals(i++, pluginTest.getNumber());
	}
    }

    public void testEquality() throws Exception
    {
	// Equality depends only on test number.
	final PluginTest t1 = new MyPluginTest(m_plugin, 57, "one thing");
	final PluginTest t2 = new MyPluginTest(m_plugin, 57, "leads to");
	final PluginTest t3 = new MyPluginTest(m_plugin, 58, "another");

	assertEquals(t1, t2);
	assertEquals(t2, t1);
	assertTrue(!t1.equals(t3));
	assertTrue(!t3.equals(t1));
	assertTrue(!t2.equals(t3));
	assertTrue(!t3.equals(t2));
    }

    public void testIsSerializable() throws Exception
    {
	final PluginTest pluginTest = new MyPluginTest(m_plugin, 123, "test");

	final ByteArrayOutputStream byteArrayOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteArrayOutputStream);

	objectOutputStream.writeObject(pluginTest);
    }

    static class MyPluginTest extends PluginTest
    {
	public MyPluginTest(GrinderPlugin plugin, int number,
			    String description)
	    throws Exception
	{
	    super(plugin, number, description);
	}
    }
}
