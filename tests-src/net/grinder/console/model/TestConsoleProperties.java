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

package net.grinder.console.model;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

import net.grinder.common.GrinderException;
import net.grinder.console.ConsoleException;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConsoleProperties extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestConsoleProperties.class);
    }

    public TestConsoleProperties(String name)
    {
	super(name);
    }

    private File m_file;
    private FileWriter m_fileWriter;
    private Random m_random = new Random();

    protected void setUp() throws Exception
    {
	m_file = File.createTempFile("testing", "123");
	m_file.deleteOnExit();

	m_fileWriter = new FileWriter(m_file);
    }

    public void testCollectSamples() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.COLLECT_SAMPLES_PROPERTY, 0,
			    Integer.MAX_VALUE) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getCollectSampleCount();
	    }

	    protected void set(ConsoleProperties properties, int i)
		throws ConsoleException
	    {
		properties.setCollectSampleCount(i);
	    }
	}.doTest();
    }

    public void testIgnoreSamples() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.IGNORE_SAMPLES_PROPERTY, 1,
			    Integer.MAX_VALUE) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getIgnoreSampleCount();
	    }

	    protected void set(ConsoleProperties properties, int i)
		throws ConsoleException
	    {
		properties.setIgnoreSampleCount(i);
	    }
	}.doTest();
    }

    public void testSampleInterval() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.SAMPLE_INTERVAL_PROPERTY, 1,
			    Integer.MAX_VALUE) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getSampleInterval();
	    }

	    protected void set(ConsoleProperties properties, int i)
		throws ConsoleException
	    {
		properties.setSampleInterval(i);
	    }
	}.doTest();
    }

    public void testSignificantFigures() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.SIG_FIG_PROPERTY, 0,
			    Integer.MAX_VALUE) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getSignificantFigures();
	    }

	    protected void set(ConsoleProperties properties, int i)
		throws ConsoleException
	    {
		properties.setSignificantFigures(i);
	    }
	}.doTest();
    }

    public void testMulticastAddress() throws Exception
    {
	final String propertyName =
	    ConsoleProperties.MULTICAST_ADDRESS_PROPERTY;

	final String s1 = "229.1.2.3";

	m_fileWriter.write(propertyName + ":" + s1);
	m_fileWriter.close();

	final ConsoleProperties properties = new ConsoleProperties(m_file);
	assertEquals(s1, properties.getMulticastAddress().getHostAddress());

	final String s2 = "239.99.33.11";

	properties.setMulticastAddress(s2);
	assertEquals(s2, properties.getMulticastAddress().getHostAddress());

	properties.save();

	final ConsoleProperties properties2 = new ConsoleProperties(m_file);
	assertEquals(s2, properties2.getMulticastAddress().getHostAddress());

	final String s3 = "224.46.68.80";

	final PropertyChangeEvent expected =
	    new PropertyChangeEvent(properties2, propertyName, s2, s3);

	final MyListener listener = new MyListener(expected);
	final MyListener listener2 = new MyListener(expected);

	properties2.addPropertyChangeListener(listener);
	properties2.addPropertyChangeListener(propertyName, listener2);

	properties2.setMulticastAddress(s3);
    }

    public void testConsolePort() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.CONSOLE_PORT_PROPERTY, 0,
			    0xFFFF) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getConsolePort();
	    }

	    protected void set(ConsoleProperties properties, int i)
		throws ConsoleException
	    {
		properties.setConsolePort(i);
	    }
	}.doTest();
    }

    public void testGrinderPort() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.GRINDER_PORT_PROPERTY, 0,
			    0xFFFF) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getGrinderPort();
	    }

	    protected void set(ConsoleProperties properties, int i)
		throws ConsoleException
	    {
		properties.setGrinderPort(i);
	    }
	}.doTest();
    }

    private abstract class TestIntTemplate
    {
	private final String m_propertyName;
	private final int m_minimum;
	private final int m_maximum;

	public TestIntTemplate(String propertyName, int minimum, int maximum)
	{
	    if (maximum <= minimum) {
		throw new IllegalArgumentException(
		    "Minimum not less than maximum");
	    }

	    m_propertyName = propertyName;
	    m_minimum = minimum;
	    m_maximum = maximum;
	}

	private int getRandomInt()
	{
	    return getRandomInt(m_minimum, m_maximum);
	}

	private int getRandomInt(int minimum, int maximum)
	{
	    // Valid values are in [minimum, maximum], so range is 1
	    // more than maximum value.Will not fit in an int, use a
	    // long.
	    long range = (long)maximum + 1 - minimum;

	    return (int)(minimum + Math.abs(m_random.nextLong()) % range);
	}

	public void doTest() throws Exception
	{
	    final int i1 = getRandomInt();

	    m_fileWriter.write(m_propertyName + ":" + i1);
	    m_fileWriter.close();

	    final ConsoleProperties properties = new ConsoleProperties(m_file);
	    assertEquals(i1, get(properties));

	    final int i2 = getRandomInt();

	    set(properties, i2);
	    assertEquals(i2, get(properties));

	    properties.save();

	    final ConsoleProperties properties2 =
		new ConsoleProperties(m_file);
	    assertEquals(i2, get(properties2));

	    final int i3 = getRandomInt();

	    final PropertyChangeEvent expected =
		new PropertyChangeEvent(properties2, m_propertyName, 
					new Integer(i2), new Integer(i3));

	    final MyListener listener = new MyListener(expected);
	    final MyListener listener2 = new MyListener(expected);

	    properties2.addPropertyChangeListener(listener);
	    properties2.addPropertyChangeListener(m_propertyName, listener2);

	    set(properties2, i3);

	    if (m_minimum > Integer.MIN_VALUE) {
		try {
		    set(properties, m_minimum - 1);
		    fail("Should not reach");
		}
		catch (ConsoleException e) {
		}

		try {
		    set(properties, Integer.MIN_VALUE);
		    fail("Should not reach");
		}
		catch (ConsoleException e) {
		}

		try {
		    set(properties, getRandomInt(Integer.MIN_VALUE,
						 m_minimum - 1));
		    fail("Should not reach");
		}
		catch (ConsoleException e) {
		}
	    }


	    if (m_maximum < Integer.MAX_VALUE) {
		try {
		    set(properties, m_maximum + 1);
		    fail("Should not reach");
		}
		catch (ConsoleException e) {
		}

		try {
		    set(properties, Integer.MAX_VALUE);
		    fail("Should not reach");
		}
		catch (ConsoleException e) {
		}

		try {
		    set(properties, getRandomInt(m_maximum + 1,
						 Integer.MAX_VALUE));
		    fail("Should not reach");
		}
		catch (ConsoleException e) {
		}
	    }
	}

	protected abstract int get(ConsoleProperties properties);

	protected abstract void set(ConsoleProperties properties, int i)
	    throws ConsoleException;
    }

    private class MyListener implements PropertyChangeListener
    {
	final PropertyChangeEvent m_expected;

	MyListener(PropertyChangeEvent expected) 
	{
	    m_expected = expected;
	}

	public void propertyChange(PropertyChangeEvent event)
	{
	    assertEquals(m_expected.getOldValue(), event.getOldValue());
	    assertEquals(m_expected.getNewValue(), event.getNewValue());
	    assertEquals(m_expected.getPropertyName(),
			 event.getPropertyName());
	}
    }
}
