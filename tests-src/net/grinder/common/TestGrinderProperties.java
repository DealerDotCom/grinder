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

package net.grinder.common;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.grinder.common.GrinderException;


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
	m_emptyGrinderProperties.setErrorWriter(
	    m_logCounter.getErrorLogWriter());

	m_prefixSet.put(s_prefix + "A string", "Some more text");
	m_prefixSet.put(s_prefix + "An int", "9");

	m_stringSet.put("A_string", "Some text");
	m_stringSet.put("Another_String", "Some text");
	m_stringSet.put("", "Some text");
	m_stringSet.put("-83*(&(*991(*&(*", "\n\r\n");
	m_stringSet.put("Another_empty_string_test", ""); 

	// A couple of properties that are almots in m_grinderSet.
	m_stringSet.put("grinder", ".no_dot_suffix"); 
	m_stringSet.put("grinder_", "blah"); 

	m_intSet.put("An_integer", "9");
	m_intSet.put("Number", "-9");

	m_brokenIntSet.put("Broken_int_1", "9x");
	m_brokenIntSet.put("Broken_int_2", "");
	m_brokenIntSet.put("Broken_int_3", "1234567890123456");
	m_brokenLongSet.put("Broken_long_4", "1e-3");

	m_longSet.put("A_long", "1234542222");
	m_longSet.put("Another_long", "-19");

	m_brokenLongSet.put("Broken_long_1", "0x9");
	m_brokenLongSet.put("Broken_long_2", "");
	m_brokenLongSet.put("Broken_long_3", "123456789012345612321321321321");
	m_brokenLongSet.put("Broken_long_4", "10.4");

	m_shortSet.put("A_short", "123");
	m_shortSet.put("Another_short", "0");

	m_brokenShortSet.put("Broken_short_1", "0x9");
	m_brokenShortSet.put("Broken_short_2", "1.4");
	m_brokenShortSet.put("Broken_short_3", "-0123456");

	m_doubleSet.put("A_double", "1.0");
	m_doubleSet.put("Another_double", "1");

	m_brokenDoubleSet.put("Broken_double_1", "0x9");
	m_brokenDoubleSet.put("Broken_double_2", "1/0");

	m_booleanSet.put("A_boolean", "true");
	m_booleanSet.put("Another_boolean", "false");
	m_booleanSet.put("Yet_another_boolean", "yes");
	m_booleanSet.put("One_more_boolean", "no");

	m_brokenBooleanSet.put("Broken_boolean_1", "abc");
	m_brokenBooleanSet.put("Broken_boolean_2", "019321 xx");
	m_brokenBooleanSet.put("Broken_boolean_3", "uhuh");

	// All properties that begin with "grinder."
	m_grinderSet.put("grinder.abc", "xyz");
	m_grinderSet.put("grinder.blah.blah", "123");

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
	m_allSet.putAll(m_grinderSet);

	m_grinderProperties = new GrinderProperties();
	m_grinderProperties.putAll(m_allSet);
	m_grinderProperties.setErrorWriter(m_logCounter.getErrorLogWriter());
    }

    private final static String s_prefix = "prefix.";

    private GrinderProperties m_emptyGrinderProperties;
    private GrinderProperties m_grinderProperties;
    private LogCounter m_logCounter = new LogCounter();

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
    private final Properties m_grinderSet = new Properties();

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

	final int oldErrorLines = m_logCounter.getNumberOfErrorLines();

	(new IterateOverProperties(m_brokenIntSet) {
		void match(String key, String value)
		{
		    assertEquals(99, m_grinderProperties.getInt(key, 99));
		}
	    }
	 ).run();

	assertEquals(oldErrorLines + m_brokenIntSet.size(),
		     m_logCounter.getNumberOfErrorLines());
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

	final int oldErrorLines = m_logCounter.getNumberOfErrorLines();

	(new IterateOverProperties(m_brokenLongSet) {
		void match(String key, String value)
		{
		    assertEquals(99, m_grinderProperties.getLong(key, 99));
		}
	    }
	 ).run();

	assertEquals(oldErrorLines + m_brokenLongSet.size(),
		     m_logCounter.getNumberOfErrorLines());
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

	final int oldErrorLines = m_logCounter.getNumberOfErrorLines();

	(new IterateOverProperties(m_brokenShortSet) {
		void match(String key, String value)
		{
		    assertEquals(99, m_grinderProperties.getShort(key,
								  (short)99));
		}
	    }
	 ).run();

	assertEquals(oldErrorLines + m_brokenShortSet.size(),
		     m_logCounter.getNumberOfErrorLines());
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

	final int oldErrorLines = m_logCounter.getNumberOfErrorLines();

	(new IterateOverProperties(m_brokenDoubleSet) {
		void match(String key, String value)
		{
		    assertEquals(99.0,
				 m_grinderProperties.getDouble(key, 99.0),
				 0);
		}
	    }
	 ).run();

	assertEquals(oldErrorLines + m_brokenDoubleSet.size(),
		     m_logCounter.getNumberOfErrorLines());
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
	assertTrue(m_grinderProperties.getBoolean("Not there", true));
	assertTrue(!m_grinderProperties.getBoolean("Not there", false));

	(new IterateOverProperties(m_booleanSet) {
		void match(String key, String value) throws Exception
		{
		    assertTrue(!(Boolean.valueOf(value).booleanValue() ^
				 m_grinderProperties.getBoolean(key, false)));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenBooleanSet) {
		void match(String key, String value)
		{
		    // If the key exists, the boolean will always
		    // parse as false.
		    assertTrue(!m_grinderProperties.getBoolean(key, false));
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
		    assertTrue(!(Boolean.valueOf(value).booleanValue() ^
				 m_grinderProperties.getMandatoryBoolean(key)));
		}
	    }
	 ).run();

	(new IterateOverProperties(m_brokenBooleanSet) {
		void match(String key, String value) throws Exception
		{
		    // If the key exists, the boolean will always
		    // parse as false.
		    assertTrue(!m_grinderProperties.getMandatoryBoolean(key));
		}
	    }
	 ).run();
    }

    public void testSetInt() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	
	(new IterateOverProperties(m_intSet) {
		void match(String key, String value) throws Exception
		{
		    properties.setInt(key, Integer.parseInt(value));
		    assertEquals(value,
				 properties.getMandatoryProperty(key));
		}
	    }
	 ).run();
    }

    public void testSetLong() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	
	(new IterateOverProperties(m_longSet) {
		void match(String key, String value) throws Exception
		{
		    properties.setLong(key, Long.parseLong(value));
		    assertEquals(value,
				 properties.getMandatoryProperty(key));
		}
	    }
	 ).run();
    }

    public void testSetShort() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	
	(new IterateOverProperties(m_shortSet) {
		void match(String key, String value) throws Exception
		{
		    properties.setShort(key, Short.parseShort(value));
		    assertEquals(value,
				 properties.getMandatoryProperty(key));
		}
	    }
	 ).run();
    }

    public void testSetDouble() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	
	(new IterateOverProperties(m_doubleSet) {
		void match(String key, String value) throws Exception
		{
		    properties.setDouble(key, Double.parseDouble(value));
		    assertEquals(Double.parseDouble(value),
				 Double.parseDouble(
				     properties.getMandatoryProperty(key)),
				 0);
		}
	    }
	 ).run();
    }


    public void testSetBoolean() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	
	(new IterateOverProperties(m_booleanSet) {
		void match(String key, String value) throws Exception
		{
		    properties.setBoolean(key,
					  Boolean.valueOf(value).
					  booleanValue());
		    assertEquals(new Boolean(value).toString(),
				 properties.getMandatoryProperty(key));
		}
	    }
	 ).run();
    }

    public void testDefaultProperties() throws Exception
    {
	setSystemProperties();

	try {
	    // Default constructor doesn't add system properties.
	    final GrinderProperties properties = new GrinderProperties();
	    assertEquals(new Properties(), properties);
	}
	finally {
	    restoreSystemProperties();
	}
    }
    
    public void testPropertiesFileHanding() throws Exception
    {
	setSystemProperties();
	
	try {
	    final File file = File.createTempFile("testing", "123");
	    file.deleteOnExit();

	    final PrintWriter writer =
		new PrintWriter(new FileWriter(file), true);
	
	    (new IterateOverProperties(m_grinderSet) {
		    void match(String key, String value) throws Exception
		    {
			writer.println(key + ":" + "should be overridden");
		    }
		}
	     ).run();
	
	    (new IterateOverProperties(m_stringSet) {
		    void match(String key, String value) throws Exception
		    {
			writer.println(key + ":" + "not overridden");
		    }
		}
	     ).run();

	    // Constructor that takes a file adds system properties
	    // beginning with "grinder.", and nothing else.
	    final GrinderProperties properties2 = new GrinderProperties(file);

	    assertEquals(m_grinderSet.size() + m_stringSet.size(),
			 properties2.size());
	    
	    (new IterateOverProperties(m_grinderSet) {
		    void match(String key, String value) throws Exception
		    {
			assertEquals(value,
				     properties2.getMandatoryProperty(key));
		    }
		}
	     ).run();


	    (new IterateOverProperties(m_stringSet) {
		    void match(String key, String value) throws Exception
		    {
			assertEquals("not overridden",
				     properties2.getMandatoryProperty(key));
		    }
		}
	     ).run();
	}
	finally {
	    restoreSystemProperties();
	}
    }
    
    private void setSystemProperties() throws Exception
    {
	(new IterateOverProperties(m_grinderProperties) {
		void match(String key, String value) throws Exception
		{
		    if (key.length() > 0) {
			System.setProperty(key, value);
		    }
		}
	    }
	 ).run();
    }

    private void restoreSystemProperties()
    {
	// Do nothing! When run under Ant, System.getProperties()
	// returns an empty object, so we can't cach/restore the old
	// properties.
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

