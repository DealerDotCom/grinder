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

package net.grinder.util;

import junit.framework.TestCase;
//import junit.swingui.TestRunner;
import junit.textui.TestRunner;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.Test;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestPropertiesHelper extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestPropertiesHelper.class);
    }

    public TestPropertiesHelper(String name)
    {
	super(name);
    }

    private GrinderProperties m_emptyProperties = new GrinderProperties();

    private GrinderProperties m_properties = new GrinderProperties();
    final Properties[] m_testParameters = new Properties[3];

    final String m_descriptions[] = {
	"The Queen is dead boys",
	"and its so lonely on a limb",
	"So I broke into the palace, with a sponge and a rust spanner" +
	"She said \"I know you and you cannot sing\", I said" +
	"\"That's nothing, you should hear me play piano\"",
    };

    protected void setUp() throws Exception
    {
	m_properties.put("grinder.plugin", "net.grinder.util.NullPlugin");
	m_properties.put("grinder.otherstuff.parameter.something", "A string");
	m_properties.put("lah", "A string");

	for (int i=0; i<m_testParameters.length; i++) {
	    m_testParameters[i] = new Properties();
	}

	m_testParameters[0].put("past", "the pub that saps your body and");
	m_testParameters[0].put("the church", "all they want is your money");

	m_testParameters[1].put("we",
				"can go for a walk where its quiet and dry");

	m_testParameters[1].put("and talk about precious things", "");
	m_testParameters[1].put("like", "love and law and politics");

	for (int i=0; i<m_testParameters.length; i++) {
	    final Iterator iterator =
		m_testParameters[i].entrySet().iterator();

	    while (iterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)iterator.next();
		final String key = (String)entry.getKey();
		final String value = (String)entry.getValue();

		final String propertyName =
		    PropertiesHelper.getTestPropertyName(i,
							 "parameter." + key);

		m_properties.put(propertyName, value);
	    }
	}

	for (int i=0; i<m_descriptions.length; i++) {
	    final String propertyName =
		PropertiesHelper.getTestPropertyName(i, "description");
	    m_properties.put(propertyName, m_descriptions[i]);
	}
    }

    public void testInstantiatePlugin() throws Exception
    {
	GrinderProperties.setProperties(m_emptyProperties);

	try {
	    final PropertiesHelper helper = new PropertiesHelper();

	    final ProcessContextImplementation context =
		new ProcessContextImplementation("host", "process");

	    helper.instantiatePlugin(context);

	    fail("Expected exception");
	}
	catch (GrinderException e) {
	}

	GrinderProperties.setProperties(m_properties);

	final PropertiesHelper helper = new PropertiesHelper();

	final ProcessContextImplementation context =
	    new ProcessContextImplementation("host", "process");

	helper.instantiatePlugin(context);
    }

    public void testGetTestSet() throws Exception
    {
	GrinderProperties.setProperties(m_properties);

	final PropertiesHelper helper = new PropertiesHelper();

	final ProcessContextImplementation context =
	    new ProcessContextImplementation("host", "process");

	final Set tests = helper.instantiatePlugin(context).getTests();

	assertEquals(m_testParameters.length, tests.size());

	final Iterator testSetIterator = tests.iterator();

	while (testSetIterator.hasNext()) {
	    final Test test = (Test)testSetIterator.next();

	    final int number = test.getNumber();

	    assertEquals(m_descriptions[number], test.getDescription());

	    final Properties originalParameters = m_testParameters[number];
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
