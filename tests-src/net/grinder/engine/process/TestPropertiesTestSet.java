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
import java.util.Set;

import net.grinder.plugininterface.TestDefinition;
import net.grinder.plugininterface.TestSetPlugin;
import net.grinder.util.GrinderProperties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestPropertiesTestSet extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestPropertiesTestSet.class);
    }

    public TestPropertiesTestSet(String name)
    {
	super(name);
    }

    protected void setUp() throws Exception
    {
    }

    public void testWithNoProperties() throws Exception
    {
	final PropertiesTestSet testSet =
	    new PropertiesTestSet(new GrinderProperties());

	assertEquals(0, testSet.getTests().size());
    }

    public void testWithTypicalProperties() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	final Properties[] testParameters = new Properties[3];

	for (int i=0; i<testParameters.length; i++) {
	    testParameters[i] = new Properties();
	}

	testParameters[0].put("past", "the pub that saps your body and");
	testParameters[0].put("the church", "all they want is your money");

	testParameters[1].put("we",
			      "can go for a walk where its quiet and dry");

	testParameters[1].put("and talk about precious things", "");
	testParameters[1].put("like", "love and law and politics");

	for (int i=0; i<testParameters.length; i++) {
	    final Iterator iterator = testParameters[i].entrySet().iterator();

	    while (iterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)iterator.next();
		final String key = (String)entry.getKey();
		final String value = (String)entry.getValue();

		final String propertyName =
		    PropertiesTestSet.getTestPropertyName(new Integer(i),
							  "parameter." + key);

		properties.put(propertyName, value);
	    }
	}

	final String descriptions[] = {
	    "The Queen is dead boys",
	    "and its so lonely on a limb",
	    "That's nothing, you should hear me play piano",
	};

	for (int i=0; i<descriptions.length; i++) {
	    final String propertyName =
		PropertiesTestSet.getTestPropertyName(new Integer(i),
						      "description");
	    properties.put(propertyName, descriptions[i]);
	}
	
	properties.put("grinder.otherstuff.parameter.something", "A string");
	properties.put("lah", "A string");

	final TestSetPlugin testSetPlugin = new PropertiesTestSet(properties);

	final Set tests = testSetPlugin.getTests();

	assertEquals(3, tests.size());

	final Iterator testSetIterator = tests.iterator();

	while (testSetIterator.hasNext()) {
	    final TestDefinition test = (TestDefinition)testSetIterator.next();

	    final int testNumber = test.getTestNumber().intValue();

	    assertEquals(descriptions[testNumber], test.getDescription());

	    final Properties originalParameters = testParameters[testNumber];
	    final Properties parsedParameters = test.getParameters();

	    assertEquals(originalParameters.size(), parsedParameters.size());
	    
	    final Iterator iterator = parsedParameters.entrySet().iterator();
	
	    while (iterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)iterator.next();
		final String key = (String)entry.getKey();
		final String value = (String)entry.getValue();

		assertEquals(originalParameters.get(key), value);
	    }
	}
    }
}
