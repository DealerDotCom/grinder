// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.engine.process;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.grinder.util.GrinderProperties;


public class TestTest extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTest.class);
    }

    public TestTest(String name)
    {
	super(name);
    }

    protected void setUp()
    {
	m_properties = new GrinderProperties();
    }

    private GrinderProperties m_properties;
    private Integer m_zero = new Integer(0);

    public void testDefaults() throws Exception
    {
	final Test test = new Test(m_zero, m_properties);
	assertEquals(null, test.getDescription());
	assertEquals(-1, test.getSleepTime());
	assertEquals(0, test.getParameters().size());
    }

    public void testBasicProperties() throws Exception
    {
	final String description = "A test description";
	final int sleepTime = 1000;

	m_properties.put("description", description);
	m_properties.put("sleepTime", Integer.toString(sleepTime));

	final Test test = new Test(m_zero, m_properties);

	assertEquals(description, test.getDescription());
	assertEquals(sleepTime, test.getSleepTime());
	assertNotNull(test.getStatistics());
    }

    public void testTestParameters() throws Exception
    {
	final Properties testParameters = new Properties();
	testParameters.put("abc", "abc");
	testParameters.put("A key with spaces", "blah");
	testParameters.put("", "Ooh, no name");
	testParameters.put("NoValue", "");
	testParameters.put("=", "");
	testParameters.put("", "=");

	Iterator entryIterator = testParameters.entrySet().iterator();

	while (entryIterator.hasNext())
	{
	    Map.Entry entry = (Map.Entry)entryIterator.next();

	    m_properties.put("parameter." + entry.getKey(), entry.getValue());
	}

	final Test test = new Test(m_zero, m_properties);

	assertEquals(testParameters, test.getParameters());
    }
}
