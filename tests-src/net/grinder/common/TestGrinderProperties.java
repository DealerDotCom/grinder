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
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGrinderProperties extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestGrinderProperties.class);
    }

    public TestGrinderProperties(String name)
    {
	super(name);
    }

    protected void setUp()
    {
	m_emptyGrinderProperties = new GrinderProperties();

	m_prefixSet.put(s_prefix + "A string", "Some more text");
	m_prefixSet.put(s_prefix + "An int", "9");

	m_stringSet.put("A string", "Some text");
	m_stringSet.put("Another string", "Some text");
	m_stringSet.put("", "Some text");
	m_stringSet.put("-83*(&(*991(*&(*", "\n\r\n");
	m_stringSet.put("Another empty string test", ""); 

	m_intSet.put("An integer", "9");
	m_intSet.put("Number", "-9");

	m_brokenIntSet.put("Broken int 1", "9x");
	m_brokenIntSet.put("Broken int 2", "");
	m_brokenIntSet.put("Broken int 3", "1234567890123456");
	m_brokenLongSet.put("Broken long 4", "1e-3");

	m_longSet.put("A long", "1234542222");
	m_longSet.put("Another long", "-19");

	m_brokenLongSet.put("Broken long 1", "0x9");
	m_brokenLongSet.put("Broken long 2", "");
	m_brokenLongSet.put("Broken long 3", "123456789012345612321321321321");
	m_brokenLongSet.put("Broken long 4", "10.4");

	m_shortSet.put("A short", "123");
	m_shortSet.put("Another short", "0");

	m_brokenShortSet.put("Broken short 1", "0x9");
	m_brokenShortSet.put("Broken short 2", "1.4");
	m_brokenShortSet.put("Broken short 3", "-0123456");

	m_doubleSet.put("A double", "1.0");
	m_doubleSet.put("Another double", "1");

	m_brokenDoubleSet.put("Broken double 1", "0x9");
	m_brokenDoubleSet.put("Broken double 2", "1/0");

	m_booleanSet.put("A boolean", "true");
	m_booleanSet.put("Another boolean", "false");
	m_booleanSet.put("Yet another boolean", "yes");
	m_booleanSet.put("One more boolean", "no");

	m_brokenBooleanSet.put("Broken boolean 1", "abc");
	m_brokenBooleanSet.put("Broken boolean 2", "019321 xx");
	m_brokenBooleanSet.put("Broken boolean 3", "uhuh");

	m_allSet.putAll(m_prefixSet);
	m_allSet.putAll(m_stringSet);
	m_allSet.putAll(m_intSet);
	m_allSet.putAll(m_brokenIntSet);
	m_allSet.putAll(m_longSet);
	m_allSet.putAll(m_brokenLongSet);
	m_allSet.putAll(m_shortSet);
	m_allSet.putAll(m_brokenShortSet);
	m_allSet.putAll(m_doubleSet);
	m_allSet.putAll(m_brokenDoubleSet);
	m_allSet.putAll(m_booleanSet);
	m_allSet.putAll(m_brokenBooleanSet);

	m_grinderProperties = new GrinderProperties();
	m_grinderProperties.putAll(m_allSet);
    }

    private final static String s_prefix = "prefix.";

    private GrinderProperties m_emptyGrinderProperties;
    private GrinderProperties m_grinderProperties;

    private final Properties m_allSet = new Properties();
    private final Properties m_prefixSet = new Properties();
    private final Properties m_stringSet = new Properties();
    private final Properties m_intSet = new Properties();
    private final Properties m_brokenIntSet = new Properties();
    private final Properties m_longSet = new Properties();
    private final Properties m_brokenLongSet = new Properties();
    private final Properties m_shortSet = new Properties();
    private final Properties m_brokenShortSet = new Properties();
    private final Properties m_doubleSet = new Properties();
    private final Properties m_brokenDoubleSet = new Properties();
    private final Properties m_booleanSet = new Properties();
    private final Properties m_brokenBooleanSet = new Properties();

    public void testGetPropertySubset() throws Exception
    {
	final GrinderProperties all =
	    m_grinderProperties.getPropertySubset("");

	assertEquals(all, m_allSet);

	final GrinderProperties none =
	    m_grinderProperties.getPropertySubset("Not there");

	assertEquals(0, none.size());

	final GrinderProperties prefixSet =
	    m_grinderProperties.getPropertySubset(s_prefix);

	assertEquals(prefixSet.size(), m_prefixSet.size());

	final Iterator iterator = prefixSet.entrySet().iterator();

	while (iterator.hasNext()) {
	    final Map.Entry entry = (Map.Entry)iterator.next();
	    final String key = (String)entry.getKey();
	    final String value = (String)entry.getValue();

	    assertEquals(value, m_prefixSet.get(s_prefix + key));
	}
    }

    public void testGetMandatoryProperty() throws Exception
    {
	try {
	    m_emptyGrinderProperties.getMandatoryProperty("Not there");
	    fail("Expected an exception");
	}
	catch (GrinderException e) {
	}

	(new IterateOverProperties(m_intSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(value,
				 m_grinderProperties.
				 getMandatoryProperty(key));
		}
	    }
	 ).run();
    }

    public void testGetInt() throws Exception
    {
	assertEquals(1, m_grinderProperties.getInt("Not there", 1));

	(new IterateOverProperties(m_intSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Integer.parseInt(value),
				 m_grinderProperties.getInt(key, 99));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenIntSet) {
		void match(String key, String value)
		{
		    assertEquals(99, m_grinderProperties.getInt(key, 99));
		}
	    }
	 ).run();
    }

    public void testGetMandatoryInt() throws Exception
    {
	try {
	    m_emptyGrinderProperties.getMandatoryInt("Not there");
	    fail("Expected an exception");
	}
	catch (GrinderException e) {
	}

	(new IterateOverProperties(m_intSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Integer.parseInt(value),
				 m_grinderProperties.getMandatoryInt(key));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenIntSet) {
		void match(String key, String value)
		{
		    try {
			m_grinderProperties.getMandatoryInt(key);
			fail("Expected an exception");
		    }
		    catch (GrinderException e) {
		    }
		}
	    }
	 ).run();
    }

    public void testGetLong() throws Exception
    {
	assertEquals(1, m_grinderProperties.getLong("Not there", 1));

	(new IterateOverProperties(m_longSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Long.parseLong(value),
				 m_grinderProperties.getLong(key, 99));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenLongSet) {
		void match(String key, String value)
		{
		    assertEquals(99, m_grinderProperties.getLong(key, 99));
		}
	    }
	 ).run();
    }

    public void testGetMandatoryLong() throws Exception
    {
	try {
	    m_emptyGrinderProperties.getMandatoryLong("Not there");
	    fail("Expected an exception");
	}
	catch (GrinderException e) {
	}

	(new IterateOverProperties(m_longSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Long.parseLong(value),
				 m_grinderProperties.getMandatoryLong(key));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenLongSet) {
		void match(String key, String value)
		{
		    try {
			m_grinderProperties.getMandatoryLong(key);
			fail("Expected an exception");
		    }
		    catch (GrinderException e) {
		    }
		}
	    }
	 ).run();
    }

    public void testGetShort() throws Exception
    {
	assertEquals((short)1, m_grinderProperties.getShort("Not there",
							    (short)1));

	(new IterateOverProperties(m_shortSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Short.parseShort(value),
				 m_grinderProperties.getShort(key, (short)99));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenShortSet) {
		void match(String key, String value)
		{
		    assertEquals(99, m_grinderProperties.getShort(key,
								  (short)99));
		}
	    }
	 ).run();
    }

    public void testGetMandatoryShort() throws Exception
    {
	try {
	    m_emptyGrinderProperties.getMandatoryShort("Not there");
	    fail("Expected an exception");
	}
	catch (GrinderException e) {
	}

	(new IterateOverProperties(m_shortSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Short.parseShort(value),
				 m_grinderProperties.getMandatoryShort(key));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenShortSet) {
		void match(String key, String value)
		{
		    try {
			m_grinderProperties.getMandatoryShort(key);
			fail("Expected an exception");
		    }
		    catch (GrinderException e) {
		    }
		}
	    }
	 ).run();
    }

    public void testGetDouble() throws Exception
    {
	assertEquals(1.0, m_grinderProperties.getDouble("Not there", 1.0), 0);

	(new IterateOverProperties(m_doubleSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Double.parseDouble(value),
				 m_grinderProperties.getDouble(key, 99.0), 0);
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenDoubleSet) {
		void match(String key, String value)
		{
		    assertEquals(99.0,
				 m_grinderProperties.getDouble(key, 99.0),
				 0);
		}
	    }
	 ).run();
    }

    public void testGetMandatoryDouble() throws Exception
    {
	try {
	    m_emptyGrinderProperties.getMandatoryDouble("Not there");
	    fail("Expected an exception");
	}
	catch (GrinderException e) {
	}

	(new IterateOverProperties(m_doubleSet) {
		void match(String key, String value) throws Exception
		{
		    assertEquals(Double.parseDouble(value),
				 m_grinderProperties.getMandatoryDouble(key),
				 0);
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenDoubleSet) {
		void match(String key, String value)
		{
		    try {
			m_grinderProperties.getMandatoryDouble(key);
			fail("Expected an exception");
		    }
		    catch (GrinderException e) {
		    }
		}
	    }
	 ).run();
    }

    public void testGetBoolean() throws Exception
    {
	assert(m_grinderProperties.getBoolean("Not there", true));
	assert(!m_grinderProperties.getBoolean("Not there", false));

	(new IterateOverProperties(m_booleanSet) {
		void match(String key, String value) throws Exception
		{
		    assert(!(Boolean.valueOf(value).booleanValue() ^
			     m_grinderProperties.getBoolean(key, false)));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenBooleanSet) {
		void match(String key, String value)
		{
		    // If the key exists, the boolean will always
		    // parse as false.
		    assert(!m_grinderProperties.getBoolean(key, false));
		}
	    }
	 ).run();

    }

    public void testGetMandatoryBoolean() throws Exception
    {
	try {
	    m_emptyGrinderProperties.getMandatoryBoolean("Not there");
	    fail("Expected an exception");
	}
	catch (GrinderException e) {
	}

	(new IterateOverProperties(m_booleanSet) {
		void match(String key, String value) throws Exception
		{
		    assert(!(Boolean.valueOf(value).booleanValue() ^
			     m_grinderProperties.getMandatoryBoolean(key)));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenBooleanSet) {
		void match(String key, String value) throws Exception
		{
		    // If the key exists, the boolean will always
		    // parse as false.
		    assert(!m_grinderProperties.getMandatoryBoolean(key));
		}
	    }
	 ).run();
    }

    private abstract class IterateOverProperties
    {
	private final Properties m_properties;

	IterateOverProperties(Properties properties) 
	{
	    m_properties = properties;
	}

	void run() throws Exception 
	{
	    final Iterator iterator = m_properties.entrySet().iterator();
	
	    while (iterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)iterator.next();
		final String key = (String)entry.getKey();
		final String value = (String)entry.getValue();

		match(key, value);
	    }
	}

	abstract void match(String key, String value) throws Exception;
    }
}

